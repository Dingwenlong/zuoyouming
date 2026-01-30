package com.library.seat.modules.sys.controller;

import com.library.seat.common.Result;
import com.library.seat.common.utils.JwtUtils;
import com.library.seat.modules.sys.entity.SysMenu;
import com.library.seat.modules.sys.service.ISysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "菜单管理", description = "动态菜单与路由接口")
@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private JwtUtils jwtUtils;

    @Operation(summary = "获取用户菜单", description = "根据当前用户角色获取菜单树")
    @GetMapping("/list")
    public Result<List<SysMenu>> getMenus(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " prefix if present
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        String role = jwtUtils.getRole(token);
        List<SysMenu> menus = menuService.getMenusByRole(role);
        return Result.success(menus);
    }
}
