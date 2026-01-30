# 后端接口实现指南

## 1. 数据库设计 (Schema)

### 用户表 (sys_user)
```sql
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(128) NOT NULL,
  `role` varchar(32) NOT NULL COMMENT 'student, librarian, admin',
  `avatar` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
);
```

### 菜单表 (sys_menu)
```sql
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `name` varchar(64) NOT NULL,
  `path` varchar(128) NOT NULL,
  `title` varchar(64) NOT NULL,
  `icon` varchar(64) DEFAULT NULL,
  `roles` varchar(255) DEFAULT NULL COMMENT 'JSON array of roles: ["admin", "student"]',
  `sort_order` int DEFAULT 0,
  PRIMARY KEY (`id`)
);
```

## 2. API 接口定义 (Spring Boot 示例)

### AuthController.java

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody LoginParams params) {
        // 1. 校验用户名密码
        SysUser user = userService.login(params.getUsername(), params.getPassword());
        
        // 2. 生成 Token (JWT)
        String token = JwtUtils.createToken(user.getId(), user.getRole());
        
        // 3. 返回结果
        LoginResult result = new LoginResult();
        result.setToken(token);
        result.setUserInfo(user);
        return Result.success(result);
    }
}
```

### MenuController.java

```java
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping("/list")
    public Result<List<MenuItem>> getMenus(@RequestHeader("Authorization") String token) {
        // 1. 从 Token 解析角色
        String role = JwtUtils.getRole(token);
        
        // 2. 查询该角色可见的菜单树
        List<MenuItem> menus = menuService.getMenusByRole(role);
        
        return Result.success(menus);
    }
}
```

### MenuService.java (逻辑)

```java
public List<MenuItem> getMenusByRole(String role) {
    // 1. 查询所有菜单
    List<SysMenu> allMenus = menuMapper.findAll();
    
    // 2. 过滤权限
    List<SysMenu> allowedMenus = allMenus.stream()
        .filter(menu -> checkPermission(menu.getRoles(), role))
        .collect(Collectors.toList());
        
    // 3. 构建树形结构
    return TreeUtils.build(allowedMenus);
}

private boolean checkPermission(String rolesJson, String userRole) {
    if (StringUtils.isEmpty(rolesJson)) return true;
    List<String> roles = JsonUtils.parseArray(rolesJson, String.class);
    return roles.contains(userRole);
}
```

## 3. 权限校验拦截器 (Interceptor/Filter)

```java
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token) || !JwtUtils.verify(token)) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
```
