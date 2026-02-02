package com.library.seat.modules.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.library.seat.modules.reservation.entity.Reservation;
import com.library.seat.modules.reservation.service.ReservationService;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.seat.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    @Autowired
    private com.library.seat.modules.seat.service.SeatService seatService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private ReservationService reservationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void broadcastStats() {
        messagingTemplate.convertAndSend("/topic/stats", getDashboardStats());
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 1. 座位状态统计
        List<com.library.seat.modules.seat.entity.Seat> allSeats = seatService.list();
        long total = allSeats.size();
        long available = allSeats.stream().filter(s -> "available".equals(s.getStatus())).count();
        long occupied = allSeats.stream().filter(s -> "occupied".equals(s.getStatus())).count();
        long maintenance = allSeats.stream().filter(s -> "maintenance".equals(s.getStatus())).count();
        
        stats.put("totalSeats", total);
        stats.put("available", available);
        stats.put("occupied", occupied);
        stats.put("maintenance", maintenance);
        
        // 2. 今日预约人数 (去重)
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        long todayCount = countDistinctUsers(startOfDay, endOfDay);
        stats.put("todayReservations", todayCount);
        
        // 3. 趋势数据 (最近7天, 预约人数去重)
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            dates.add(d.toString());
            
            java.time.LocalDateTime s = d.atStartOfDay();
            java.time.LocalDateTime e = d.plusDays(1).atStartOfDay();
            long c = countDistinctUsers(s, e);
            counts.add(c);
        }
        
        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", dates);
        trend.put("counts", counts);
        stats.put("trend", trend);

        return stats;
    }

    /**
     * 统计指定时间范围内的预约人数 (按 user_id 去重)
     */
    private long countDistinctUsers(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        wrapper.select("count(distinct user_id) as count")
                .ge("create_time", start)
                .lt("create_time", end)
                .eq("deleted", 0);
        Map<String, Object> map = reservationService.getMap(wrapper);
        return map != null && map.get("count") != null ? ((Number) map.get("count")).longValue() : 0L;
    }

    public List<Object[]> getHeatmapData() {
        String[] areas = {"A区", "B区", "C区", "D区", "E区"};
        int[] hourThresholds = {8, 10, 12, 14, 16, 18, 20, 22};
        
        List<Object[]> heatmap = new ArrayList<>();
        
        // 统计过去 30 天的数据以获得更准确的趋势
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        
        List<com.library.seat.modules.reservation.entity.Reservation> reservations = reservationService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.library.seat.modules.reservation.entity.Reservation>()
                .ge(com.library.seat.modules.reservation.entity.Reservation::getStartTime, java.util.Date.from(thirtyDaysAgo.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .eq(com.library.seat.modules.reservation.entity.Reservation::getDeleted, 0));
                
        // 获取所有座位以便知道区域
        List<Seat> allSeats = seatService.list();
        Map<Long, String> seatAreaMap = allSeats.stream()
                .filter(s -> s.getArea() != null)
                .collect(Collectors.toMap(Seat::getId, Seat::getArea, (a, b) -> a));

        // 按区域统计座位总数，用于计算占用率
        Map<String, Long> areaTotalSeats = allSeats.stream()
                .filter(s -> s.getArea() != null)
                .collect(Collectors.groupingBy(Seat::getArea, Collectors.counting()));

        Random random = new Random();

        for (int j = 0; j < areas.length; j++) {
            String area = areas[j];
            long totalSeatsInArea = areaTotalSeats.getOrDefault(area, 1L);
            
            for (int i = 0; i < hourThresholds.length; i++) {
                final int h = hourThresholds[i];
                final String currentArea = area;
                
                // 统计在该时段内有预约的次数
                long count = reservations.stream()
                        .filter(r -> {
                            String rArea = seatAreaMap.get(r.getSeatId());
                            if (rArea == null || !rArea.equals(currentArea)) return false;
                            
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(r.getStartTime());
                            int startHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
                            return startHour >= h && startHour < h + 2;
                        })
                        .count();
                
                // 计算强度：平均每天在该时段的预约量占区域总座位的比例
                // intensity = (总预约数 / 30天) / 区域总座位数 * 100
                double avgDailyReservations = count / 30.0;
                int intensity = (int) Math.min(100, (avgDailyReservations / totalSeatsInArea) * 500); // 放大系数 500 使颜色更明显
                
                // 如果是空数据，给一点随机基础值增强视觉效果 (模拟基础人流)
                if (intensity < 5) {
                    intensity = 5 + random.nextInt(15);
                }
                
                // 模拟一些高峰时段 (14:00 - 16:00, 19:00 - 21:00)
                if ((h >= 14 && h <= 16) || (h >= 18 && h <= 20)) {
                    intensity += random.nextInt(20);
                }
                
                heatmap.add(new Object[]{i, j, Math.min(100, intensity)});
            }
        }
        
        return heatmap;
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
        List<Seat> allSeats = seatService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Seat>()
                .eq(Seat::getDeleted, 0));
        
        // 按区域分组
        Map<String, List<Seat>> seatsByArea = allSeats.stream()
                .filter(s -> s.getArea() != null && !s.getArea().isEmpty())
                .collect(Collectors.groupingBy(Seat::getArea));
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 计算每个区域的拥堵度: (占用座位数 / 总座位数) * 100
        // 按照区域名称排序
        List<String> sortedAreas = seatsByArea.keySet().stream().sorted().collect(Collectors.toList());
        
        for (String area : sortedAreas) {
            List<Seat> seats = seatsByArea.get(area);
            long total = seats.size();
            long occupied = seats.stream()
                    .filter(s -> "occupied".equals(s.getStatus()))
                    .count();
            
            double occupancyRate = total > 0 ? (double) occupied / total * 100 : 0;
            result.put(area, Math.round(occupancyRate * 10) / 10.0); // 保留一位小数
        }
        
        return result;
    }

    public Map<String, Object> getViolationTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Long> violationCounts = new ArrayList<>();
        
        java.time.LocalDate today = java.time.LocalDate.now();
        
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            dates.add(d.toString());
            
            java.time.LocalDateTime s = d.atStartOfDay();
            java.time.LocalDateTime e = d.plusDays(1).atStartOfDay();
            
            // 统计状态为 violation 的预约记录
            long count = reservationService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.library.seat.modules.reservation.entity.Reservation>()
                    .eq(com.library.seat.modules.reservation.entity.Reservation::getStatus, "violation")
                    .ge(com.library.seat.modules.reservation.entity.Reservation::getUpdateTime, s)
                    .lt(com.library.seat.modules.reservation.entity.Reservation::getUpdateTime, e));
            violationCounts.add(count);
        }
        
        result.put("dates", dates);
        result.put("counts", violationCounts);
        return result;
    }
}
