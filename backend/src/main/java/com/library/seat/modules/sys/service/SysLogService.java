package com.library.seat.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.seat.modules.sys.entity.SysLog;

public interface SysLogService extends IService<SysLog> {
    void log(String username, String operation, String content);
}
