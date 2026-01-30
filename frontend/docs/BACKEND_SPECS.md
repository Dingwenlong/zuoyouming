# 图书馆座位预约系统 - 后端开发需求规格说明书

本文档旨在为后端开发人员提供详细的接口定义、数据库设计及业务逻辑实现指南，确保与现有前端（Vue 3 + TypeScript）无缝对接。

---

## 1. 数据库设计 (Database Design)

建议使用 MySQL 8.0+ 或 PostgreSQL 13+。

### 1.1 用户表 (`users`)
存储系统用户信息。

| 字段名 | 类型 | 必填 | 默认值 | 描述 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键 |
| `username` | VARCHAR(50) | YES | - | 学号/工号 (唯一) |
| `password` | VARCHAR(100) | YES | - | 加密后的密码 |
| `real_name` | VARCHAR(50) | NO | NULL | 真实姓名 |
| `role` | VARCHAR(20) | YES | 'student' | 角色: `student`, `admin`, `librarian` |
| `avatar` | VARCHAR(255) | NO | NULL | 头像URL |
| `phone` | VARCHAR(20) | NO | NULL | 联系电话 |
| `credit_score` | INT | YES | 100 | 信用分 |
| `status` | VARCHAR(20) | YES | 'active' | 状态: `active` (正常), `banned` (封禁) |
| `created_at` | DATETIME | YES | NOW() | 创建时间 |

### 1.2 座位表 (`seats`)
存储图书馆座位信息。

| 字段名 | 类型 | 必填 | 默认值 | 描述 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键 |
| `seat_no` | VARCHAR(20) | YES | - | 座位编号 (如 "A-001") |
| `area_id` | INT | NO | NULL | 区域ID (预留) |
| `type` | VARCHAR(20) | YES | 'standard' | 类型: `standard` (普通), `window` (靠窗), `sofa` (沙发) |
| `x` | INT | NO | 0 | 坐标X (用于可视化地图) |
| `y` | INT | NO | 0 | 坐标Y (用于可视化地图) |
| `status` | VARCHAR(20) | YES | 'available' | 状态: `available`, `occupied`, `maintenance` |

### 1.3 预约记录表 (`reservations`)
核心业务表，记录每一次预约/入座流程。

| 字段名 | 类型 | 必填 | 默认值 | 描述 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键 |
| `user_id` | BIGINT | YES | - | 外键 -> users.id |
| `seat_id` | BIGINT | YES | - | 外键 -> seats.id |
| `start_time` | DATETIME | YES | - | 预约开始时间 |
| `end_time` | DATETIME | NO | NULL | 结束时间 (退座时更新) |
| `deadline` | DATETIME | NO | NULL | 签到/暂离截止时间 |
| `status` | VARCHAR(20) | YES | 'reserved' | 状态: `reserved`(已预约), `checked_in`(使用中), `away`(暂离), `completed`(结束), `cancelled`(取消), `violation`(违规) |
| `type` | VARCHAR(20) | YES | 'appointment' | 类型: `appointment`(预约), `immediate`(扫码入座) |
| `created_at` | DATETIME | YES | NOW() | 创建时间 |

### 1.4 违规申诉表 (`appeals`)
处理用户针对违规记录的申诉。

| 字段名 | 类型 | 必填 | 默认值 | 描述 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | YES | AUTO_INCREMENT | 主键 |
| `reservation_id` | BIGINT | YES | - | 外键 -> reservations.id |
| `reason` | TEXT | YES | - | 申诉理由 |
| `images` | JSON | NO | NULL | 图片凭证 URL 数组 |
| `status` | VARCHAR(20) | YES | 'pending' | 状态: `pending`(待处理), `approved`(通过), `rejected`(驳回) |
| `admin_reply` | TEXT | NO | NULL | 管理员回复 |
| `created_at` | DATETIME | YES | NOW() | 创建时间 |

---

## 2. 接口设计 (API Specification)

**通用约定**:
- 响应格式:
  ```json
  {
    "code": 200,
    "msg": "Success",
    "data": { ... }
  }
  ```
- 鉴权方式: Header `Authorization: Bearer <token>`

### 2.1 认证模块 (Auth)

#### 登录
- **URL**: `POST /api/auth/login`
- **Params**: `{ "username": "...", "password": "..." }`
- **Response**: `{ "token": "...", "userInfo": { ... } }`

#### 注册
- **URL**: `POST /api/auth/register`
- **Params**: `{ "username": "...", "password": "..." }`
- **Response**: `{ "token": "...", "userInfo": { ... } }`

