-- ========================================
-- ToolBox 简化版数据库初始化脚本
-- 版本: v2.0 (精简版)
-- 日期: 2024-01-01
-- ========================================

CREATE DATABASE IF NOT EXISTS toolbox DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE toolbox;

-- ========================================
-- 工具配置表
-- ========================================

CREATE TABLE IF NOT EXISTS tool_category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(200),
    description VARCHAR(200),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tool_config (
    id INT PRIMARY KEY AUTO_INCREMENT,
    tool_key VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(200),
    description VARCHAR(500),
    category_id INT,
    page_path VARCHAR(200),
    web_path VARCHAR(200),
    api_path VARCHAR(200),
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    is_vip TINYINT DEFAULT 0,
    daily_limit INT DEFAULT 0,
    config TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================
-- PDF记录表 (保留)
-- ========================================

CREATE TABLE IF NOT EXISTS pdf_merge_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    task_no VARCHAR(64),
    source_files TEXT,
    output_file VARCHAR(500),
    file_count INT DEFAULT 0,
    page_count INT DEFAULT 0,
    file_size BIGINT DEFAULT 0,
    status TINYINT DEFAULT 0,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_task_no (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pdf_split_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    task_no VARCHAR(64),
    source_file VARCHAR(500),
    source_file_name VARCHAR(255),
    output_files TEXT,
    page_range VARCHAR(100),
    total_pages INT DEFAULT 0,
    split_count INT DEFAULT 0,
    status TINYINT DEFAULT 0,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_task_no (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS image_to_pdf_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    task_no VARCHAR(64),
    source_files TEXT,
    output_file VARCHAR(500),
    image_count INT DEFAULT 0,
    page_count INT DEFAULT 0,
    file_size BIGINT DEFAULT 0,
    sort_order TEXT,
    status TINYINT DEFAULT 0,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_task_no (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================
-- 图片处理表 (保留)
-- ========================================

CREATE TABLE IF NOT EXISTS image_compress_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    task_no VARCHAR(64),
    source_file VARCHAR(500),
    source_file_name VARCHAR(255),
    output_file VARCHAR(500),
    source_size BIGINT DEFAULT 0,
    output_size BIGINT DEFAULT 0,
    compression_rate DECIMAL(5,2),
    quality INT DEFAULT 80,
    width INT,
    height INT,
    status TINYINT DEFAULT 0,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_task_no (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS id_photo_edit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    task_no VARCHAR(64),
    source_file VARCHAR(500),
    output_file VARCHAR(500),
    source_bg_color VARCHAR(20),
    target_bg_color VARCHAR(20),
    target_bg_name VARCHAR(50),
    status TINYINT DEFAULT 0,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_task_no (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================
-- 简历表 (保留)
-- ========================================

CREATE TABLE IF NOT EXISTS resume_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    resume_no VARCHAR(64) UNIQUE NOT NULL,
    template_id BIGINT,
    title VARCHAR(200),
    name VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    gender VARCHAR(10),
    birthday DATE,
    avatar VARCHAR(500),
    current_address VARCHAR(200),
    job_intention VARCHAR(200),
    salary_expectation VARCHAR(50),
    entry_time VARCHAR(50),
    self_evaluation TEXT,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_resume_no (resume_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume_education (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    school_name VARCHAR(200) NOT NULL,
    major VARCHAR(200),
    degree VARCHAR(50),
    start_date DATE,
    end_date DATE,
    description TEXT,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume_work (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    position VARCHAR(100),
    start_date DATE,
    end_date DATE,
    is_current TINYINT DEFAULT 0,
    description TEXT,
    achievements TEXT,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    role VARCHAR(100),
    start_date DATE,
    end_date DATE,
    description TEXT,
    achievements TEXT,
    tech_stack TEXT,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    skill_type VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    level VARCHAR(50),
    issue_date DATE,
    description TEXT,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    thumbnail VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    is_vip TINYINT DEFAULT 0,
    template_path VARCHAR(500),
    style_config TEXT,
    use_count INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resume_generate_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    resume_id BIGINT,
    task_no VARCHAR(64),
    template_id BIGINT,
    resume_data TEXT,
    output_file VARCHAR(500),
    output_format VARCHAR(10) DEFAULT 'pdf',
    ai_optimized TINYINT DEFAULT 0,
    ai_feedback TEXT,
    file_size BIGINT DEFAULT 0,
    status TINYINT DEFAULT 0,
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_resume_id (resume_id),
    INDEX idx_task_no (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========================================
-- 初始化数据
-- ========================================

-- 工具分类 (精简版)
INSERT INTO tool_category (name, icon, description, sort_order, status) VALUES
('PDF工具', 'pdf', 'PDF文件处理相关工具', 1, 1),
('图片工具', 'image', '图片处理相关工具', 2, 1),
('简历工具', 'resume', '简历制作相关工具', 3, 1);

-- 工具配置 (精简版 - 只保留需要的功能)
INSERT INTO tool_config (tool_key, name, icon, description, category_id, page_path, api_path, sort_order, status, is_vip, daily_limit) VALUES
-- PDF模块
('pdf_merge', 'PDF合并', 'merge', '将多个PDF文件合并为一个', 1, '/pages/pdf/merge/index', '/api/pdf-merge/merge', 1, 1, 0, 0),
('pdf_split', 'PDF拆分', 'split', '将PDF文件按页码拆分为多个', 1, '/pages/pdf/split/index', '/api/pdf-split/split', 2, 1, 0, 0),
('pdf_image', '图片转PDF', 'image2pdf', '将多张图片合成一个PDF', 1, '/pages/pdf/image2pdf/index', '/api/pdf-batch/convert', 3, 1, 0, 0),
-- 图片模块
('image_compress', '图片压缩', 'compress', '压缩图片大小，保持清晰度', 2, '/pages/image/compress/index', '/api/image-compress/compress', 1, 1, 0, 0),
('image_background', '证件照换底', 'background', 'AI智能更换证件照背景颜色', 2, '/pages/image/background/index', '/api/simple-image/background', 2, 1, 0, 0),
('image_convert', '图片格式转换', 'convert', '支持多种图片格式互转', 2, '/pages/image/convert/index', '/api/image-convert/convert', 3, 1, 0, 0),
-- 简历模块
('resume_create', '简历制作', 'resume', '在线制作精美简历', 3, '/pages/resume/create/index', '/api/resume/create', 1, 1, 0, 0),
('resume_list', '我的简历', 'list', '查看和管理我的简历', 3, '/pages/resume/list/index', '/api/resume/list', 2, 1, 0, 0);

-- 简历模板
INSERT INTO resume_template (name, thumbnail, category, is_vip, sort_order, status) VALUES
('经典商务', '/templates/classic.jpg', 'developer', 0, 1, 1),
('简约清新', '/templates/simple.jpg', 'fresh_graduate', 0, 2, 1),
('专业正式', '/templates/professional.jpg', 'product_manager', 0, 3, 1),
('创意设计', '/templates/creative.jpg', 'designer', 1, 4, 1),
('行政模板', '/templates/admin.jpg', 'admin', 0, 5, 1);

-- ========================================
-- 初始化完成
-- ========================================

SELECT '数据库初始化完成 (精简版 v2.0)' AS message;
SELECT '保留功能: PDF合并, PDF拆分, 图片转PDF, 图片压缩, 证件照换底, 图片格式转换, 去水印, 简历制作' AS features;