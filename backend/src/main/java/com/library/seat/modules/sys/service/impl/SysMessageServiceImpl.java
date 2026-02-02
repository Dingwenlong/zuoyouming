package com.library.seat.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.modules.reservation.entity.Reservation;
import com.library.seat.modules.reservation.service.ReservationService;
import com.library.seat.modules.seat.entity.Seat;
import com.library.seat.modules.seat.service.SeatService;
import com.library.seat.modules.sys.entity.SysMessage;
import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.mapper.SysMessageMapper;
import com.library.seat.modules.sys.service.ISysMessageService;
import com.library.seat.modules.sys.service.UserDetailsServiceImpl;
import com.library.seat.common.websocket.WebSocketEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage> implements ISysMessageService {

    @Autowired
    private UserDetailsServiceImpl userService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SeatService seatService;

    @Override
    public Page<SysMessage> getMessagePage(Page<SysMessage> page) {
        Page<SysMessage> result = this.page(page, new LambdaQueryWrapper<SysMessage>().orderByDesc(SysMessage::getCreateTime));
        
        for (SysMessage msg : result.getRecords()) {
            fillMessageInfo(msg);
        }
        return result;
    }

    @Override
    public SysMessage getOneWithInfo(Long id) {
        SysMessage msg = this.getById(id);
        if (msg != null) {
            fillMessageInfo(msg);
        }
        return msg;
    }

    private void fillMessageInfo(SysMessage msg) {
        SysUser user = userService.getById(msg.getUserId());
        if (user != null) {
            msg.setUsername(user.getUsername());
            msg.setRealName(user.getRealName());
            msg.setRole(user.getRole());
            msg.setAvatar(user.getAvatar());
            
            // 使用 WebSocket 实时在线状态
            msg.setStatus(WebSocketEventListener.isOnline(user.getUsername()) ? "active" : "offline");
            
            // 获取当前占用座位
            Reservation activeRes = reservationService.getOne(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getUserId, user.getId())
                    .in(Reservation::getStatus, "reserved", "checked_in", "away")
                    .last("LIMIT 1"));
            
            if (activeRes != null) {
                Seat seat = seatService.getById(activeRes.getSeatId());
                if (seat != null) {
                    msg.setSeatNo(seat.getSeatNo());
                }
            }
        }
    }
}
