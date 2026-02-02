package com.library.seat.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.modules.sys.entity.SysConfig;
import com.library.seat.modules.sys.mapper.SysConfigMapper;
import com.library.seat.modules.sys.service.ISysConfigService;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    @Override
    public String getValue(String key, String defaultValue) {
        SysConfig config = this.getOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        try {
            String value = getValue(key, null);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
