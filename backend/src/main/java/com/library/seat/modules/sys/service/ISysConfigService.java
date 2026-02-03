package com.library.seat.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.seat.modules.sys.entity.SysConfig;

public interface ISysConfigService extends IService<SysConfig> {
    String getValue(String key, String defaultValue);
    int getIntValue(String key, int defaultValue);
    double getDoubleValue(String key, double defaultValue);
}
