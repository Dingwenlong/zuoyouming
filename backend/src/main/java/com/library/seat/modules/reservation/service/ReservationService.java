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
    public Result<Boolean> reserve(Reservation reservation) {
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

            // 1. 检查座位是否存在且空闲
            Seat seat = seatService.getById(reservation.getSeatId());
            if (seat == null || !"available".equals(seat.getStatus())) {
                return Result.error("座位已被预约或不可用");
            }
            
            // 2. 检查用户是否有未完成的预约
            Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getUserId, reservation.getUserId())
                    .in(Reservation::getStatus, "reserved", "checked_in", "away"));
            
            if (count > 0) {
                return Result.error("您当前已有预约，不能重复预约");
            }

            // 3. 创建预约
            reservation.setStatus("reserved");
            if (reservation.getType() == null) {
                reservation.setType("appointment");
            }
            
            // Set start/end time based on slot or defaults
            Date now = new Date();
            
            if (reservation.getSlot() != null) {
                // Calculate times based on slot
                // Assume slot is for today. If current time is past slot start, maybe it's for tomorrow?
                // For simplicity, let's assume it's for today.
                // morning: 08:00-12:00
                // afternoon: 13:00-17:00
                // evening: 18:00-22:00
                
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(now);
                int year = cal.get(java.util.Calendar.YEAR);
                int month = cal.get(java.util.Calendar.MONTH);
                int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
                
                java.util.Calendar startCal = java.util.Calendar.getInstance();
                startCal.set(year, month, day, 0, 0, 0);
                java.util.Calendar endCal = java.util.Calendar.getInstance();
                endCal.set(year, month, day, 0, 0, 0);
                
                switch (reservation.getSlot()) {
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
                    default:
                        // Invalid slot, fallback to default logic
                        break;
                }
                
                if (reservation.getStartTime() == null) {
                    reservation.setStartTime(startCal.getTime());
                }
                if (reservation.getEndTime() == null) {
                    reservation.setEndTime(endCal.getTime());
                }
            }
            
            // Fallback if still null
            if (reservation.getStartTime() == null) {
                reservation.setStartTime(now);
            }
            if (reservation.getEndTime() == null) {
                // Default duration: 4 hours
                reservation.setEndTime(new Date(reservation.getStartTime().getTime() + 4 * 60 * 60 * 1000));
            }

            // 设置签到截止时间: start_time + 配置分钟数
            int violationTime = configService.getIntValue("violation_time", 30);
            reservation.setDeadline(new Date(reservation.getStartTime().getTime() + (long) violationTime * 60 * 1000));
            reservation.setCreateTime(now);
            this.save(reservation);

            // 4. 更新座位状态
            seat.setStatus("occupied");
            seatService.updateById(seat);

            // 5. 广播更新
            broadcastReservationUpdate(reservation.getUserId(), "reservation_success", "预约成功");
            seatService.broadcastSeatUpdate(seat.getId(), "occupied");
            statsService.broadcastStats();

            // 6. 发送通知
            notificationService.send(reservation.getUserId(), "预约成功", "您已成功预约座位 " + seat.getSeatNo() + "，请在规定时间内签到。", "success");

            // 7. 记录日志
            sysLogService.log(user.getUsername(), "预约座位", "预约座位号: " + seat.getSeatNo());

            return Result.success(true);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> checkIn(Long id, Long userId) {
        Reservation reservation = this.getById(id);

        if (reservation == null || !reservation.getUserId().equals(userId)) {
            return Result.error("预约记录不存在");
        }
        
        if (!"reserved".equals(reservation.getStatus()) && !"away".equals(reservation.getStatus())) {
            return Result.error("当前状态不可签到");
        }
        
        // 检查截止时间
        if (reservation.getDeadline() != null && new Date().after(reservation.getDeadline())) {
             return Result.error("已超过签到/返回截止时间");
        }

        // 更新预约状态
        reservation.setStatus("checked_in");
        reservation.setDeadline(null); // 清除截止时间
        this.updateById(reservation);
        
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

        // 履约奖励: +2 信用分
        userDetailsService.addCreditScore(userId, 2);

        // 发送通知
        notificationService.send(userId, "取消预约成功", "您的预约已取消，座位已释放。", "info");

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
