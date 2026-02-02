CREATE DATABASE IF NOT EXISTS library_seat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE library_seat;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- Table structure for sys_seat
-- ----------------------------
DROP TABLE IF EXISTS `sys_seat`;
CREATE TABLE `sys_seat` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- ----------------------------
-- Table structure for sys_reservation
-- ----------------------------
DROP TABLE IF EXISTS `sys_reservation`;
CREATE TABLE `sys_reservation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '关联用户ID',
  `seat_id` bigint(20) NOT NULL COMMENT '关联座位ID',
  `start_time` datetime NOT NULL COMMENT '预约开始时间',
  `end_time` datetime NOT NULL COMMENT '预约结束时间',
  `deadline` datetime DEFAULT NULL COMMENT '签到/暂离截止时间',
  `status` varchar(20) DEFAULT 'reserved' COMMENT 'reserved, checked_in, completed, cancelled, violation',
  `type` varchar(20) DEFAULT 'appointment' COMMENT '类型: appointment, immediate',
  `reservation_date` date DEFAULT NULL COMMENT '预约日期',
  `slot` varchar(20) DEFAULT NULL COMMENT '时段: morning, afternoon, evening',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_seat_id` (`seat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- ----------------------------
-- Table structure for sys_appeal
-- ----------------------------
DROP TABLE IF EXISTS `sys_appeal`;
CREATE TABLE `sys_appeal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reservation_id` bigint(20) NOT NULL COMMENT '关联预约记录ID',
  `reason` text NOT NULL COMMENT '申诉理由',
  `images` text DEFAULT NULL COMMENT '图片凭证 (JSON数组或逗号分隔)',
  `status` varchar(20) DEFAULT 'pending' COMMENT 'pending (待审核), approved (通过), rejected (驳回)',
  `reply` text DEFAULT NULL COMMENT '管理员回复',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是',
  PRIMARY KEY (`id`),
  KEY `idx_reservation_id` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规申诉表';

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作人账号',
  `operation` varchar(100) DEFAULT NULL COMMENT '操作类型',
  `content` text DEFAULT NULL COMMENT '操作内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
-- password: 123456
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `status`, `credit_score`) VALUES 
(1, 'admin', '$2a$10$/DicGv2VwwNohvyU2O29k.9fsie0WBUfmaw6oPUYB4lZnSsY/Rb4q', '管理员', 'admin', 'active', 100),
(2, 'lib', '$2a$10$/DicGv2VwwNohvyU2O29k.9fsie0WBUfmaw6oPUYB4lZnSsY/Rb4q', '图书馆员', 'librarian', 'active', 100),
(3, 'student', '$2a$10$/DicGv2VwwNohvyU2O29k.9fsie0WBUfmaw6oPUYB4lZnSsY/Rb4q', '学生', 'student', 'active', 100);

-- ----------------------------
-- Records of sys_seat (Initial Test Data)
-- ----------------------------
INSERT INTO `sys_seat` (`seat_no`, `area`, `type`, `status`, `x_coord`, `y_coord`) VALUES 
('A-01', 'A区', '靠窗', 'available', 100, 100),
('A-02', 'A区', '标准', 'available', 200, 100),
('A-03', 'A区', '插座', 'available', 300, 100),
('B-01', 'B区', '靠窗', 'available', 100, 200),
('B-02', 'B区', '标准', 'available', 200, 200);

SET FOREIGN_KEY_CHECKS = 1;
