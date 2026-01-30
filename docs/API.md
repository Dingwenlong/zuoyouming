# API 接口文档

## 1. 基础说明
*   **Base URL**: `http://localhost:8080/api`
*   **认证方式**: Header `Token: <jwt_token>`
*   **通用响应结构**:
    ```json
    {
      "code": 200, // 200:成功, 401:未登录, 500:错误
      "msg": "Success",
      "data": {}
    }
    ```

---

## 2. 认证模块 (`Auth`)

### 2.1 用户登录
*   **URL**: `/auth/login`
*   **Method**: `POST`
*   **无需认证**
*   **请求参数**:
    ```json
    {
      "account": "admin",
      "password": "password"
    }
    ```
*   **响应数据**:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9..."
    }
    ```

---

## 3. 用户模块 (`User`)

### 3.1 获取用户信息
*   **URL**: `/sys/user/info`
*   **Method**: `GET`
*   **响应数据**:
    ```json
    {
      "id": 1,
      "account": "admin",
      "nickname": "System Admin",
      "avatar": "",
      "permissions": ["*:*:*"]
    }
    ```

---

## 4. 座位管理模块 (`Seat`)

### 4.1 获取阅览室列表
*   **URL**: `/seat/room/list`
*   **Method**: `GET`
*   **响应数据**:
    ```json
    [
      {
        "id": 1,
        "name": "Reading Room 1",
        "floor": 1,
        "description": "Quiet Area"
      }
    ]
    ```

### 4.2 获取座位列表
*   **URL**: `/seat/list`
*   **Method**: `GET`
*   **请求参数**:
    *   `roomId` (query, required): 阅览室ID
*   **响应数据**:
    ```json
    [
      {
        "id": 101,
        "seatNumber": "A01",
        "type": 1,
        "status": 0, // 0:空闲 1:预约 2:使用中...
        "xCoord": 1,
        "yCoord": 1
      }
    ]
    ```

### 4.3 保存/更新座位 (管理员)
*   **URL**: `/seat/save`
*   **Method**: `POST`
*   **请求参数**:
    ```json
    {
      "roomId": 1,
      "seatNumber": "A01",
      "xCoord": 1,
      "yCoord": 1
    }
    ```

---

## 5. 预约业务模块 (`Reservation`)

### 5.1 提交预约
*   **URL**: `/seat/reserve`
*   **Method**: `POST`
*   **请求参数**:
    ```json
    {
      "seatId": 101,
      "startTime": "2023-10-01 10:00:00",
      "endTime": "2023-10-01 12:00:00"
    }
    ```
*   **说明**: 系统会自动进行时间冲突检测与分布式锁抢占。

### 5.2 签到
*   **URL**: `/seat/reserve/checkin`
*   **Method**: `POST`
*   **请求参数**:
    ```json
    {
      "seatId": 101
    }
    ```
*   **说明**: 必须在预约开始时间前后规定范围内签到，成功后状态变更为“使用中”。
