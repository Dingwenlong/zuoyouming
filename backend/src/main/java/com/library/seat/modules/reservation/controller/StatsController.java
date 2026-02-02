package com.library.seat.modules.reservation.controller;

import com.library.seat.common.Result;
import com.library.seat.modules.reservation.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "统计监控", description = "仪表盘与数据统计")
@RestController
@RequestMapping("/api/v1/stats")
@PreAuthorize("hasAnyAuthority('admin', 'librarian')")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @Operation(summary = "获取首页仪表盘数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        return Result.success(statsService.getDashboardStats());
    }

    @Operation(summary = "获取热力图数据")
    @GetMapping("/heatmap")
    public Result<List<Object[]>> getHeatmap() {
        return Result.success(statsService.getHeatmapData());
    }

    @Operation(summary = "获取预约趋势数据")
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend() {
        return Result.success(statsService.getTrendData());
    }

    @Operation(summary = "获取区域拥堵度数据")
    @GetMapping("/congestion")
    public Result<Map<String, Object>> getCongestion() {
        return Result.success(statsService.getCongestionData());
    }

    @Operation(summary = "获取违规趋势数据")
    @GetMapping("/violation-trend")
    public Result<Map<String, Object>> getViolationTrend() {
        return Result.success(statsService.getViolationTrend());
    }
}
