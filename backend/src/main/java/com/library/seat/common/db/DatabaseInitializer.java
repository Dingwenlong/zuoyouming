package com.library.seat.common.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库初始化器
 * 负责在应用启动时检查并创建必要的表结构和初始数据
 * 兼容本地开发和 Docker 环境
 */
@Slf4j
@Component
public class DatabaseInitializer {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        log.info("Starting database initialization...");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. 创建系统用户表
            log.info("Checking/Creating table: sys_user");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_user` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`username` varchar(50) NOT NULL COMMENT '登录账号'," +
                    "`password` varchar(100) NOT NULL COMMENT 'BCrypt加密密码'," +
                    "`real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名'," +
                    "`role` varchar(20) DEFAULT 'student' COMMENT '角色: student, librarian, admin'," +
                    "`phone` varchar(20) DEFAULT NULL COMMENT '手机号'," +
                    "`avatar` varchar(255) DEFAULT NULL COMMENT '头像URL'," +
                    "`credit_score` int(11) DEFAULT '100' COMMENT '信用分'," +
                    "`status` varchar(20) DEFAULT 'active' COMMENT '状态: active (正常), banned (封禁)'," +
                    "`last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "`open_id` varchar(64) DEFAULT NULL COMMENT '微信OpenID'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_username` (`username`)," +
                    "UNIQUE KEY `uk_open_id` (`open_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表'");
            
            // 兼容性字段检查 (ALTER TABLE)
            String[] userAlters = {
                "ALTER TABLE `sys_user` CHANGE COLUMN `nickname` `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名'",
                "ALTER TABLE `sys_user` ADD COLUMN `role` varchar(20) DEFAULT 'student' COMMENT '角色: student, librarian, admin'",
                "ALTER TABLE `sys_user` ADD COLUMN `phone` varchar(20) DEFAULT NULL COMMENT '手机号'",
                "ALTER TABLE `sys_user` ADD COLUMN `credit_score` int(11) DEFAULT '100' COMMENT '信用分'",
                "ALTER TABLE `sys_user` MODIFY COLUMN `status` varchar(20) DEFAULT 'active' COMMENT '状态: active (正常), banned (封禁)'",
                "ALTER TABLE `sys_user` ADD COLUMN `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间'",
                "ALTER TABLE `sys_user` ADD COLUMN `open_id` varchar(64) DEFAULT NULL COMMENT '微信OpenID'",
                "ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_open_id` (`open_id`)"
            };
            for (String sql : userAlters) {
                try { stmt.execute(sql); } catch (SQLException e) { /* Ignore existing column/key */ }
            }

            // 2. 插入或更新默认账号
            log.info("Checking/Upserting default users");
            String adminPassword = "$2a$10$/DicGv2VwwNohvyU2O29k.9fsie0WBUfmaw6oPUYB4lZnSsY/Rb4q";
            
            String[] users = {
                "INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES ('admin', '" + adminPassword + "', '管理员', 'admin', 'active') ON DUPLICATE KEY UPDATE `password` = '" + adminPassword + "', `role` = 'admin'",
                "INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES ('lib', '" + adminPassword + "', '图书馆员', 'librarian', 'active') ON DUPLICATE KEY UPDATE `password` = '" + adminPassword + "', `role` = 'librarian'",
                "INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES ('student', '" + adminPassword + "', '学生', 'student', 'active') ON DUPLICATE KEY UPDATE `password` = '" + adminPassword + "', `role` = 'student'"
            };
            for (String sql : users) {
                stmt.execute(sql);
            }

