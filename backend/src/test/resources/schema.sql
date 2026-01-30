-- 1.1 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt加密密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `role` varchar(20) DEFAULT 'student' COMMENT '角色: student, librarian, admin',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `credit_score` int(11) DEFAULT '100' COMMENT '信用分',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态: active (正常), banned (封禁)',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是',
  `open_id` varchar(64) DEFAULT NULL COMMENT '微信OpenID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_open_id` (`open_id`)
);

-- 1.2 座位表
CREATE TABLE IF NOT EXISTS `sys_seat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `seat_no` varchar(20) NOT NULL COMMENT '座位号 (如 A-01)',
  `area` varchar(20) NOT NULL COMMENT '区域 (A区/B区...)',
  `type` varchar(20) NOT NULL COMMENT '类型 (标准/靠窗/插座)',
  `status` varchar(20) DEFAULT 'available' COMMENT '状态: available, occupied, maintenance',
  `x_coord` int(11) DEFAULT '0' COMMENT '平面图 X 坐标',
  `y_coord` int(11) DEFAULT '0' COMMENT '平面图 Y 坐标',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seat_no` (`seat_no`)
);

-- 1.3 预约记录表
CREATE TABLE IF NOT EXISTS `sys_reservation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '关联用户ID',
  `seat_id` bigint(20) NOT NULL COMMENT '关联座位ID',
  `start_time` datetime NOT NULL COMMENT '预约开始时间',
  `end_time` datetime NOT NULL COMMENT '预约结束时间',
  `deadline` datetime DEFAULT NULL COMMENT '签到/暂离截止时间',
  `status` varchar(20) DEFAULT 'reserved' COMMENT 'reserved, checked_in, completed, cancelled, violation',
  `type` varchar(20) DEFAULT 'appointment' COMMENT '类型: appointment, immediate',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是',
  PRIMARY KEY (`id`)
);

-- 1.6 系统日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作人账号',
  `operation` varchar(100) DEFAULT NULL COMMENT '操作类型',
  `content` text DEFAULT NULL COMMENT '操作内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
);

-- 1.4 违规申诉表
CREATE TABLE IF NOT EXISTS `sys_appeal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reservation_id` bigint(20) NOT NULL COMMENT '关联预约记录ID',
  `reason` text NOT NULL COMMENT '申诉理由',
  `images` text DEFAULT NULL COMMENT '图片凭证 (JSON数组或逗号分隔)',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending (待审核), approved (通过), rejected (驳回)',
  `reply` text DEFAULT NULL COMMENT '管理员回复',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是',
  PRIMARY KEY (`id`)
);

-- 1.5 系统菜单表 (Optional, but needed for UserController)
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父菜单ID',
  `name` varchar(50) DEFAULT NULL COMMENT '菜单名称',
  `path` varchar(100) DEFAULT NULL COMMENT '路由路径',
  `title` varchar(50) DEFAULT NULL COMMENT '菜单标题',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `roles` varchar(255) DEFAULT NULL COMMENT '权限角色JSON',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序',
  `deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是',
  PRIMARY KEY (`id`)
);
