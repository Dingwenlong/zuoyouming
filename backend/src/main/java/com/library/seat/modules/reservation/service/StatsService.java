package com.library.seat.modules.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.seat.modules.reservation.entity.Reservation;
import com.library.seat.modules.reservation.service.ReservationService;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.seat.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    @Autowired
    private SeatService seatService;

    @Autowired
    private ReservationService reservationService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalSeats = seatService.count();
        long occupiedSeats = seatService.count(new QueryWrapper<Seat>().eq("status", "occupied"));
        
        // Mocking some dynamic data
        stats.put("totalSeats", totalSeats);
        stats.put("occupiedSeats", occupiedSeats);
        stats.put("utilizationRate", totalSeats > 0 ? (double)occupiedSeats / totalSeats * 100 : 0);
        stats.put("activeReservations", reservationService.count(new QueryWrapper<Reservation>().in("status", "reserved", "checked_in")));
        
        return stats;
    }

    public List<Map<String, Object>> getHeatmapData() {
        // In a real app, this would query historical reservation counts per seat
        // Mocking for now based on seat list
        List<Seat> seats = seatService.list();
        return seats.stream().map(seat -> {
            Map<String, Object> map = new HashMap<>();
            map.put("x", seat.getXCoord());
            map.put("y", seat.getYCoord());
            map.put("value", Math.random() * 100); // Mock heat value
            return map;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getTrendData() {
        // Mock trend data (last 7 days)
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        
        for (int i = 6; i >= 0; i--) {
            dates.add("Day " + i);
            values.add((int)(Math.random() * 50));
        }
        
        result.put("dates", dates);
        result.put("values", values);
        return result;
    }

    public Map<String, Object> getCongestionData() {
        // Mock congestion by area
        Map<String, Object> result = new HashMap<>();
        result.put("A区", Math.random() * 100);
        result.put("B区", Math.random() * 100);
        result.put("C区", Math.random() * 100);
        return result;
    }
}
