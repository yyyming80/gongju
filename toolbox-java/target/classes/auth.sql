-- ========================================
-- 微信小程序用户表
-- ========================================

-- 创建用户表
CREATE TABLE IF NOT EXISTS `wx_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信openid',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '微信unionid',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
  `country` VARCHAR(50) DEFAULT NULL COMMENT '国家',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  `is_vip` TINYINT DEFAULT 0 COMMENT '是否VIP: 0-否, 1-是',
  `vip_expire_time` DATETIME DEFAULT NULL COMMENT 'VIP过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_unionid` (`unionid`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信用户表';

-- ========================================
-- 登录会话表（可选，用于存储会话密钥）
-- ========================================

CREATE TABLE IF NOT EXISTS `wx_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信openid',
  `session_key` VARCHAR(64) DEFAULT NULL COMMENT '微信session_key',
  `session_token` VARCHAR(128) DEFAULT NULL COMMENT '自定义会话token',
  `token_expire_time` DATETIME DEFAULT NULL COMMENT 'token过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_token` (`session_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信会话表';

-- ========================================
-- 测试数据（可选）
-- ========================================

-- INSERT INTO `wx_user` (`openid`, `nickname`, `avatar`, `status`) 
-- VALUES ('test_openid_123', '测试用户', 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRnaF5pXM找工作找找工作/0', 1);
