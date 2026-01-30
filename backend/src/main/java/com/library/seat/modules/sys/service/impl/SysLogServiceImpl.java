package com.library.seat.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.modules.sys.entity.SysLog;
import com.library.seat.modules.sys.mapper.SysLogMapper;
import com.library.seat.modules.sys.service.SysLogService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {
    
    @Override
    public void log(String username, String operation, String content) {
        SysLog log = new SysLog();
        log.setUsername(username);
        log.setOperation(operation);
        log.setContent(content);
        log.setCreateTime(new Date());
        this.save(log);
    }
}
