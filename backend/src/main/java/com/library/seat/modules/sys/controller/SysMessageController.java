package com.library.seat.modules.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.seat.common.Result;
import com.library.seat.modules.sys.entity.SysMessage;
import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.service.ISysMessageService;
import com.library.seat.modules.sys.service.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@Tag(name = "消息广场", description = "消息广场社交接口")
@RestController
@RequestMapping("/api/v1/messages")
public class SysMessageController {

    @Autowired
    private ISysMessageService messageService;

    @Autowired
    private UserDetailsServiceImpl userService;

    @Autowired
    private com.library.seat.modules.sys.service.ISysNotificationService notificationService;

    @Autowired
    private com.library.seat.modules.sys.service.ISysConfigService configService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "发布消息")
    @PostMapping
    public Result<Boolean> post(@RequestBody SysMessage message) {
        // 检查消息广场是否开启
        String enabled = configService.getValue("message_square_enabled", "true");
        if (!"true".equalsIgnoreCase(enabled)) {
            return Result.error("消息广场发言功能已暂时关闭");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        
        message.setUserId(user.getId());
        message.setCreateTime(new Date());
        boolean success = messageService.save(message);
        
        if (success) {
            // 广播新消息
            SysMessage fullMessage = messageService.getOneWithInfo(message.getId());
            messagingTemplate.convertAndSend("/topic/messages", fullMessage);

            if (message.getAtUserId() != null) {
                notificationService.send(message.getAtUserId(), "消息广场提到你", user.getRealName() + " 在消息广场@了你。", "info");
            }
        }
        
        return Result.success(success);
    }

    @Operation(summary = "分页获取消息")
    @GetMapping("/list")
    public Result<Page<SysMessage>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(messageService.getMessagePage(new Page<>(page, size)));
    }

    @Operation(summary = "发送系统通知 (管理员)")
    @PostMapping("/system-notification")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Boolean> sendSystemNotification(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        String content = params.get("content");
        String type = params.getOrDefault("type", "info");
        
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return Result.error("标题和内容不能为空");
        }
        
        notificationService.sendToAll(title, content, type);
        return Result.success(true);
    }
}