#### 获取用户信息
- **URL**: `GET /api/auth/info`
- **Response**: `{ "id": 1, "username": "...", "role": "student", ... }`

### 2.2 座位模块 (Seat)

#### 获取座位列表
- **URL**: `GET /api/seats`
- **Response**: `[ { "id": 1, "seat_no": "A-001", "status": "available", "x": 10, "y": 20 }, ... ]`
- **Note**: 前端 `SeatMap.vue` 依赖 `x`, `y` 坐标渲染。

#### 更新座位状态 (管理员)
- **URL**: `PUT /api/seats/:id/status`
- **Params**: `{ "status": "maintenance" }`

### 2.3 预约模块 (Reservation)

#### 创建预约
- **URL**: `POST /api/reservations`
- **Params**: `{ "seatId": 1 }`
- **Logic**:
  1. 检查用户是否有未完成的预约。
  2. 检查座位当前是否 `available`。
  3. 创建记录，状态设为 `reserved`。
  4. 设置 `deadline` 为当前时间 + 15分钟。
  5. 更新 `seats` 表状态为 `occupied` (或锁定状态)。

#### 签到
- **URL**: `POST /api/reservations/:id/check-in`
- **Logic**:
  1. 验证是否在 `deadline` 之前。
  2. 更新状态为 `checked_in`。
  3. 清除 `deadline`。

#### 暂离
- **URL**: `POST /api/reservations/:id/leave`
- **Logic**:
  1. 更新状态为 `away`。
  2. 设置 `deadline` 为当前时间 + 30分钟。

#### 释放/退座
- **URL**: `POST /api/reservations/:id/release`
- **Logic**:
  1. 更新状态为 `completed`。
  2. 更新 `seats` 表状态为 `available`。
  3. 记录 `end_time`。

#### 获取我的历史
- **URL**: `GET /api/reservations/my-history`
- **Response**: `[ { "id": 1, "status": "completed", "seatNumber": "A-001", ... } ]`

### 2.4 申诉模块 (Appeal)

#### 提交申诉
- **URL**: `POST /api/reservations/:id/appeal`
- **Params**: `{ "reason": "...", "images": ["url1", "url2"] }`

---

## 3. 业务逻辑与实现注意事项 (Business Logic Notes)

### 3.1 状态机流转 (State Machine)
后端需严格控制以下状态流转，防止非法操作：
1. `reserved` (已预约)
   - -> `checked_in` (用户签到)
   - -> `violation` (超时未签到 - **后台定时任务**)
   - -> `cancelled` (用户主动取消)
2. `checked_in` (使用中)
   - -> `away` (用户点击暂离)
   - -> `completed` (用户退座)
3. `away` (暂离中)
   - -> `checked_in` (用户回来签到)
   - -> `violation` (超时未归 - **后台定时任务**)
   - -> `completed` (用户直接退座)

### 3.2 定时任务 (Cron Jobs)
后端需要运行定时任务（建议每分钟执行一次）：
1. **预约签到超时检查**:
   - 查询所有 `status='reserved'` 且 `deadline < NOW()` 的记录。
   - 更新为 `violation`。
   - 释放对应座位 (`seats.status = 'available'`)。
   - 扣除用户信用分。
2. **暂离超时检查**:
   - 查询所有 `status='away'` 且 `deadline < NOW()` 的记录。
   - 更新为 `violation` (或 `completed`，视业务规则而定，通常超时未归算违规)。
   - 释放对应座位。

### 3.3 WebSocket 实时通知
前端 `src/layouts/BasicLayout.vue` 已集成 WebSocket 监听。后端需在以下事件发生时推送消息：
- **Channel**: `/ws` (或 Socket.IO)
- **Event Types**:
  - `seat.update`: 当任一座位状态改变时广播，用于刷新地图。
  - `alert`: 定向推送给特定用户。
    - "您的预约即将超时，请尽快签到"
    - "您已暂离超过 20 分钟"
    - "您的座位已被强制释放"

### 3.4 信用分体系
- 初始分: 100
- 违规扣分: -10/次
- 正常履约奖励: +1/次 (可选)
- 封禁阈值: < 60 分禁止预约。

### 3.5 管理员权限
- 管理员可强制释放任意座位 (`/api/seats/:id/force-release`)。
- 管理员可处理申诉 (`/api/appeals/:id/review`)。

---

## 4. 前后端联调配置
- 前端通过 `.env` 文件控制 API 地址。
- 确保后端跨域 (CORS) 配置允许前端域名。
- 推荐使用 Swagger/OpenAPI 导出接口文档供前端直接生成类型。
