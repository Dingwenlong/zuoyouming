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
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `credit_score` int(11) DEFAULT '100' COMMENT '信用分',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除 0:未删除 1:已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
-- password: 123456 (BCrypt)
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$7JB720yubVSZv5W56jdx.euT/bUZXW.Q5.F.hTa.W.s.s.s', 'System Admin', '', 'admin@library.com', '13800000000', 100, 1, NOW(), NOW(), 0);

SET FOREIGN_KEY_CHECKS = 1;
