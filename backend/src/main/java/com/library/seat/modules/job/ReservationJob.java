package com.library.seat.modules.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.seat.modules.reservation.entity.Reservation;
import com.library.seat.modules.reservation.service.ReservationService;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.seat.service.SeatService;
import com.library.seat.modules.sys.service.UserDetailsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ReservationJob {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SeatService seatService;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private void broadcastSeatUpdate(Long seatId, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("seatId", seatId);
        message.put("status", status);
        message.put("event", "seat_update");
        messagingTemplate.convertAndSend("/topic/seats", message);
    }
    
    private void sendUserAlert(Long userId, String message) {
        // Send to specific user: /user/{userId}/queue/alerts
        // Note: Client needs to subscribe to /user/queue/alerts
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/alerts",
                message
        );
    }

    /**
     * 每分钟检查违约记录
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkViolation() {
        log.info("Executing Violation Check Job...");
        
        Date now = new Date();
        
        // 1. Check 15-min no-show (reserved -> violation)
        List<Reservation> noShows = reservationService.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, "reserved")
                .lt(Reservation::getDeadline, now));

        for (Reservation res : noShows) {
            handleViolation(res, "No-show violation");
        }
        
        // 2. Check 30-min away (away -> violation)
        List<Reservation> timeOuts = reservationService.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, "away")
                .lt(Reservation::getDeadline, now));

        for (Reservation res : timeOuts) {
            handleViolation(res, "Away timeout violation");
        }
    }
    
    private void handleViolation(Reservation res, String reason) {
        log.info("Violation found: id={}, reason={}", res.getId(), reason);
        
        // Mark as violation
        res.setStatus("violation");
        reservationService.updateById(res);

        // Free seat
        Seat seat = seatService.getById(res.getSeatId());
        if (seat != null) {
            seat.setStatus("available");
            seatService.updateById(seat);
            broadcastSeatUpdate(seat.getId(), "available");
        }
        
        // Deduct credit score (-10)
        userDetailsService.deductCreditScore(res.getUserId(), 10);
        
        // Notify user
        sendUserAlert(res.getUserId(), "Your reservation has been cancelled due to violation: " + reason);
    }

    /**
     * 每分钟检查已完成的预约 (endTime < now)
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkExpiration() {
        log.info("Executing Expiration Check Job...");
        
        Date now = new Date();

        // Find checked_in reservations that have ended
        List<Reservation> expired = reservationService.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, "checked_in") 
                .lt(Reservation::getEndTime, now));

        for (Reservation res : expired) {
            log.info("Expiration found: reservationId={}", res.getId());
            
            // Mark completed
            res.setStatus("completed");
            reservationService.updateById(res);

            // Free seat
            Seat seat = seatService.getById(res.getSeatId());
            if (seat != null) {
                seat.setStatus("available");
                seatService.updateById(seat);
                broadcastSeatUpdate(seat.getId(), "available");
            }
        }
    }
}