            // 3. 创建座位表
            log.info("Checking/Creating table: sys_seat");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_seat` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`seat_no` varchar(20) NOT NULL COMMENT '座位号 (如 A-01)'," +
                    "`area` varchar(20) NOT NULL COMMENT '区域 (A区/B区...)'," +
                    "`type` varchar(20) NOT NULL COMMENT '类型 (标准/靠窗/插座)'," +
                    "`status` varchar(20) DEFAULT 'available' COMMENT '状态: available, occupied, maintenance'," +
                    "`x_coord` int(11) DEFAULT '0' COMMENT '平面图 X 坐标'," +
                    "`y_coord` int(11) DEFAULT '0' COMMENT '平面图 Y 坐标'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_seat_no` (`seat_no`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表'");

            // 4. 创建预约记录表
            log.info("Checking/Creating table: sys_reservation");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_reservation` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`user_id` bigint(20) NOT NULL COMMENT '关联用户ID'," +
                    "`seat_id` bigint(20) NOT NULL COMMENT '关联座位ID'," +
                    "`start_time` datetime NOT NULL COMMENT '预约开始时间'," +
                    "`end_time` datetime NOT NULL COMMENT '预约结束时间'," +
                    "`deadline` datetime DEFAULT NULL COMMENT '签到/暂离截止时间'," +
                    "`status` varchar(20) DEFAULT 'reserved' COMMENT 'reserved, checked_in, completed, cancelled, violation'," +
                    "`type` varchar(20) DEFAULT 'appointment' COMMENT '类型: appointment, immediate'," +
                    "`reservation_date` date DEFAULT NULL COMMENT '预约日期'," +
                    "`slot` varchar(20) DEFAULT NULL COMMENT '时段: morning, afternoon, evening'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_user_id` (`user_id`)," +
                    "KEY `idx_seat_id` (`seat_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表'");

            // 兼容性字段检查
            String[] resAlters = {
                "ALTER TABLE `sys_reservation` ADD COLUMN `deadline` datetime DEFAULT NULL COMMENT '签到/暂离截止时间'",
                "ALTER TABLE `sys_reservation` ADD COLUMN `type` varchar(20) DEFAULT 'appointment' COMMENT '类型: appointment, immediate'",
                "ALTER TABLE `sys_reservation` ADD COLUMN `slot` varchar(20) DEFAULT NULL COMMENT '时段: morning, afternoon, evening'",
                "ALTER TABLE `sys_reservation` ADD COLUMN `reservation_date` date DEFAULT NULL COMMENT '预约日期'"
            };
            for (String sql : resAlters) {
                try { stmt.execute(sql); } catch (SQLException e) { }
            }

            // 5. 创建违规申诉表
            log.info("Checking/Creating table: sys_appeal");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_appeal` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`reservation_id` bigint(20) NOT NULL COMMENT '关联预约记录ID'," +
                    "`reason` text NOT NULL COMMENT '申诉理由'," +
                    "`images` text DEFAULT NULL COMMENT '图片凭证 (JSON数组或逗号分隔)'," +
                    "`status` varchar(20) DEFAULT 'pending' COMMENT 'pending (待审核), approved (通过), rejected (驳回)'," +
                    "`reply` text DEFAULT NULL COMMENT '管理员回复'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_reservation_id` (`reservation_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规申诉表'");
            
            // 6. 创建系统日志表
            log.info("Checking/Creating table: sys_log");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_log` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`username` varchar(50) DEFAULT NULL COMMENT '操作人账号'," +
                    "`operation` varchar(100) DEFAULT NULL COMMENT '操作类型'," +
                    "`content` text DEFAULT NULL COMMENT '操作内容'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表'");

