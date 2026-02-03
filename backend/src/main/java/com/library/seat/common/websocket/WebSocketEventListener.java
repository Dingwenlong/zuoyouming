package com.library.seat.common.websocket;

import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.service.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserDetailsServiceImpl userService;

    // 存储在线用户: username -> sessionId
    private static final Map<String, String> onlineUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            String sessionId = headerAccessor.getSessionId();
            onlineUsers.put(username, sessionId);
            log.info("User connected: {}, sessionId: {}", username, sessionId);

            broadcastStatus(username, "active");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            onlineUsers.remove(username);
            log.info("User disconnected: {}", username);

            broadcastStatus(username, "offline");
        }
    }

    private void broadcastStatus(String username, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("username", username);
        message.put("status", status);
        message.put("event", "user_status_change");
        messagingTemplate.convertAndSend("/topic/online_status", message);
    }

    /**
     * 获取当前在线用户列表
     */
    public static boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }
}
