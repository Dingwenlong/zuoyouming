package com.library.seat.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.seat.common.Result;
import com.library.seat.modules.sys.entity.SysLog;
import com.library.seat.modules.sys.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统日志", description = "操作日志管理接口")
@RestController
@RequestMapping("/api/v1/logs")
@PreAuthorize("hasAuthority('admin')")
public class SysLogController {

    @Autowired
    private SysLogService sysLogService;

    @Operation(summary = "获取日志列表")
    @GetMapping("/list")
    public Result<Page<SysLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation) {
        Page<SysLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(username), SysLog::getUsername, username)
                    .like(StringUtils.hasText(operation), SysLog::getOperation, operation)
                    .orderByDesc(SysLog::getCreateTime);
        return Result.success(sysLogService.page(pageObj, queryWrapper));
    }
}