            // 7. 创建消息广场表
            log.info("Checking/Creating table: sys_message");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_message` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`user_id` bigint(20) NOT NULL COMMENT '发布人ID'," +
                    "`content` text NOT NULL COMMENT '消息内容'," +
                    "`at_user_id` bigint(20) DEFAULT NULL COMMENT '被@人ID'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_user_id` (`user_id`)," +
                    "KEY `idx_at_user_id` (`at_user_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息广场表'");

            // 8. 创建系统通知表
            log.info("Checking/Creating table: sys_notification");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_notification` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`user_id` bigint(20) NOT NULL COMMENT '接收人ID'," +
                    "`title` varchar(100) NOT NULL COMMENT '通知标题'," +
                    "`content` text NOT NULL COMMENT '通知内容'," +
                    "`type` varchar(20) DEFAULT 'info' COMMENT '类型: info, success, warning, error'," +
                    "`is_read` int(11) DEFAULT '0' COMMENT '是否已读 0:否 1:是'," +
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_user_id` (`user_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表'");

            // 9. 创建系统配置表
            log.info("Checking/Creating table: sys_config");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_config` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`config_key` varchar(50) NOT NULL UNIQUE COMMENT '配置键'," +
                    "`config_value` varchar(255) NOT NULL COMMENT '配置值'," +
                    "`config_name` varchar(100) DEFAULT NULL COMMENT '配置名称'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表'");

            // 初始化默认配置
            stmt.execute("REPLACE INTO `sys_config` (id, config_key, config_value, config_name) VALUES " +
                    "(1, 'violation_time', '30', '预约签到截止时间(分钟)'), " +
                    "(2, 'min_credit_score', '60', '预约所需最低信用分'), " +
                    "(3, 'message_square_enabled', 'true', '消息广场是否允许发言')");

            // 10. 创建系统菜单表
            log.info("Checking/Creating table: sys_menu");
            stmt.execute("CREATE TABLE IF NOT EXISTS `sys_menu` (" +
                    "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "`parent_id` bigint(20) DEFAULT '0' COMMENT '父菜单ID'," +
                    "`name` varchar(50) DEFAULT NULL COMMENT '菜单名称'," +
                    "`path` varchar(100) DEFAULT NULL COMMENT '路由路径'," +
                    "`title` varchar(50) DEFAULT NULL COMMENT '菜单标题'," +
                    "`icon` varchar(50) DEFAULT NULL COMMENT '图标'," +
                    "`roles` varchar(255) DEFAULT NULL COMMENT '权限角色JSON'," +
                    "`sort_order` int(11) DEFAULT '0' COMMENT '排序'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表'");

            // 初始化默认菜单 (REPLACE INTO 以确保更新，解决乱码覆盖问题)
            String[] initialMenus = {
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (1, 0, 'dashboard', '/dashboard', '首页', 'DashboardOutlined', '[\"student\",\"admin\",\"librarian\"]', 1)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (2, 0, 'seat', '/seat', '座位预约', 'DesktopOutlined', '[\"student\",\"admin\",\"librarian\"]', 2)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (3, 0, 'checkin', '/checkin', '签到', 'EnvironmentOutlined', '[\"student\",\"admin\",\"librarian\"]', 3)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (4, 0, 'square', '/square', '消息广场', 'CommentOutlined', '[\"student\",\"admin\",\"librarian\"]', 4)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (5, 0, 'stats', '/stats', '数据统计', 'BarChartOutlined', '[\"admin\",\"librarian\"]', 5)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (6, 0, 'system', '/system', '系统管理', 'SettingOutlined', '[\"admin\",\"librarian\"]', 6)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (7, 6, 'SystemUser', '/system/user', '用户管理', 'UserOutlined', '[\"admin\"]', 1)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (8, 6, 'SystemSeat', '/system/seat', '座位管理', 'ProjectOutlined', '[\"admin\",\"librarian\"]', 2)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (9, 6, 'SystemLog', '/system/log', '系统日志', 'FileTextOutlined', '[\"admin\"]', 3)",
                "REPLACE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `title`, `icon`, `roles`, `sort_order`) VALUES (10, 6, 'SystemConfig', '/system/config', '系统配置', 'SettingOutlined', '[\"admin\"]', 4)"
            };
            for (String sql : initialMenus) {
                try { stmt.execute(sql); } catch (SQLException e) { log.warn("Failed to insert menu: " + e.getMessage()); }
            }

            log.info("Database initialization completed successfully.");

        } catch (SQLException e) {
            log.error("Database initialization failed", e);
        }
    }
}
