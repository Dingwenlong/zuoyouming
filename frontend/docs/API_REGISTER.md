# 用户注册接口文档 (User Registration API)

本文档描述了图书馆座位预约系统中用户注册功能的后端接口规范。

## 1. 接口概述

*   **URL**: `/auth/register`
*   **Method**: `POST`
*   **Content-Type**: `application/json`
*   **描述**: 接收新用户的账号和密码，创建新账户并自动登录，返回认证 Token。

---

## 2. 请求参数 (Request Body)

| 字段名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :--- | :--- | :--- |
| `username` | String | 是 | 用户名或学号，作为系统唯一标识 | `"2021001"` |
| `password` | String | 是 | 用户密码（建议传输前或后端接收后进行哈希处理） | `"password123"` |

**请求示例:**
```json
{
  "username": "2021001",
  "password": "password123"
}
```

---

## 3. 响应结果 (Response)

### 3.1 成功响应 (200 OK)

注册成功后，直接返回登录凭证，无需用户再次登录。

```json
{
  "code": 200,
  "msg": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 1001,
      "username": "2021001",
      "role": "student",
      "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=2021001",
      "phone": null
    }
  }
}
```

**字段说明:**

*   `token`: JWT 认证令牌，后续请求需放入 Header (`Authorization: Bearer <token>`)。
*   `userInfo`: 用户基本信息对象。
    *   `role`: 默认注册角色应设为 `"student"`。
    *   `avatar`: 建议后端生成一个默认头像或使用 Gravatar/DiceBear 服务。

### 3.2 失败响应

**用户名已存在 (400 Bad Request):**
```json
{
  "code": 400,
  "msg": "用户名已存在，请直接登录",
  "data": null
}
```

**参数校验错误 (400 Bad Request):**
```json
{
  "code": 400,
  "msg": "密码长度不能少于6位",
  "data": null
}
```

---

## 4. 业务逻辑注意事项

1.  **密码安全**: 数据库中**严禁**存储明文密码。请使用 BCrypt 或 Argon2 等算法进行加盐哈希存储。
2.  **默认角色**: 所有通过此接口注册的用户，默认角色 (`role`) 必须设置为 `"student"`。管理员账号不应通过此公开接口注册。
3.  **防刷限制**: 建议对同一 IP 的注册请求频率进行限制（如 1分钟内不超过 5 次）。
4.  **数据完整性**: 注册时建议同时初始化用户的扩展信息表（如 `users` 表中的 `credit_score` 默认为 100）。
