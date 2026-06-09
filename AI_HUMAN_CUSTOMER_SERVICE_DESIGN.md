# Toolbox AI+人工客服系统设计方案

**文档版本**: v1.0  
**编写日期**: 2024-01-01  
**设计阶段**: 架构设计 & 数据库设计（第一阶段）

---

## 目录

1. [系统概述](#1-系统概述)
2. [数据库设计](#2-数据库设计)
3. [WebSocket架构设计](#3-websocket架构设计)
4. [前端页面结构设计](#4-前端页面结构设计)
5. [AI转人工流程设计](#5-ai转人工流程设计)
6. [文件清单](#6-文件清单)
7. [下一步计划](#7-下一步计划)

---

## 1. 系统概述

### 1.1 设计目标

- 实现AI客服与人工客服的无缝切换
- 保持现有业务功能不受影响
- 提供流畅的用户体验
- 支持会话记录和数据分析

### 1.2 系统角色

| 角色 | 说明 | 入口 |
|------|------|------|
| **普通用户** | 通过微信小程序发起咨询 | toolbox-miniprogram |
| **AI客服** | Ollama本地AI自动回复 | 后端服务 |
| **人工客服** | 后台管理人员 | toolbox-vue |
| **系统管理员** | 客服系统管理 | toolbox-vue |

### 1.3 核心流程

```
用户发起咨询 
    ↓
AI客服接待（自动回复）
    ↓
判断是否需要转人工
    ├─ 否 → AI继续回答
    └─ 是 → 等待人工客服接入
            ↓
        人工客服接待
            ↓
        会话结束，满意度评价
            ↓
        生成工单（如需要）
```

---

## 2. 数据库设计

### 2.1 数据库ER图

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│  cs_agent    │       │  cs_session  │       │  cs_message  │
│  客服账号表   │       │  客服会话表   │       │  客服消息表   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │◄──────│ agent_id(FK) │       │ id (PK)      │
│ username     │       │ id (PK)      │◄──────│ session_id   │
│ password     │       │ session_no   │       │ session_no   │
│ nickname     │       │ user_id      │       │ msg_id       │
│ avatar       │       │ user_nickname│       │ msg_type     │
│ role         │       │ agent_nickname      │ content      │
│ status       │       │ status       │       │ sender_type  │
│ max_sessions │       │ start_time   │       │ sender_id    │
│ current_sessions     │ end_time     │       │ is_read      │
│ total_served │       │ duration     │       │ create_time  │
│ rating       │       │ rating       │       └──────────────┘
└──────────────┘       └──────────────┘               
                              │
                              │
                              ▼
                     ┌──────────────┐       ┌──────────────┐
                     │ cs_work_order │       │cs_quick_reply│
                     │  客服工单表   │       │ 快捷回复表    │
                     ├──────────────┤       ├──────────────┤
                     │ id (PK)      │       │ id (PK)      │
                     │ order_no     │       │ category     │
                     │ session_id   │       │ content      │
                     │ category     │       │ sort         │
                     │ title        │       │ use_count    │
                     │ description  │       │ status       │
                     │ status       │       └──────────────┘
                     │ rating       │
                     │ create_time  │
                     └──────────────┘

┌──────────────┐
│ cs_sensitive_word│
│  敏感词表      │
├──────────────┤
│ id (PK)      │
│ word         │
│ replace_word │
│ level        │
│ create_time  │
└──────────────┘

┌──────────────────┐
│  cs_statistics   │
│  客服统计表       │
├──────────────────┤
│ id (PK)         │
│ stat_date       │
│ total_sessions  │
│ ai_sessions     │
│ human_sessions  │
│ transfer_count  │
│ avg_response_time│
│ user_satisfaction│
│ create_time     │
└──────────────────┘
```

### 2.2 数据表SQL设计

#### 表1：cs_agent（客服账号表）

```sql
CREATE TABLE `cs_agent` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客服ID',
  `openid` VARCHAR(64) DEFAULT NULL COMMENT '关联用户openid',
  `username` VARCHAR(50) NOT NULL COMMENT '客服账号',
  `password` VARCHAR(128) NOT NULL COMMENT '登录密码(MD5加密)',
  `nickname` VARCHAR(50) NOT NULL COMMENT '客服昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '客服头像',
  `role` TINYINT DEFAULT 1 COMMENT '角色: 1-客服 2-管理员',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-离线 1-在线 2-忙碌 3-休息',
  `max_sessions` INT DEFAULT 50 COMMENT '最大接待会话数',
  `current_sessions` INT DEFAULT 0 COMMENT '当前接待会话数',
  `total_served` INT DEFAULT 0 COMMENT '累计服务用户数',
  `rating` DECIMAL(3,2) DEFAULT 5.00 COMMENT '平均评分(1-5)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_openid` (`openid`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服账号表';
```

**说明**：
- `username`：客服登录账号，唯一
- `password`：MD5加密存储
- `role`：区分客服和管理员权限
- `status`：客服在线状态，影响会话分配
- `max_sessions`：限制单个客服同时接待数量

#### 表2：cs_session（客服会话表）

```sql
CREATE TABLE `cs_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_no` VARCHAR(32) NOT NULL COMMENT '会话编号(UUID)',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID(openid)',
  `user_nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` VARCHAR(255) DEFAULT NULL COMMENT '用户头像',
  `agent_id` BIGINT DEFAULT NULL COMMENT '接待客服ID',
  `agent_nickname` VARCHAR(50) DEFAULT NULL COMMENT '客服昵称',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-AI接待中 1-等待转人工 2-人工接待中 3-已结束',
  `ai_unsolved_count` INT DEFAULT 0 COMMENT 'AI连续未解决次数',
  `ai_transfer_keywords` VARCHAR(255) DEFAULT NULL COMMENT '触发转人工的关键词',
  `last_message` VARCHAR(500) DEFAULT NULL COMMENT '最后一条消息摘要',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `unread_count` INT DEFAULT 0 COMMENT '用户未读消息数',
  `agent_unread_count` INT DEFAULT 0 COMMENT '客服未读消息数',
  `rating` TINYINT DEFAULT NULL COMMENT '用户评分(1-5)',
  `rating_time` DATETIME DEFAULT NULL COMMENT '评分时间',
  `transfer_reason` VARCHAR(255) DEFAULT NULL COMMENT '转人工原因',
  `transfer_time` DATETIME DEFAULT NULL COMMENT '转人工时间',
  `start_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '会话结束时间',
  `duration` INT DEFAULT 0 COMMENT '会话时长(秒)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话表';
```

**说明**：
- `session_no`：唯一标识，用于WebSocket连接
- `status`：会话状态流转的核心字段
- `ai_unsolved_count`：AI连续未解决计数，达到阈值触发转人工
- `unread_count`/`agent_unread_count`：未读消息计数，用于消息提醒

#### 表3：cs_message（客服消息表）

```sql
CREATE TABLE `cs_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `session_no` VARCHAR(32) NOT NULL COMMENT '会话编号',
  `msg_id` VARCHAR(32) NOT NULL COMMENT '消息ID(客户端生成)',
  `msg_type` TINYINT NOT NULL COMMENT '消息类型: 1-文本 2-图片 3-语音 4-视频 5-文件 6-卡片',
  `content` TEXT COMMENT '消息内容',
  `media_url` VARCHAR(500) DEFAULT NULL COMMENT '媒体文件URL',
  `media_thumb` VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
  `sender_type` TINYINT NOT NULL COMMENT '发送者类型: 1-用户 2-AI 3-客服',
  `sender_id` VARCHAR(64) NOT NULL COMMENT '发送者ID',
  `sender_nickname` VARCHAR(50) DEFAULT NULL COMMENT '发送者昵称',
  `sender_avatar` VARCHAR(255) DEFAULT NULL COMMENT '发送者头像',
  `is_ai_answer` TINYINT DEFAULT 0 COMMENT '是否AI回复: 0-否 1-是',
  `ai_confidence` DECIMAL(5,4) DEFAULT NULL COMMENT 'AI置信度(0-1)',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`msg_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_session_no` (`session_no`),
  KEY `idx_sender_type` (`sender_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服消息表';
```

**说明**：
- `msg_id`：客户端生成的唯一消息ID，防止重复提交
- `sender_type`：区分消息来源，便于前端渲染
- `is_ai_answer`：标记AI回复，用于训练数据收集
- `ai_confidence`：AI回复置信度，用于评估AI能力

#### 表4：cs_work_order（客服工单表）

```sql
CREATE TABLE `cs_work_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '工单编号',
  `session_id` BIGINT NOT NULL COMMENT '关联会话ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `agent_id` BIGINT DEFAULT NULL COMMENT '处理客服ID',
  `category` TINYINT NOT NULL COMMENT '工单类型: 1-咨询 2-建议 3-投诉 4-功能反馈 5-其他',
  `title` VARCHAR(100) NOT NULL COMMENT '工单标题',
  `description` TEXT COMMENT '工单描述',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待处理 1-处理中 2-已解决 3-已关闭',
  `priority` TINYINT DEFAULT 1 COMMENT '优先级: 1-低 2-中 3-高 4-紧急',
  `rating` TINYINT DEFAULT NULL COMMENT '满意度评分(1-5)',
  `feedback` TEXT COMMENT '用户反馈',
  `handle_time` INT DEFAULT 0 COMMENT '处理时长(分钟)',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handler_name` VARCHAR(50) DEFAULT NULL COMMENT '处理人名称',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_agent_id` (`agent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_category` (`category`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单表';
```

#### 表5：cs_quick_reply（快捷回复表）

```sql
CREATE TABLE `cs_agent` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `category` VARCHAR(50) DEFAULT 'common' COMMENT '分类: greeting-问候 common-通用 closing-结束语',
  `content` TEXT NOT NULL COMMENT '回复内容',
  `sort` INT DEFAULT 0 COMMENT '排序(越小越靠前)',
  `use_count` INT DEFAULT 0 COMMENT '使用次数',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快捷回复表';
```

#### 表6：cs_sensitive_word（敏感词表）

```sql
CREATE TABLE `cs_sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `word` VARCHAR(50) NOT NULL COMMENT '敏感词',
  `replace_word` VARCHAR(50) DEFAULT '***' COMMENT '替换词',
  `level` TINYINT DEFAULT 1 COMMENT '级别: 1-替换 2-警告 3-拦截',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';
```

#### 表7：cs_statistics（客服统计表）

```sql
CREATE TABLE `cs_statistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `total_sessions` INT DEFAULT 0 COMMENT '总会话数',
  `ai_sessions` INT DEFAULT 0 COMMENT 'AI接待会话数',
  `human_sessions` INT DEFAULT 0 COMMENT '人工接待会话数',
  `transfer_count` INT DEFAULT 0 COMMENT '转人工次数',
  `avg_response_time` INT DEFAULT 0 COMMENT '平均首次响应时间(秒)',
  `avg_handle_time` INT DEFAULT 0 COMMENT '平均处理时长(秒)',
  `user_satisfaction` DECIMAL(5,2) DEFAULT 0 COMMENT '用户满意度(%)',
  `total_messages` INT DEFAULT 0 COMMENT '总消息数',
  `total_users` INT DEFAULT 0 COMMENT '服务用户数',
  `new_users` INT DEFAULT 0 COMMENT '新用户数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服统计表';
```

### 2.3 索引设计

| 表名 | 索引名称 | 索引字段 | 类型 | 说明 |
|------|----------|----------|------|------|
| cs_agent | uk_username | username | UNIQUE | 唯一登录账号 |
| cs_agent | idx_openid | openid | INDEX | 关联用户 |
| cs_agent | idx_status | status | INDEX | 在线状态查询 |
| cs_session | uk_session_no | session_no | UNIQUE | 唯一会话编号 |
| cs_session | idx_user_id | user_id | INDEX | 用户会话查询 |
| cs_session | idx_agent_id | agent_id | INDEX | 客服会话查询 |
| cs_session | idx_status | status | INDEX | 状态筛选 |
| cs_session | idx_last_message_time | last_message_time | INDEX | 最新消息排序 |
| cs_message | uk_msg_id | msg_id | UNIQUE | 唯一消息ID |
| cs_message | idx_session_id | session_id | INDEX | 会话消息查询 |
| cs_message | idx_create_time | create_time | INDEX | 时间排序 |
| cs_work_order | uk_order_no | order_no | UNIQUE | 唯一工单编号 |
| cs_work_order | idx_status | status | INDEX | 工单状态查询 |
| cs_quick_reply | idx_category | category | INDEX | 分类查询 |

---

## 3. WebSocket架构设计

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          客户端层                                        │
├───────────────────────────┬───────────────────────────┬───────────────────┤
│   微信小程序客户端        │    Vue客服工作台          │   其他客户端       │
│   (toolbox-miniprogram)  │   (toolbox-vue)          │                   │
│   ws://api/customer/ws   │   ws://api/customer/ws    │                   │
└───────────┬───────────────┴───────────┬───────────────┴───────────────────┘
            │                           │
            └───────────────────────────┼───────────────────────────────┘
                                        │ WebSocket
┌───────────────────────────────────────▼───────────────────────────────┐
│                        WebSocket网关层                                  │
│                   (CustomerWebSocketConfig)                            │
│                                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │  连接管理       │  │  消息路由       │  │  心跳检测       │        │
│  │  ConnectionMgr │  │  MessageRouter │  │  HeartBeat     │        │
│  └────────────────┘  └────────────────┘  └────────────────┘        │
└───────────────────────────────────────┬───────────────────────────────┘
                                        │
            ┌───────────────────────────┼───────────────────────────────┐
            │                           │                               │
            ▼                           ▼                               ▼
┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐
│   AI客服处理器         │   │   人工客服处理器       │   │   系统处理器          │
│  AiCustomerHandler    │   │  HumanHandler         │   │  SystemHandler        │
│                       │   │                       │   │                       │
│  • Ollama调用         │   │  • 会话分配           │   │  • 心跳响应           │
│  • 意图识别           │   │  • 消息转发           │   │  • 连接维护           │
│  • 转人工判断         │   │  • 快捷回复           │   │  • 错误处理           │
└───────────────────────┘   └───────────────────────┘   └───────────────────────┘
            │                           │
            └───────────────────────────┼───────────────────────────────┘
                                        │
┌───────────────────────────────────────▼───────────────────────────────┐
│                        业务服务层                                       │
│                                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │ ChatService    │  │ SessionService │  │ MessageService │        │
│  │ AI聊天服务     │  │ 会话服务       │  │ 消息服务       │        │
│  └────────────────┘  └────────────────┘  └────────────────┘        │
│                                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │ AgentService   │  │ WorkOrderService│ │ StatisticsService│      │
│  │ 客服管理服务   │  │ 工单服务       │  │ 统计服务       │        │
│  └────────────────┘  └────────────────┘  └────────────────┘        │
└───────────────────────────────────────┬───────────────────────────────┘
                                        │
┌───────────────────────────────────────▼───────────────────────────────┐
│                        数据访问层                                       │
│                   (Spring JDBC Template)                              │
│                                                                      │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │ SessionDao     │  │ MessageDao    │  │ AgentDao       │        │
│  └────────────────┘  └────────────────┘  └────────────────┘        │
└───────────────────────────────────────────────────────────────────────┘
```

### 3.2 WebSocket消息协议

#### 消息格式

```json
{
  "msgId": "msg_123456789",      // 消息唯一ID
  "type": "text",                // 消息类型: text/image/audio/file/card
  "sessionNo": "sess_abc123",    // 会话编号
  "content": "用户消息内容",       // 消息内容
  "mediaUrl": "",               // 媒体文件URL（可选）
  "timestamp": 1704067200000,   // 时间戳
  "sender": {
    "id": "user_openid_123",    // 发送者ID
    "type": "user",             // 发送者类型: user/ai/agent
    "nickname": "用户昵称",      // 发送者昵称
    "avatar": ""                // 发送者头像
  }
}
```

#### 消息类型定义

| type | 说明 | 适用场景 |
|------|------|----------|
| `text` | 文本消息 | 普通对话 |
| `image` | 图片消息 | 用户发送图片 |
| `audio` | 语音消息 | 语音消息 |
| `file` | 文件消息 | 发送文件 |
| `card` | 卡片消息 | AI回复卡片（如图文教程） |
| `system` | 系统消息 | 接入提示、转接提示等 |

#### 控制消息

```json
{
  "msgId": "ctrl_001",
  "type": "control",
  "action": "transfer_to_human",   // 控制动作
  "sessionNo": "sess_abc123",
  "params": {
    "reason": "用户请求人工",
    "keywords": ["退款", "投诉"]
  }
}
```

| action | 说明 |
|--------|------|
| `start_session` | 开始会话 |
| `transfer_to_human` | 转人工请求 |
| `end_session` | 结束会话 |
| `typing` | 对方正在输入 |
| `read` | 消息已读 |
| `ping/pong` | 心跳检测 |

### 3.3 连接管理

```
用户A ──────► WebSocket连接建立
                  │
                  ▼
            验证Token
                  │
                  ▼
            创建会话上下文
            (SessionContext)
                  │
                  ▼
            加入连接池
            (ConnectionPool)
                  │
                  ▼
            心跳保活
            (HeartBeat)
                  │
                  ▼
            WebSocket断开
                  │
                  ▼
            保存会话状态
            清理连接资源
```

### 3.4 会话分配策略

```
用户发起会话请求
        │
        ▼
  检查是否有空闲客服
        │
    ┌───┴───┐
    │       │
   是      否
    │       │
    ▼       ▼
  分配给   进入排队队列
  空闲客服   (Queue)
    │       │
    │       ▼
    │    等待客服
    │    释放资源
    │       │
    └──►◄──┘
        │
        ▼
  更新会话状态
  (分配agent_id)
```

---

## 4. 前端页面结构设计

### 4.1 Vue客服后台（toolbox-vue）

#### 页面目录结构

```
toolbox-vue/src/views/customer/
├── Dashboard.vue              # 客服工作台首页
├── SessionList.vue           # 会话列表页
├── SessionDetail.vue         # 会话详情页
├── WorkOrder.vue            # 工单管理页
├── QuickReply.vue           # 快捷回复管理
├── AgentManage.vue          # 客服账号管理
├── Statistics.vue           # 数据统计页
└── Settings.vue             # 系统设置页
```

#### 页面功能设计

##### 4.1.1 Dashboard.vue（客服工作台首页）

**功能**：
- 显示今日会话统计
- 显示当前在线客服状态
- 显示等待中的会话
- 显示紧急工单

**布局**：
```
┌────────────────────────────────────────────────┐
│  今日概览                                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────┐ │
│  │总会话数  │ │AI接待   │ │人工接待  │ │转人工│ │
│  │  128    │ │  95     │ │  33     │ │  12  │ │
│  └──────────┘ └──────────┘ └──────────┘ └─────┘ │
├────────────────────────────────────────────────┤
│  客服状态                                       │
│  ┌─────────────────────────────────────────┐  │
│  │ 客服A ●在线  3/50会话    评分:4.8       │  │
│  │ 客服B ●忙碌  50/50会话   评分:4.9       │  │
│  │ 客服C ○离线  0/50会话    评分:4.7       │  │
│  └─────────────────────────────────────────┘  │
├────────────────────────────────────────────────┤
│  待处理会话                                     │
│  ┌─────────────────────────────────────────┐  │
│  │ [等待中] 用户001 - "退款问题" - 5分钟    │  │
│  │ [等待中] 用户002 - "功能咨询" - 2分钟    │  │
│  │ [排队中] 用户003 - "投诉建议" - 1分钟    │  │
│  └─────────────────────────────────────────┘  │
└────────────────────────────────────────────────┘
```

##### 4.1.2 SessionList.vue（会话列表）

**功能**：
- 列表展示所有会话
- 支持按状态筛选（AI接待/人工接待/已结束）
- 支持按时间范围筛选
- 支持关键词搜索

**表格列**：
| 列名 | 说明 |
|------|------|
| 会话编号 | session_no |
| 用户 | 昵称 + 头像 |
| 接待方 | AI/客服昵称 |
| 状态 | 当前会话状态 |
| 最后消息 | 消息摘要 |
| 最后活跃 | 相对时间 |
| 操作 | 查看详情 |

##### 4.1.3 SessionDetail.vue（会话详情）

**功能**：
- 实时聊天窗口
- 历史消息展示
- 用户信息展示
- 快捷回复选择
- 转接/结束会话按钮

**布局**：
```
┌──────────────────────────────────────────────────────┐
│ 会话信息                          操作按钮             │
│ 用户: 张三 │ 状态: 人工接待中  │ [快捷回复] [转接] [结束]│
├────────────────────────────────────┬─────────────────┤
│                                    │ 用户信息         │
│  09:30  AI: 您好，请问有什么帮助？  │ 昵称: 张三      │
│                                    │ 会员: VIP       │
│  09:31  用户: 我想退款              │ 注册时间: xxx   │
│                                    │                 │
│  09:32  AI: 抱歉，我无法处理退款... │ 历史会话:       │
│                                    │ • 2024-01-01   │
│  09:33  用户: 转人工                │ • 2023-12-15   │
│                                    │                 │
│  09:33  系统: 已为您转接人工客服      │                 │
│                                    │                 │
│  09:34  客服小王: 您好，我来帮您处理 │                 │
│                                    │                 │
│  ┌────────────────────────────────┴─────────────────┐│
│  │ 输入消息...                    │ [发送] [图片]    ││
│  └───────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────┘
```

##### 4.1.4 WorkOrder.vue（工单管理）

**功能**：
- 工单列表展示
- 工单状态流转
- 工单详情查看
- 工单分配和处理

##### 4.1.5 QuickReply.vue（快捷回复管理）

**功能**：
- 分类管理快捷回复
- 添加/编辑/删除回复
- 排序管理
- 使用统计

##### 4.1.6 AgentManage.vue（客服管理）

**功能**：
- 客服账号列表
- 添加/编辑客服
- 设置客服状态
- 查看客服统计数据

##### 4.1.7 Statistics.vue（数据统计）

**功能**：
- 今日/本周/本月数据
- 会话趋势图
- 响应时间统计
- 满意度评分
- AI转人工统计

### 4.2 微信小程序客服页面（toolbox-miniprogram）

#### 页面目录结构

```
toolbox-miniprogram/pages/customer/
├── index/                    # 客服首页
│   ├── index.js
│   ├── index.wxml
│   ├── index.wxss
│   └── index.json
├── chat/                     # 聊天页面
│   ├── index.js
│   ├── index.wxml
│   ├── index.wxss
│   └── index.json
└── service/                 # 客服设置页
    ├── index.js
    ├── index.wxml
    ├── index.wxss
    └── index.json
```

#### 页面功能设计

##### 4.2.1 index/（客服首页）

**功能**：
- 常见问题FAQ列表
- 人工客服入口
- 智能助手入口
- 服务协议入口

**布局**：
```
┌────────────────────────────────┐
│  客服中心                       │
├────────────────────────────────┤
│  ┌──────────────────────────┐  │
│  │      🤖 智能助手          │  │
│  │   24小时在线，即问即答    │  │
│  │   [立即咨询]              │  │
│  └──────────────────────────┘  │
├────────────────────────────────┤
│  常见问题                       │
│  ├─ 如何开通VIP会员？         │
│  ├─ PDF合并失败怎么办？       │
│  ├─ 图片压缩有次数限制吗？    │
│  └─ 更多问题...               │
├────────────────────────────────┤
│  ┌──────────────────────────┐  │
│  │    👨‍💻 转人工客服          │  │
│  │   工作时间: 9:00-21:00  │  │
│  │   [立即转人工]           │  │
│  └──────────────────────────┘  │
├────────────────────────────────┤
│  [我的会话记录]                │
└────────────────────────────────┘
```

##### 4.2.2 chat/（聊天页面）

**功能**：
- 实时聊天窗口
- 消息类型支持（文字/图片）
- AI回复展示
- 转人工按钮
- 会话评价

**布局**：
```
┌────────────────────────────────┐
│  ◀  客服助手         [···]      │
├────────────────────────────────┤
│                                │
│  ┌────────────────────────┐    │
│  │ 🤖 您好！我是智能助手    │    │
│  │ 请告诉我您的问题...      │    │
│  └────────────────────────┘    │
│                                │
│         ┌────────────────┐     │
│         │ 我想退款怎么办？ │     │
│         └────────────────┘     │
│                                │
│  ┌────────────────────────┐    │
│  │ 很抱歉，这类问题需要    │    │
│  │ 转人工客服为您处理      │    │
│  └────────────────────────┘    │
│                                │
│  ┌────────────────────────┐    │
│  │ [转人工客服]            │    │
│  └────────────────────────┘    │
│                                │
├────────────────────────────────┤
│  ┌────────────────────┐ [发送] │
│  │ 输入消息...         │        │
│  └────────────────────┘        │
│  [😊] [📷]                     │
└────────────────────────────────┘
```

**交互流程**：
1. 用户进入聊天页面
2. 显示欢迎消息和常见问题引导
3. AI自动接待并回复
4. 用户可随时点击"转人工客服"
5. 转人工后，AI会话保留到人工会话
6. 会话结束后弹出评价窗口

##### 4.2.3 service/（客服设置页）

**功能**：
- 客服协议
- 隐私政策
- 历史会话记录
- 清除会话记录

---

## 5. AI转人工流程设计

### 5.1 触发条件

AI客服在以下情况自动判断是否需要转人工：

| 触发条件 | 说明 | 优先级 |
|----------|------|--------|
| **关键词匹配** | 包含"退款"、"投诉"、"人工"等关键词 | 高 |
| **连续未解决** | AI连续3次回答后用户表示不满意 | 高 |
| **情绪识别** | 检测到用户情绪负面 | 中 |
| **超时无响应** | 用户发送消息后5分钟无有效响应 | 中 |
| **手动触发** | 用户主动点击"转人工"按钮 | 最高 |

### 5.2 转人工流程图

```
用户发送消息
      │
      ▼
┌─────────────────┐
│  AI接收消息     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  敏感词检测     │
└────────┬────────┘
         │
    ┌────┴────┐
    │检测到？  │
    └────┬────┘
     是   │ 否
     ▼    │
替换敏感词 │
     │    │
     ▼    ▼
┌─────────────────┐
│  关键词匹配     │
└────────┬────────┘
         │
    ┌────┴────┐
    │匹配成功？│
    └────┬────┘
     是   │ 否
     ▼    │
转人工建议 │   ┌────────────────┐
     │    │   │  Ollama生成回复 │
     │    └──►│  (带系统提示词) │
     │        └────────────────┘
     │              │
     ▼              ▼
┌────────────────────────┐
│  显示AI回复           │
│  + "是否转人工？"     │
└───────────┬──────────┘
            │
       ┌───┴───┐
       │用户选择│
    ┌───┴───┐  │
   是│      │否 │
    ▼      │   │
继续AI服务 │   ▼
    │      │ 继续AI服务
    │      │   │
    └──────┴───┘
            │
            ▼
┌────────────────────────┐
│  用户点击"转人工"      │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│  更新会话状态          │
│  status = 1           │
│  (等待转人工)          │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│  查询空闲客服          │
│  status = 1           │
│  current < max        │
└───────────┬────────────┘
            │
       ┌────┴────┐
       │有空闲客服│
       └────┬────┘
        是   │ 否
        ▼    │
分配客服    ▼
        进入排队队列
            │
            ▼
┌────────────────────────┐
│  更新会话状态          │
│  status = 2           │
│  (人工接待中)          │
│  agent_id = xxx       │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│  发送系统消息          │
│  "已为您转接人工客服"  │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐
│  WebSocket通知         │
│  客服工作台            │
└───────────┬────────────┘
            │
            ▼
人工客服开始接待
```

### 5.3 转人工关键词库

```sql
-- 初始关键词数据
INSERT INTO cs_sensitive_word (word, level) VALUES
-- 转人工关键词
('转人工', 1),
('人工客服', 1),
('我要投诉', 1),
('退款', 2),
('还钱', 2),
('骗子', 3),
-- 情绪负面
('垃圾', 3),
('太差了', 2),
('不满意', 2),
('没用', 2);
```

### 5.4 AI提示词设计

```text
【系统提示词 - AI客服】

你是Toolbox平台的智能客服助手。你需要：
1. 友好、专业的回答用户问题
2. 优先使用平台内置知识回答
3. 如果遇到以下关键词，触发转人工建议：
   - 退款、投诉、举报、还钱
   - 涉及账户安全、资金问题
   - 多次表示不满或负面情绪
   
4. 转人工回复模板：
   "很抱歉，这类问题需要转人工客服为您处理。请点击下方的[转人工客服]按钮，我们会尽快为您安排专业客服。"

5. 如果用户回复"是"、"好的"、"转人工"等，确认转人工。
```

---

## 6. 文件清单

### 6.1 需修改的现有文件

#### 后端（toolbox-java）

| 文件路径 | 修改内容 |
|----------|----------|
| `src/main/java/com/toolbox/ToolboxApplication.java` | 添加@ComponentScan |
| `src/main/java/com/toolbox/common/GlobalExceptionHandler.java` | 添加WebSocket异常处理 |
| `src/main/resources/application.yml` | 添加客服相关配置 |
| `src/main/resources/customer_service.sql` | 优化数据表设计 |

#### Vue前端（toolbox-vue）

| 文件路径 | 修改内容 |
|----------|----------|
| `src/router/index.ts` | 添加客服路由 |
| `src/utils/api.ts` | 添加客服API接口 |
| `src/stores/user.ts` | 添加客服状态管理 |

#### 微信小程序（toolbox-miniprogram）

| 文件路径 | 修改内容 |
|----------|----------|
| `pages/index/index.wxml` | 添加客服入口 |
| `utils/api.js` | 添加客服API |
| `app.json` | 添加客服页面路由 |

### 6.2 新增文件清单

#### 后端新增（toolbox-java）

```
src/main/java/com/toolbox/
├── config/
│   └── CustomerWebSocketConfig.java    # WebSocket配置
├── controller/
│   └── CustomerServiceController.java  # 客服控制器
├── service/
│   └── customer/
│       ├── CustomerSessionService.java # 会话服务
│       ├── CustomerMessageService.java # 消息服务
│       ├── AiCustomerService.java      # AI客服服务
│       └── TransferService.java        # 转人工服务
├── handler/
│   └── CustomerWebSocketHandler.java  # WebSocket处理器
├── dao/
│   ├── CustomerSessionDao.java        # 会话DAO
│   ├── CustomerMessageDao.java        # 消息DAO
│   └── CustomerAgentDao.java          # 客服DAO
└── dto/
    ├── WebSocketMessage.java           # 消息DTO
    └── CustomerSessionDto.java        # 会话DTO
```

#### Vue前端新增（toolbox-vue）

```
src/views/customer/
├── Dashboard.vue                      # 客服工作台首页
├── SessionList.vue                   # 会话列表
├── SessionDetail.vue                 # 会话详情
├── WorkOrder.vue                     # 工单管理
├── QuickReply.vue                   # 快捷回复
├── AgentManage.vue                   # 客服管理
├── Statistics.vue                   # 数据统计
└── Settings.vue                     # 系统设置

src/components/customer/
├── ChatWindow.vue                    # 聊天窗口组件
├── MessageList.vue                  # 消息列表组件
├── QuickReplyPanel.vue             # 快捷回复面板
└── AgentStatus.vue                 # 客服状态组件

src/utils/
└── websocket.js                      # WebSocket工具类
```

#### 微信小程序新增（toolbox-miniprogram）

```
pages/customer/
├── index/
│   ├── index.js                     # 客服首页
│   ├── index.wxml                   # 客服首页模板
│   ├── index.wxss                   # 客服首页样式
│   └── index.json                  # 页面配置
├── chat/
│   ├── index.js                     # 聊天页面
│   ├── index.wxml                   # 聊天模板
│   ├── index.wxss                   # 聊天样式
│   └── index.json                  # 页面配置
└── service/
    ├── index.js                     # 客服设置
    ├── index.wxml                   # 设置模板
    ├── index.wxss                   # 设置样式
    └── index.json                  # 页面配置

utils/
└── websocket.js                      # WebSocket工具类
```

#### 数据库脚本

| 文件路径 | 说明 |
|----------|------|
| `src/main/resources/customer_service.sql` | 客服系统数据库脚本（优化版） |

### 6.3 文件统计

| 模块 | 新增文件 | 修改文件 |
|------|----------|----------|
| 后端Java | 12个 | 4个 |
| Vue前端 | 11个 | 3个 |
| 微信小程序 | 12个 | 3个 |
| 数据库脚本 | 1个 | 0个 |
| **合计** | **36个** | **10个** |

---

## 7. 下一步计划

### 7.1 确认事项

- [ ] 数据库设计是否满足需求？
- [ ] WebSocket消息协议是否完整？
- [ ] 前端页面结构是否合理？
- [ ] AI转人工流程是否完善？

### 7.2 开发阶段划分

#### 第二阶段：后端开发
1. 创建数据库表
2. 实现DAO层
3. 实现Service层
4. 配置WebSocket
5. 实现消息处理逻辑

#### 第三阶段：Vue客服后台
1. 搭建页面框架
2. 实现会话列表
3. 实现会话详情
4. 实现工单管理
5. 实现数据统计

#### 第四阶段：微信小程序客服
1. 创建客服页面
2. 实现聊天功能
3. 实现AI转人工
4. 实现会话评价

### 7.3 测试计划

- [ ] 单元测试（Service层）
- [ ] 集成测试（WebSocket连接）
- [ ] 功能测试（完整流程）
- [ ] 性能测试（并发连接）
- [ ] 用户体验测试

---

**文档结束**

> ⚠️ **注意**：本设计文档仅包含架构和数据库设计，不包含代码实现。
> 请确认方案后，我将进入开发阶段。