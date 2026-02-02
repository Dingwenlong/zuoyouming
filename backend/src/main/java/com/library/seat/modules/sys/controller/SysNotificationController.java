package com.library.seat.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.seat.common.Result;
import com.library.seat.modules.sys.entity.SysNotification;
import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.service.ISysNotificationService;
import com.library.seat.modules.sys.service.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统通知", description = "消息中心接口")
@RestController
@RequestMapping("/api/v1/notifications")
public class SysNotificationController {

    @Autowired
    private ISysNotificationService notificationService;

    @Autowired
    private UserDetailsServiceImpl userService;

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        return user != null ? user.getId() : null;
    }

    @Operation(summary = "获取当前用户通知列表")
    @GetMapping("/list")
    public Result<Page<SysNotification>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.page(new Page<>(page, size), 
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .orderByDesc(SysNotification::getCreateTime)));
    }

    @Operation(summary = "标记通知为已读")
    @PutMapping("/{id}/read")
    public Result<Boolean> read(@PathVariable Long id) {
        SysNotification notification = notificationService.getById(id);
        if (notification != null) {
            notification.setIsRead(1);
            return Result.success(notificationService.updateById(notification));
        }
        return Result.error("Notification not found");
    }

    @Operation(summary = "全部标记为已读")
    @PutMapping("/read-all")
    public Result<Boolean> readAll() {
        Long userId = getCurrentUserId();
        List<SysNotification> list = notificationService.list(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0));
        for (SysNotification n : list) {
            n.setIsRead(1);
        }
        return Result.success(notificationService.updateBatchById(list));
    }

    @Operation(summary = "获取未读消息数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = getCurrentUserId();
        return Result.success(notificationService.count(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0)));
    }
}
