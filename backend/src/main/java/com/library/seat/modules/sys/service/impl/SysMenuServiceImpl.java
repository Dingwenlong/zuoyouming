package com.library.seat.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.seat.common.utils.JsonUtils;
import com.library.seat.common.utils.TreeUtils;
import com.library.seat.modules.sys.entity.SysMenu;
import com.library.seat.modules.sys.mapper.SysMenuMapper;
import com.library.seat.modules.sys.service.ISysMenuService;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    @Override
    public List<SysMenu> getMenusByRole(String role) {
        // 1. Query all menus
        List<SysMenu> allMenus = list();

        // 2. Filter permissions
        List<SysMenu> allowedMenus = allMenus.stream()
                .filter(menu -> checkPermission(menu.getRoles(), role))
                .collect(Collectors.toList());

        // 3. Build tree
        return TreeUtils.build(allowedMenus);
    }

    private boolean checkPermission(String rolesJson, String userRole) {
        // If no roles defined, maybe public or hidden? Assuming public as per user guide logic "if empty return true"
        if (StringUtils.isEmpty(rolesJson)) return true;
        
        // Admin usually sees everything
        if ("admin".equals(userRole)) return true;
        
        List<String> roles = JsonUtils.parseArray(rolesJson, String.class);
        return roles != null && roles.contains(userRole);
    }
}
