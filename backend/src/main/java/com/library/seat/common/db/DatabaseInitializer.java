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
            
            // 1.1 尝试更新 sys_user 表结构 (兼容旧表)
            try {
                // Rename nickname -> real_name
                stmt.execute("ALTER TABLE `sys_user` CHANGE COLUMN `nickname` `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名'");
            } catch (SQLException e) {
                // Ignore if column doesn't exist
            }
            try {
                // Add role if not exists
                stmt.execute("ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `role` varchar(20) DEFAULT 'student' COMMENT '角色: student, librarian, admin'");
            } catch (SQLException e) { }
             try {
                // Modify status type int -> varchar
                stmt.execute("ALTER TABLE `sys_user` MODIFY COLUMN `status` varchar(20) DEFAULT 'active' COMMENT '状态: active (正常), banned (封禁)'");
            } catch (SQLException e) { }
            
            try {
                // Add open_id if not exists
                stmt.execute("ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `open_id` varchar(64) DEFAULT NULL COMMENT '微信OpenID'");
                stmt.execute("ALTER TABLE `sys_user` ADD UNIQUE KEY `uk_open_id` (`open_id`)");
            } catch (SQLException e) { 
                // Ignore key already exists error
            }

            // 2. 插入或更新默认管理员账号
            // 密码: 123456
            log.info("Checking/Upserting default admin user");
            String adminPassword = "$2a$10$/DicGv2VwwNohvyU2O29k.9fsie0WBUfmaw6oPUYB4lZnSsY/Rb4q";
            
            // 检查 role 字段是否存在，如果不存在则不包含在 insert 语句中（虽然上面尝试添加了，但可能因为某些原因失败或未生效）
            // 为了安全起见，这里可以先执行一次显式的 ALTER 确保字段存在
            try {
                stmt.execute("ALTER TABLE `sys_user` ADD COLUMN IF NOT EXISTS `role` varchar(20) DEFAULT 'student' COMMENT '角色: student, librarian, admin'");
            } catch (SQLException e) {
                // Ignore
            }

            stmt.execute("INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) " +
                    "VALUES ('admin', '" + adminPassword + "', '管理员', 'admin', 'active') " +
                    "ON DUPLICATE KEY UPDATE `password` = '" + adminPassword + "', `role` = 'admin'");

            stmt.execute("INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) " +
                    "VALUES ('lib', '" + adminPassword + "', '图书馆员', 'librarian', 'active') " +
                    "ON DUPLICATE KEY UPDATE `password` = '" + adminPassword + "', `role` = 'librarian'");

            stmt.execute("INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) " +
                    "VALUES ('student', '" + adminPassword + "', '学生', 'student', 'active') " +
                    "ON DUPLICATE KEY UPDATE `password` = '" + adminPassword + "', `role` = 'student'");

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
                    "`create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "`deleted` int(11) DEFAULT '0' COMMENT '是否删除 0:否 1:是'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_user_id` (`user_id`)," +
                    "KEY `idx_seat_id` (`seat_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表'");

            // 4.1 尝试添加新字段 (兼容旧表结构)
            try {
                stmt.execute("ALTER TABLE `sys_reservation` ADD COLUMN `deadline` datetime DEFAULT NULL COMMENT '签到/暂离截止时间'");
            } catch (SQLException e) {
                // Ignore if column exists
            }
            try {
                stmt.execute("ALTER TABLE `sys_reservation` ADD COLUMN `type` varchar(20) DEFAULT 'appointment' COMMENT '类型: appointment, immediate'");
            } catch (SQLException e) {
                // Ignore if column exists
            }
            try {
                stmt.execute("ALTER TABLE `sys_reservation` ADD COLUMN `slot` varchar(20) DEFAULT NULL COMMENT '时段: morning, afternoon, evening'");
            } catch (SQLException e) {
                // Ignore if column exists
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

            log.info("Database initialization completed successfully.");

        } catch (SQLException e) {
            log.error("Database initialization failed", e);
            // 记录错误但不抛出异常，以免影响应用启动（视需求而定）
            // 如果数据库连接失败，Spring Boot 的 DataSource 初始化本身就会失败，所以这里主要是捕获 SQL 执行错误
        }
    }
}
