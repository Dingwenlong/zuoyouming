package com.library.seat.modules.sys.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.library.seat.modules.sys.entity.SysMessage;

public interface ISysMessageService extends IService<SysMessage> {
    Page<SysMessage> getMessagePage(Page<SysMessage> page);
    SysMessage getOneWithInfo(Long id);
}
