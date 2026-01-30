package com.library.seat.modules.seat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.seat.common.Result;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.seat.service.SeatService;
import com.library.seat.modules.sys.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "座位管理", description = "座位信息管理接口")
@RestController
@RequestMapping("/api/v1/seats")
public class SeatController {

    @Autowired
    private SeatService seatService;
    
    @Autowired
    private SysLogService sysLogService;

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Operation(summary = "获取所有座位列表", description = "支持区域筛选")
    @GetMapping
    public Result<List<Seat>> list(@Parameter(description = "区域 (A区/B区...)", required = false) @RequestParam(required = false) String area) {
        return Result.success(seatService.list(new LambdaQueryWrapper<Seat>()
                .eq(area != null, Seat::getArea, area)
                .eq(Seat::getDeleted, 0)));
    }

    @Operation(summary = "获取座位地图数据", description = "包含坐标信息的座位列表")
    @GetMapping("/map")
    public Result<List<Seat>> getMap() {
        return Result.success(seatService.list(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getDeleted, 0)));
    }

    @Operation(summary = "新增座位")
    @PostMapping
    public Result<Boolean> add(@RequestBody Seat seat) {
        Result<Boolean> res = seatService.addSeat(seat);
        if (res.isSuccess()) {
            sysLogService.log(getCurrentUsername(), "新增座位", "座位号: " + seat.getSeatNo());
        }
        return res;
    }

    @Operation(summary = "修改座位信息")
    @PutMapping
    public Result<Boolean> update(@RequestBody Seat seat) {
        Result<Boolean> res = seatService.updateSeat(seat);
        if (res.isSuccess()) {
            sysLogService.log(getCurrentUsername(), "修改座位", "座位ID: " + seat.getId());
        }
        return res;
    }

    @Operation(summary = "删除座位")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        Result<Boolean> res = seatService.deleteSeat(id);
        if (res.isSuccess()) {
            sysLogService.log(getCurrentUsername(), "删除座位", "座位ID: " + id);
        }
        return res;
    }

    @Operation(summary = "批量删除座位")
    @PostMapping("/batch/delete")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        Result<Boolean> res = seatService.batchDelete(ids);
        if (res.isSuccess()) {
            sysLogService.log(getCurrentUsername(), "批量删除座位", "ID列表: " + ids.toString());
        }
        return res;
    }

    @Operation(summary = "批量导入座位")
    @PostMapping("/batch/import")
    public Result<Boolean> batchImport(@RequestBody List<Seat> seats) {
        Result<Boolean> res = seatService.batchImport(seats);
        if (res.isSuccess()) {
            sysLogService.log(getCurrentUsername(), "批量导入座位", "数量: " + seats.size());
        }
        return res;
    }

    @Operation(summary = "管理员修改座位状态", description = "强制释放/设为故障")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(
            @PathVariable Long id, 
            @Parameter(description = "状态: available, occupied, maintenance") @RequestBody Map<String, String> params) {
        
        String status = params.get("status");
        if (status == null) {
            return Result.error("Status is required");
        }
        
        Seat seat = seatService.getById(id);
        if (seat == null) {
            return Result.error("Seat not found");
        }
        
        String oldStatus = seat.getStatus();
        seat.setStatus(status);
        boolean success = seatService.updateById(seat);
        
        if (success) {
             sysLogService.log(getCurrentUsername(), "修改座位状态", 
                     String.format("ID: %d, %s -> %s", id, oldStatus, status));
        }
        
        return Result.success(success);
    }
}
