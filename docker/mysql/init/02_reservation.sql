USE library_seat;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_reservation
-- ----------------------------
DROP TABLE IF EXISTS `tb_reservation`;
CREATE TABLE `tb_reservation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'user id',
  `seat_id` bigint(20) NOT NULL COMMENT 'seat id',
  `start_time` datetime NOT NULL COMMENT 'start time',
  `end_time` datetime NOT NULL COMMENT 'end time',
  `status` tinyint(4) DEFAULT '0' COMMENT 'status 0:pending 1:checked-in 2:completed 3:cancelled 4:violated',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_seat_time` (`seat_id`,`start_time`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='reservation table';

SET FOREIGN_KEY_CHECKS = 1;
