-- ========================================
-- AI+人工客服系统 数据库设计 (精简版)
-- 版本: v1.0 MVP
-- 日期: 2024-01-01
-- 说明: 仅保留核心4张表
-- ========================================

USE toolbox;

-- ========================================
-- 1. 客服账号表
-- ========================================
DROP TABLE IF EXISTS `cs_agent`;
CREATE TABLE `cs_agent` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客服ID',
  `username` VARCHAR(50) NOT NULL COMMENT '客服账号',
  `password` VARCHAR(128) NOT NULL COMMENT '登录密码(MD5)',
  `nickname` VARCHAR(50) NOT NULL COMMENT '客服昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '客服头像',
  `role` TINYINT DEFAULT 1 COMMENT '角色: 1-客服 2-管理员',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-离线 1-在线 2-忙碌',
  `max_sessions` INT DEFAULT 50 COMMENT '最大接待会话数',
  `current_sessions` INT DEFAULT 0 COMMENT '当前接待会话数',
  `total_served` INT DEFAULT 0 COMMENT '累计服务用户数',
  `rating` DECIMAL(3,2) DEFAULT 5.00 COMMENT '平均评分',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服账号表';

-- ========================================
-- 2. 客服会话表
-- ========================================
DROP TABLE IF EXISTS `cs_session`;
CREATE TABLE `cs_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_no` VARCHAR(32) NOT NULL COMMENT '会话编号',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID(openid)',
  `user_nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像',
  `agent_id` BIGINT DEFAULT NULL COMMENT '接待客服ID',
  `agent_nickname` VARCHAR(50) DEFAULT NULL COMMENT '客服昵称',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-AI接待 1-等待转人工 2-人工接待 3-已结束',
  `ai_unsolved_count` INT DEFAULT 0 COMMENT 'AI连续未解决次数',
  `last_message` VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '用户未读消息数',
  `agent_unread_count` INT DEFAULT 0 COMMENT '客服未读消息数',
  `rating` TINYINT DEFAULT NULL COMMENT '用户评分(1-5)',
  `rating_time` DATETIME DEFAULT NULL,
  `transfer_reason` VARCHAR(255) DEFAULT NULL COMMENT '转人工原因',
  `transfer_time` DATETIME DEFAULT NULL COMMENT '转人工时间',
  `start_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '会话结束时间',
  `duration` INT DEFAULT 0 COMMENT '会话时长(秒)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话表';

-- ========================================
-- 3. 客服消息表
-- ========================================
DROP TABLE IF EXISTS `cs_message`;
CREATE TABLE `cs_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `session_no` VARCHAR(32) NOT NULL COMMENT '会话编号',
  `msg_id` VARCHAR(32) NOT NULL COMMENT '消息ID',
  `msg_type` TINYINT NOT NULL COMMENT '消息类型: 1-文本 2-图片 3-语音',
  `content` TEXT COMMENT '消息内容',
  `media_url` VARCHAR(500) DEFAULT NULL COMMENT '媒体文件URL',
  `sender_type` TINYINT NOT NULL COMMENT '发送者类型: 1-用户 2-AI 3-客服',
  `sender_id` VARCHAR(64) NOT NULL COMMENT '发送者ID',
  `sender_nickname` VARCHAR(50) DEFAULT NULL COMMENT '发送者昵称',
  `sender_avatar` VARCHAR(255) DEFAULT NULL COMMENT '发送者头像',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
  `read_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`msg_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_session_no` (`session_no`),
  KEY `idx_sender_type` (`sender_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服消息表';

-- ========================================
-- 4. 客服工单表
-- ========================================
DROP TABLE IF EXISTS `cs_work_order`;
CREATE TABLE `cs_work_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '工单编号',
  `session_id` BIGINT NOT NULL COMMENT '关联会话ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `agent_id` BIGINT DEFAULT NULL COMMENT '处理客服ID',
  `category` TINYINT NOT NULL COMMENT '工单类型: 1-咨询 2-建议 3-投诉 4-其他',
  `title` VARCHAR(100) NOT NULL COMMENT '工单标题',
  `description` TEXT COMMENT '工单描述',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理 1-处理中 2-已解决 3-已关闭',
  `priority` TINYINT DEFAULT 1 COMMENT '优先级: 1-低 2-中 3-高 4-紧急',
  `rating` TINYINT DEFAULT NULL COMMENT '满意度评分',
  `feedback` TEXT COMMENT '用户反馈',
  `handle_time` INT DEFAULT 0 COMMENT '处理时长(分钟)',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handler_name` VARCHAR(50) DEFAULT NULL COMMENT '处理人名称',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO `cs_agent` (`username`, `password`, `nickname`, `role`, `status`, `max_sessions`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 2, 1, 100);

-- 插入测试客服账号 (密码: 123456)
INSERT INTO `cs_agent` (`username`, `password`, `nickname`, `role`, `status`, `max_sessions`) VALUES
('cs001', 'e10adc3949ba59abbe56e057f20f883e', '客服小王', 1, 1, 50),
('cs002', 'e10adc3949ba59abbe56e057f20f883e', '客服小李', 1, 1, 50);

SELECT 'AI+人工客服系统数据库初始化完成 (MVP v1.0)' AS message;