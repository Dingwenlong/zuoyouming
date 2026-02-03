package com.library.seat.modules.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.common.Result;
import com.library.seat.modules.reservation.entity.Appeal;
import com.library.seat.modules.reservation.entity.Reservation;
import com.library.seat.modules.reservation.mapper.AppealMapper;
import com.library.seat.modules.reservation.mapper.ReservationMapper;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.seat.service.SeatService;
import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ReservationService extends ServiceImpl<ReservationMapper, Reservation> {

    @Autowired
    private SeatService seatService;

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private StatsService statsService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private com.library.seat.modules.sys.service.ISysNotificationService notificationService;

    @Autowired
    private com.library.seat.modules.sys.service.SysLogService sysLogService;

    @Autowired
    private com.library.seat.modules.sys.service.ISysConfigService configService;

    public com.library.seat.modules.sys.service.ISysNotificationService getNotificationService() {
        return notificationService;
    }

    public void broadcastReservationUpdate(Long userId, String event, String reason) {
        broadcastReservationUpdate(userId, event, reason, true);
    }

    public void broadcastReservationUpdate(Long userId, String event, String reason, boolean includeStats) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("reason", reason);
        payload.put("timestamp", new Date());

        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/reservation_update",
                payload
        );
        
        // 同时广播公共状态更新（用于消息广场实时刷新座位信息）
        broadcastUserSeatStatus(userId);
        
        // 广播统计信息更新（用于首页实时刷新数据）
        if (includeStats) {
            statsService.broadcastStats();
        }
    }

    public void broadcastUserSeatStatus(Long userId) {
        SysUser user = userDetailsService.getById(userId);
        if (user == null) return;

        // 获取当前活跃预约
        Reservation activeRes = this.getOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus, "reserved", "checked_in", "away")
                .last("LIMIT 1"));

        String seatNo = null;
        if (activeRes != null) {
            Seat seat = seatService.getById(activeRes.getSeatId());
            if (seat != null) {
                seatNo = seat.getSeatNo();
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", user.getUsername());
        payload.put("seatNo", seatNo);
        payload.put("event", "user_seat_change");
        messagingTemplate.convertAndSend("/topic/user_seat_status", payload);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> reserve(Reservation reservation) {
        String lockKey = "lock:seat:" + reservation.getSeatId();
        // 简单分布式锁 (SetNX)
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, String.valueOf(reservation.getUserId()), 10, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(lock)) {
            return Result.error("该座位正在被抢占，请稍后重试");
        }

        try {
            // 0. 检查用户信用分
            int minScore = configService.getIntValue("min_credit_score", 60);
            SysUser user = userDetailsService.getById(reservation.getUserId());
            if (user != null && user.getCreditScore() < minScore) {
                return Result.error("您的信用分低于 " + minScore + " 分，暂时无法预约。请通过申诉或联系管理员处理。");
            }

            // 1. 检查座位是否存在
            Seat seat = seatService.getById(reservation.getSeatId());
            if (seat == null || "maintenance".equals(seat.getStatus())) {
                return Result.error("座位不存在或正在维护中");
            }

            // 获取需要预约的时段列表
            List<String> slots = reservation.getSlots();
            if (slots == null || slots.isEmpty()) {
                if (reservation.getSlot() != null) {
                    slots = java.util.Collections.singletonList(reservation.getSlot());
                } else {
                    return Result.error("请选择预约时段");
                }
            }

            // 2. 检查冲突 (座位冲突和用户冲突)
            for (String slot : slots) {
                // 检查座位在该时段是否已被占用
                Long seatOccupied = this.baseMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getSeatId, reservation.getSeatId())
                        .eq(Reservation::getSlot, slot)
                        .in(Reservation::getStatus, "reserved", "checked_in", "away"));
                if (seatOccupied > 0) {
                    return Result.error("座位在时段 " + slot + " 已被预约");
                }

                // 检查用户在该时段是否已有预约
                Long userReserved = this.baseMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, reservation.getUserId())
                        .eq(Reservation::getSlot, slot)
                        .in(Reservation::getStatus, "reserved", "checked_in", "away"));
                if (userReserved > 0) {
                    return Result.error("您在时段 " + slot + " 已有预约，请勿重复预约");
                }
            }

            // 3. 创建预约
            Date now = new Date();
            List<Reservation> createdReservations = new java.util.ArrayList<>();
            
            for (String slot : slots) {
                Reservation res = new Reservation();
                org.springframework.beans.BeanUtils.copyProperties(reservation, res);
                res.setSlot(slot);
                res.setStatus("reserved");
                if (res.getType() == null) {
                    res.setType("appointment");
                }
                
                // 计算该时段的起止时间
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(now);
                int year = cal.get(java.util.Calendar.YEAR);
                int month = cal.get(java.util.Calendar.MONTH);
                int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
                
                java.util.Calendar startCal = java.util.Calendar.getInstance();
                startCal.set(year, month, day, 0, 0, 0);
                java.util.Calendar endCal = java.util.Calendar.getInstance();
                endCal.set(year, month, day, 0, 0, 0);
                
                switch (slot) {
                    case "morning":
                        startCal.set(java.util.Calendar.HOUR_OF_DAY, 8);
                        endCal.set(java.util.Calendar.HOUR_OF_DAY, 12);
                        break;
                    case "afternoon":
                        startCal.set(java.util.Calendar.HOUR_OF_DAY, 13);
                        endCal.set(java.util.Calendar.HOUR_OF_DAY, 17);
                        break;
                    case "evening":
                        startCal.set(java.util.Calendar.HOUR_OF_DAY, 18);
                        endCal.set(java.util.Calendar.HOUR_OF_DAY, 22);
                        break;
                }
                
                res.setStartTime(startCal.getTime());
                res.setEndTime(endCal.getTime());
                
                // 设置签到截止时间: start_time + checkin_after_window
                int checkInAfter = configService.getIntValue("checkin_after_window", 15);
                res.setDeadline(new Date(res.getStartTime().getTime() + (long) checkInAfter * 60 * 1000));
                res.setCreateTime(now);
                this.save(res);
                createdReservations.add(res);
            }

            // 4. 更新座位实时状态 (如果当前有时段被占用，则设为 occupied)
            updateSeatRealtimeStatus(reservation.getSeatId());

            // 5. 广播更新
            broadcastReservationUpdate(reservation.getUserId(), "reservation_success", "预约成功");
            statsService.broadcastStats();

            // 6. 发送通知
            notificationService.send(reservation.getUserId(), "预约成功", "您已成功预约座位 " + seat.getSeatNo() + " (" + String.join(",", slots) + ")，请在规定时间内签到。", "success");

            // 7. 记录日志
            sysLogService.log(user.getUsername(), "批量预约座位", "座位号: " + seat.getSeatNo() + ", 时段: " + String.join(",", slots));

            // 返回第一个时段的信息用于前端倒计时
            Reservation first = createdReservations.get(0);
            Map<String, Object> result = new HashMap<>();
            result.put("id", first.getId());
            result.put("startTime", first.getStartTime());
            result.put("deadline", first.getDeadline());

            return Result.success(result);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void updateSeatRealtimeStatus(Long seatId) {
        String currentSlot = getCurrentSlot();
        if (currentSlot == null) {
            seatService.updateStatus(seatId, "available");
            return;
        }

        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId)
                .eq(Reservation::getSlot, currentSlot)
                .in(Reservation::getStatus, "reserved", "checked_in", "away"));
        
        String newStatus = count > 0 ? "occupied" : "available";
        seatService.updateStatus(seatId, newStatus);
        seatService.broadcastSeatUpdate(seatId, newStatus);
    }

    private String getCurrentSlot() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 8 && hour < 12) return "morning";
        if (hour >= 13 && hour < 17) return "afternoon";
        if (hour >= 18 && hour < 22) return "evening";
        return null;
    }

    private void autoCheckInNextSlots(Reservation current) {
        String nextSlot = getNextSlot(current.getSlot());
        if (nextSlot == null) return;

        Reservation nextRes = this.getOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, current.getUserId())
                .eq(Reservation::getSeatId, current.getSeatId())
                .eq(Reservation::getSlot, nextSlot)
                .eq(Reservation::getStatus, "reserved"));

        if (nextRes != null) {
            nextRes.setStatus("checked_in");
            nextRes.setDeadline(null);
            this.updateById(nextRes);
            // 递归处理下一个
            autoCheckInNextSlots(nextRes);
        }
    }

    private String getNextSlot(String slot) {
        if ("morning".equals(slot)) return "afternoon";
        if ("afternoon".equals(slot)) return "evening";
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> checkIn(Long id, Long userId, Map<String, Object> params) {
        Reservation reservation = this.getById(id);

        if (reservation == null || !reservation.getUserId().equals(userId)) {
            return Result.error("预约记录不存在");
        }
        
        if (!"reserved".equals(reservation.getStatus()) && !"away".equals(reservation.getStatus())) {
            return Result.error("当前状态不可签到");
        }
        
        long nowTime = System.currentTimeMillis();
        long startTime = reservation.getStartTime().getTime();
        int beforeWindow = configService.getIntValue("checkin_before_window", 15);
        int afterWindow = configService.getIntValue("checkin_after_window", 15);

        // 1. 针对预约签到 (reserved -> checked_in)
        if ("reserved".equals(reservation.getStatus())) {
            if (nowTime < startTime - (long) beforeWindow * 60 * 1000) {
                return Result.error("尚未到达签到时间，请在起始时间前" + beforeWindow + "分钟内签到");
            }
            if (nowTime > startTime + (long) afterWindow * 60 * 1000) {
                return Result.error("已超过签到截止时间");
            }
        }
        
        // 2. 针对暂离返回 (away -> checked_in)
        if ("away".equals(reservation.getStatus())) {
            if (reservation.getDeadline() != null && nowTime > reservation.getDeadline().getTime()) {
                 return Result.error("已超过暂离返回截止时间");
            }
        }

        // 校验位置或扫码
        if (params != null) {
            // 1. 定位校验
            if (params.containsKey("lat") && params.containsKey("lng")) {
                double userLat = Double.parseDouble(params.get("lat").toString());
                double userLng = Double.parseDouble(params.get("lng").toString());
                double libLat = configService.getDoubleValue("library_latitude", 0);
                double libLng = configService.getDoubleValue("library_longitude", 0);
                
                if (libLat != 0 && libLng != 0) {
                    double distance = calculateDistance(userLat, userLng, libLat, libLng);
                    if (distance > 200) {
                        return Result.error(String.format("您距离图书馆太远 (%.1f米)，请到馆后再签到", distance));
                    }
                }
            } 
            // 2. 扫码校验
            else if (params.containsKey("qrCode")) {
                String qrCode = params.get("qrCode").toString();
                // 预期格式: "Area:A区,SeatNo:A-01"
                Seat seat = seatService.getById(reservation.getSeatId());
                if (seat != null) {
                    String expected = "Area:" + seat.getArea() + ",SeatNo:" + seat.getSeatNo();
                    if (!expected.equals(qrCode)) {
                        return Result.error("二维码信息不匹配，请扫描正确的座位二维码");
                    }
                }
            } else {
                return Result.error("未提供有效的签到凭证");
            }
        } else {
            return Result.error("请使用扫码或定位进行签到");
        }

        // 更新预约状态
        reservation.setStatus("checked_in");
        reservation.setDeadline(null); // 清除截止时间
        this.updateById(reservation);
        
        // 处理连续时段自动签到
        autoCheckInNextSlots(reservation);
        
        seatService.broadcastSeatUpdate(reservation.getSeatId(), "occupied");

        // 发送通知
        notificationService.send(userId, "签到成功", "您已成功签到，祝您学习愉快！", "success");

        // 记录日志
        SysUser user = userDetailsService.getById(userId);
        Seat seat = seatService.getById(reservation.getSeatId());
        if (user != null && seat != null) {
            sysLogService.log(user.getUsername(), "座位签到", "座位号: " + seat.getSeatNo());
        }

        return Result.success(true);
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> leave(Long id, Long userId) {
        Reservation reservation = this.getById(id);

        if (reservation == null || !reservation.getUserId().equals(userId)) {
            return Result.error("预约记录不存在");
        }
        
        if (!"checked_in".equals(reservation.getStatus())) {
            return Result.error("当前状态不可暂离");
        }

        // 更新预约状态 (暂离)
        reservation.setStatus("away");
        // Update updateTime to track when leave started
        reservation.setUpdateTime(new Date());
        // 设置暂离返回截止时间: 当前时间 + 配置分钟数
        int violationTime = configService.getIntValue("violation_time", 30);
        reservation.setDeadline(new Date(System.currentTimeMillis() + (long) violationTime * 60 * 1000));
        this.updateById(reservation);

        return Result.success(true);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> release(Long id, Long userId) {
        Reservation reservation = this.getById(id);

        if (reservation == null || !reservation.getUserId().equals(userId)) {
            return Result.error("预约记录不存在");
        }
        
        // 允许 release 的状态: reserved, checked_in, away
        if ("completed".equals(reservation.getStatus()) || "cancelled".equals(reservation.getStatus()) || "violation".equals(reservation.getStatus())) {
            return Result.error("当前状态无需释放");
        }

        String originalStatus = reservation.getStatus();
        long nowTime = System.currentTimeMillis();
        long startTime = reservation.getStartTime().getTime();
        int bufferTime = configService.getIntValue("release_buffer_time", 15);

        // 检查退座规则：必须在起始时间 release_buffer_time 分钟前退座
        if ("reserved".equals(originalStatus)) {
            if (nowTime > startTime - (long) bufferTime * 60 * 1000) {
                // 视为违约
                reservation.setStatus("violation");
                reservation.setEndTime(new Date());
                this.updateById(reservation);

                // 释放座位
                Seat seat = seatService.getById(reservation.getSeatId());
                if (seat != null) {
                    seat.setStatus("available");
                    seatService.updateById(seat);
                    seatService.broadcastSeatUpdate(seat.getId(), "available");
                }

                // 扣除信用分 (-10)
                userDetailsService.deductCreditScore(userId, 10);

                // 发送通知
                notificationService.send(userId, "退座违约扣分", "由于您在预约起始时间" + bufferTime + "分钟内取消，已被视为违约并扣除信用分。", "error");
                
                return Result.success(true);
            }
        }

        // 更新预约状态
        reservation.setStatus("completed");
        reservation.setEndTime(new Date()); // Actual end time
        this.updateById(reservation);

        // 释放座位
        Seat seat = seatService.getById(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus("available");
            seatService.updateById(seat);
            seatService.broadcastSeatUpdate(seat.getId(), "available");
        }

        // 履约奖励: +2 信用分 (仅限已签到并正常结束的情况)
        if ("checked_in".equals(originalStatus)) {
            userDetailsService.addCreditScore(userId, 2);
        }

        // 发送通知
        String notifyTitle = "reserved".equals(originalStatus) ? "取消预约成功" : "释放座位成功";
        String notifyMsg = "reserved".equals(originalStatus) ? "您的预约已取消，座位已释放。" : "您已成功结束座位使用，欢迎下次光临。";
        notificationService.send(userId, notifyTitle, notifyMsg, "info");

        // 记录日志
        SysUser user = userDetailsService.getById(userId);
        if (user != null && seat != null) {
            String op = "取消预约";
            if ("checked_in".equals(reservation.getStatus()) || "away".equals(reservation.getStatus())) {
                op = "释放座位";
            }
            sysLogService.log(user.getUsername(), op, "座位号: " + seat.getSeatNo());
        }

        return Result.success(true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void terminateActiveReservationBySeat(Long seatId, String reason) {
        terminateActiveReservationBySeat(seatId, reason, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void terminateActiveReservationBySeat(Long seatId, String reason, boolean includeStats) {
        // 查找该座位下的活跃预约 (reserved, checked_in, away)
        Reservation reservation = this.getOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSeatId, seatId)
                .in(Reservation::getStatus, "reserved", "checked_in", "away")
                .last("LIMIT 1"));

        if (reservation != null) {
            reservation.setStatus("completed");
            reservation.setEndTime(new Date());
            this.updateById(reservation);
            
            // 通知学生预约已结束
            broadcastReservationUpdate(reservation.getUserId(), "reservation_ended", reason, includeStats);

            // 发送系统通知
            notificationService.send(reservation.getUserId(), "预约被取消", "您预约的座位已被管理员取消，原因: " + reason, "warning");
        }
    }

    public Result<List<Reservation>> getMyHistory(Long userId) {
        List<Reservation> list = this.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreateTime));
        
        // 补全座位号
        for (Reservation res : list) {
            Seat seat = seatService.getById(res.getSeatId());
            if (seat != null) res.setSeatNo(seat.getSeatNo());
        }
        
        return Result.success(list);
    }

    public Result<Reservation> getActiveReservation(Long userId) {
        Reservation res = this.getOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus, "reserved", "checked_in", "away")
                .orderByAsc(Reservation::getStartTime)
                .last("LIMIT 1"));
        
        if (res != null) {
            Seat seat = seatService.getById(res.getSeatId());
            if (seat != null) res.setSeatNo(seat.getSeatNo());
        }
        
        return Result.success(res);
    }
    
    public Result<Boolean> appeal(Appeal appeal) {
        appeal.setCreateTime(new Date());
        appeal.setStatus("pending");
        appealMapper.insert(appeal);
        return Result.success(true);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> forceRelease(Long id) {
        Reservation reservation = this.getById(id);
        if (reservation == null) {
            return Result.error("Reservation not found");
        }
        
        // Mark as completed (or maybe admin_release? spec says just release)
        // If it was already completed/violation, maybe do nothing?
        // Let's assume we force it to completed status and free seat.
        
        reservation.setStatus("completed");
        reservation.setEndTime(new Date());
        this.updateById(reservation);

        // Free seat
        Seat seat = seatService.getById(reservation.getSeatId());
        if (seat != null) {
            seat.setStatus("available");
            seatService.updateById(seat);
            seatService.broadcastSeatUpdate(seat.getId(), "available");
        }

        // 通知学生预约已结束 (解决前端卡顿)
        broadcastReservationUpdate(reservation.getUserId(), "reservation_ended", "admin_force_release");

        // 发送系统通知
        notificationService.send(reservation.getUserId(), "预约被取消", "您预约的座位已被管理员取消。", "warning");

        // 记录日志
        String adminUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        sysLogService.log(adminUsername, "强制释放座位", "座位号: " + seat.getSeatNo() + ", 原用户: " + reservation.getUserId());

        return Result.success(true);
    }

    public Result<Boolean> reviewAppeal(Long id, String status, String reply) {
        Appeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            return Result.error("Appeal not found");
        }
        
        appeal.setStatus(status);
        appeal.setReply(reply);
        appeal.setUpdateTime(new Date());
        appealMapper.updateById(appeal);
        
        // If approved, maybe revoke violation status of reservation? 
        // Spec doesn't specify, but logically if appeal is approved, we might want to revert credit deduction?
        // For now, just update appeal status as per spec requirements.
        
        return Result.success(true);
    }
}
