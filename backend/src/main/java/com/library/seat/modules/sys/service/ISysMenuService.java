package com.library.seat.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.seat.modules.sys.entity.SysMenu;

import java.util.List;

public interface ISysMenuService extends IService<SysMenu> {
    List<SysMenu> getMenusByRole(String role);
}
