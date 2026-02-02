package com.library.seat.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.seat.modules.sys.entity.SysNotification;

public interface ISysNotificationService extends IService<SysNotification> {
    void send(Long userId, String title, String content, String type);
    void sendToAll(String title, String content, String type);
}
