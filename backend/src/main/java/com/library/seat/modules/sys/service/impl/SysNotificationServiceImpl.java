package com.library.seat.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.modules.sys.entity.SysNotification;
import com.library.seat.modules.sys.mapper.SysNotificationMapper;
import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.mapper.SysUserMapper;
import com.library.seat.modules.sys.service.ISysNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SysNotificationServiceImpl extends ServiceImpl<SysNotificationMapper, SysNotification> implements ISysNotificationService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void send(Long userId, String title, String content, String type) {
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setCreateTime(new Date());
        this.save(notification);

        // 获取用户名用于 WebSocket 推送 (Spring Security 使用 username 作为 Principal name)
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    notification
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToAll(String title, String content, String type) {
        List<SysUser> users = userMapper.selectList(null);
        Date now = new Date();
        List<SysNotification> notifications = new ArrayList<>();
        
        for (SysUser user : users) {
            SysNotification notification = new SysNotification();
            notification.setUserId(user.getId());
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setIsRead(0);
            notification.setCreateTime(now);
            notifications.add(notification);
        }
        
        // 批量保存
        this.saveBatch(notifications);

        // 批量发送 WebSocket 消息
        for (int i = 0; i < users.size(); i++) {
            messagingTemplate.convertAndSendToUser(
                    users.get(i).getUsername(),
                    "/queue/notifications",
                    notifications.get(i)
            );
        }
    }
}
