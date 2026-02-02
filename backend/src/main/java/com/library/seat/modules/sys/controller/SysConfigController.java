package com.library.seat.modules.sys.controller;

import com.library.seat.common.Result;
import com.library.seat.modules.sys.entity.SysConfig;
import com.library.seat.modules.sys.service.ISysConfigService;
import com.library.seat.modules.sys.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/v1/configs")
public class SysConfigController {

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysLogService sysLogService;

    @Operation(summary = "获取所有配置")
    @GetMapping("/list")
    public Result<List<SysConfig>> list() {
        return Result.success(configService.list());
    }

    @Operation(summary = "更新配置")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('admin', 'librarian')")
    public Result<Boolean> update(@RequestBody SysConfig config) {
        SysConfig old = configService.getById(config.getId());
        boolean success = configService.updateById(config);
        if (success && old != null) {
            String detail = String.format("配置项: %s, 值变更: %s -> %s", 
                old.getConfigName(), old.getConfigValue(), config.getConfigValue());
            // 获取当前用户名，假设这里能获取到
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            sysLogService.log(username, "更新系统配置", detail);
        }
        return Result.success(success);
    }
}
