package com.library.seat.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.seat.common.Result;
import com.library.seat.modules.sys.entity.SysMenu;
import com.library.seat.modules.sys.entity.SysUser;
import com.library.seat.modules.sys.service.ISysMenuService;
import com.library.seat.modules.sys.service.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理", description = "系统用户管理接口")
@RestController
@RequestMapping("/api/v1/users")
public class SysUserController {

    @Autowired
    private UserDetailsServiceImpl userService;
    
    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Operation(summary = "分页获取用户列表")
    @GetMapping
    public Result<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status) {
        
        Page<SysUser> pageParam = new Page<>(page, size);
        return Result.success(userService.page(pageParam, new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .eq(StringUtils.hasText(status), SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime)));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Boolean> add(@RequestBody SysUser user) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return Result.success(userService.save(user));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return Result.success(userService.updateById(user));
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String status = params.get("status");
        if (status == null) {
            return Result.error("Status is required");
        }
        
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error("User not found");
        }
        
        user.setStatus(status);
        return Result.success(userService.updateById(user));
    }

    @Operation(summary = "获取当前用户菜单树")
    @GetMapping("/menus")
    public Result<List<SysMenu>> getMenus() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        
        return Result.success(menuService.getMenusByRole(user.getRole()));
    }
}
