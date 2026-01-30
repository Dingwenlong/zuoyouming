# 数据库设计文档

## 1. 概述
本项目采用 **MySQL 8.0** 作为关系型数据库，存储用户信息、空间资源及业务记录。数据库名称为 `library_seat`。

---

## 2. 数据表设计

### 2.1 用户表 (`sys_user`)
存储系统用户的基本信息、认证凭证及信用分。

| 字段名 | 类型 | 长度 | 允许空 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **id** | bigint | 20 | NO | 主键ID (Auto Increment) |
| username | varchar | 64 | NO | 用户名 (Unique) |
| password | varchar | 128 | NO | 密码 (BCrypt加密) |
| nickname | varchar | 64 | YES | 昵称 |
| avatar | varchar | 255 | YES | 头像URL |
| email | varchar | 64 | YES | 邮箱 |
| mobile | varchar | 20 | YES | 手机号 |
| credit_score | int | 11 | YES | 信用分 (默认100) |
| status | tinyint | 4 | YES | 状态 (0:禁用 1:正常) |
| create_time | datetime | - | YES | 创建时间 |
| update_time | datetime | - | YES | 更新时间 |
| deleted | tinyint | 4 | YES | 逻辑删除 (0:否 1:是) |

### 2.2 阅览室表 (`tb_room`)
定义图书馆的物理空间区域。

| 字段名 | 类型 | 长度 | 允许空 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **id** | bigint | 20 | NO | 主键ID |
| name | varchar | 64 | NO | 阅览室名称 |
| floor | int | 11 | YES | 所在楼层 |
| description | varchar | 255 | YES | 描述信息 |
| deleted | tinyint | 4 | YES | 逻辑删除 |

### 2.3 座位表 (`tb_seat`)
定义具体的座位资源及其属性。

| 字段名 | 类型 | 长度 | 允许空 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **id** | bigint | 20 | NO | 主键ID |
| room_id | bigint | 20 | NO | 所属阅览室ID (FK) |
| seat_number | varchar | 20 | NO | 座位编号 (如 A01) |
| type | tinyint | 4 | YES | 类型 (1:普通 2:靠窗 3:插座) |
| status | tinyint | 4 | YES | **状态** (0:空闲 1:预约 2:使用中 3:暂离 4:故障) |
| x_coord | int | 11 | YES | 布局X坐标 (Grid Column) |
| y_coord | int | 11 | YES | 布局Y坐标 (Grid Row) |
| deleted | tinyint | 4 | YES | 逻辑删除 |

### 2.4 预约记录表 (`tb_reservation`)
记录用户的预约行为及状态流转。

| 字段名 | 类型 | 长度 | 允许空 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **id** | bigint | 20 | NO | 主键ID |
| user_id | bigint | 20 | NO | 用户ID (FK) |
| seat_id | bigint | 20 | NO | 座位ID (FK) |
| start_time | datetime | - | NO | 预约开始时间 |
| end_time | datetime | - | NO | 预约结束时间 |
| status | tinyint | 4 | YES | **状态** (0:待签到 1:已签到 2:已完成 3:已取消 4:违约) |
| create_time | datetime | - | YES | 创建时间 |
| update_time | datetime | - | YES | 更新时间 |
| deleted | tinyint | 4 | YES | 逻辑删除 |

---

## 3. 索引设计
*   `sys_user`: `uk_username` (Unique)
*   `tb_seat`: `uk_room_seat` (room_id + seat_number)
*   `tb_reservation`: 
    *   `idx_user_status` (user_id, status) - 用于查询用户当前预约
    *   `idx_seat_time` (seat_id, start_time, end_time) - 用于冲突检测
