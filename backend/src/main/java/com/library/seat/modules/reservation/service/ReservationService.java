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

    private void broadcastSeatUpdate(Long seatId, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("seatId", seatId);
        message.put("status", status);
        message.put("event", "seat_update");
        messagingTemplate.convertAndSend("/topic/seats", message);
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

            // 设置签到截止时间: start_time + 15分钟 (Previously was now + 15m, but should be relative to start time)
            // But if it's an immediate reservation (start now), then now + 15m is correct.
            // If it's a future slot, deadline should be around start time.
            // Let's use start_time + 15m for deadline.
            reservation.setDeadline(new Date(reservation.getStartTime().getTime() + 15 * 60 * 1000));
            reservation.setCreateTime(now);
            this.save(reservation);

            // 4. 更新座位状态
            seat.setStatus("occupied");
            seatService.updateById(seat);

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
        
        broadcastSeatUpdate(reservation.getSeatId(), "occupied");

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
        // 设置暂离返回截止时间: 当前时间 + 30分钟
        reservation.setDeadline(new Date(System.currentTimeMillis() + 30 * 60 * 1000));
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
            broadcastSeatUpdate(seat.getId(), "available");
        }

        return Result.success(true);
    }

    public Result<List<Reservation>> getMyHistory(Long userId) {
        return Result.success(this.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreateTime)));
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
            broadcastSeatUpdate(seat.getId(), "available");
        }

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
