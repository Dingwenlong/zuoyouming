USE library_seat;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_room
-- ----------------------------
DROP TABLE IF EXISTS `tb_room`;
CREATE TABLE `tb_room` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT 'room name',
  `floor` int(11) DEFAULT NULL COMMENT 'floor',
  `description` varchar(255) DEFAULT NULL COMMENT 'description',
  `deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='room table';

-- ----------------------------
-- Table structure for tb_seat
-- ----------------------------
DROP TABLE IF EXISTS `tb_seat`;
CREATE TABLE `tb_seat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `room_id` bigint(20) NOT NULL COMMENT 'room id',
  `seat_number` varchar(20) NOT NULL COMMENT 'seat number',
  `type` tinyint(4) DEFAULT '1' COMMENT 'type 1:normal 2:window 3:socket',
  `status` tinyint(4) DEFAULT '0' COMMENT 'status 0:idle 1:reserved 2:in use 3:temp leave 4:faulty',
  `x_coord` int(11) DEFAULT '0' COMMENT 'X coord',
  `y_coord` int(11) DEFAULT '0' COMMENT 'Y coord',
  `deleted` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_seat` (`room_id`,`seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='seat table';

-- ----------------------------
-- Records of tb_room
-- ----------------------------
INSERT INTO `tb_room` VALUES (1, 'Reading Room 1', 1, 'Quiet Area', 0);
INSERT INTO `tb_room` VALUES (2, 'Reading Room 2', 2, 'Discussion Area', 0);

-- ----------------------------
-- Records of tb_seat
-- ----------------------------
-- Room 1 Seats (Grid 3x3 example)
INSERT INTO `tb_seat` (room_id, seat_number, type, status, x_coord, y_coord) VALUES 
(1, 'A01', 2, 0, 1, 1), (1, 'A02', 1, 0, 1, 2), (1, 'A03', 1, 2, 1, 3),
(1, 'B01', 3, 0, 2, 1), (1, 'B02', 1, 1, 2, 2), (1, 'B03', 1, 0, 2, 3);

SET FOREIGN_KEY_CHECKS = 1;
