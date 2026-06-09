-- ========================================
-- 修复客服登录密码 - 将MD5密码改为明文
-- 执行此SQL前请确保已备份数据库
-- ========================================

USE toolbox;

-- 更新管理员密码为明文
UPDATE cs_agent SET password = 'admin123' WHERE username = 'admin';

-- 更新客服账号密码为明文
UPDATE cs_agent SET password = '123456' WHERE username = 'cs001';
UPDATE cs_agent SET password = '123456' WHERE username = 'cs002';

-- 验证更新结果
SELECT id, username, nickname, role, password FROM cs_agent;