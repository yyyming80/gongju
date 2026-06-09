# 项目概述

## 项目名称

Toolbox - 在线工具平台

## 项目目标

为用户提供一站式在线工具服务，支持微信小程序和Web端访问，实现PDF处理、图片处理、AI聊天、会员管理等核心功能的便捷访问。

## 项目功能

- **PDF工具**：PDF合并、拆分、图片转PDF
- **图片工具**：图片压缩、格式转换、证件照换底色
- **AI聊天**：智能问答助手
- **会员系统**：VIP权限管理
- **客服系统**：AI+人工混合客服（WebSocket）

---

# 系统架构

```
┌──────────────────────────────────────────────────────────────┐
│                        客户端层                                │
├─────────────────┬──────────────────┬────────────────────────┤
│   微信小程序     │    Web前端       │    客服管理后台          │
│ toolbox-mini    │  toolbox-vue     │    toolbox-web          │
└────────┬────────┴────────┬─────────┴─────────┬───────────────┘
         │                 │                  │
         └─────────────────┼──────────────────┘
                           │ HTTP/WebSocket
         ┌─────────────────▼──────────────────┐
         │        Spring Boot 后端服务          │
         │         (toolbox-java : 8080)        │
         │                                      │
         │  ┌─────────┐  ┌─────────┐  ┌──────┐ │
         │  │Controller│  │ Service │  │  DAO │ │
         │  └─────────┘  └─────────┘  └──────┘ │
         └─────────────────┬──────────────────┘
                           │
         ┌─────────────────┼────────────────────┐
         │                 │                      │
         ▼                 ▼                      ▼
┌────────────────┐ ┌──────────────┐ ┌─────────────────┐
│   MySQL 数据库   │ │  文件存储     │ │   Ollama AI服务  │
│    (toolbox)    │ │  (本地磁盘)   │ │  (本地:11434)    │
└────────────────┘ └──────────────┘ └─────────────────┘
                           │
         ┌─────────────────┼────────────────────┐
         │                 │                      │
         ▼                 ▼                      ▼
┌────────────────┐ ┌──────────────┐ ┌─────────────────┐
│  图片去背服务    │ │    Redis     │ │    微信支付      │
│ toolbox-rembg   │ │   (可选)     │ │    API集成       │
│    (:5000)      │ │              │ │                  │
└────────────────┘ └──────────────┘ └─────────────────┘
```

---

# 技术栈

## 前端

| 技术 | 说明 |
|------|------|
| **微信小程序** | 原生开发，承载核心工具功能 |
| **Vue 3** | Web端管理后台框架 |
| **TypeScript** | 类型安全的开发语言 |
| **Element Plus** | UI组件库 |
| **Pinia** | 状态管理 |
| **Vite** | 构建工具 |

## 后端

| 技术 | 说明 |
|------|------|
| **Spring Boot 2.7.18** | 核心框架 |
| **Java 17** | 开发语言 |
| **Spring JDBC** | 数据库访问 |
| **Apache PDFBox 2.0.27** | PDF处理 |
| **Apache POI 5.2.5** | Word文档处理 |
| **Thumbnailator 0.4.20** | 图片压缩 |
| **JWT (jjwt 0.11.5)** | Token认证 |

## 数据库

| 技术 | 说明 |
|------|------|
| **MySQL 8.0.33** | 主数据库 |
| **Redis** | 缓存（可选） |

## 认证

| 技术 | 说明 |
|------|------|
| **微信登录** | 小程序openid认证 |
| **JWT** | Token会话管理 |

---

# 功能模块

## 用户登录模块

**实现方式：**
- 微信小程序通过wx.login获取code
- 后端通过微信API获取openid/session_key
- 生成JWT Token返回给客户端
- Token包含用户信息和过期时间

**涉及文件：**
```
toolbox-java/src/main/java/com/toolbox/
├── common/JwtUtil.java              # JWT工具类
├── controller/AuthController.java   # 认证控制器
└── service/AuthService.java         # 认证服务（逻辑层）
toolbox-java/src/main/resources/
├── auth.sql                        # 用户表初始化脚本
└── application.yml                 # 微信配置（appid/secret）
```

---

## AI聊天模块

**实现方式：**
- 调用Ollama本地AI服务（qwen2.5模型）
- Spring Boot WebSocket实现流式响应
- 支持上下文对话管理
- 预留DeepSeek/OpenAI API接口

**涉及文件：**
```
toolbox-java/src/main/java/com/toolbox/
├── service/ChatService.java         # 聊天服务
├── service/OllamaClient.java        # Ollama API客户端
└── controller/ChatController.java   # 聊天控制器
toolbox-java/src/main/resources/
└── application.yml                 # Ollama配置（localhost:11434）
```

---

## 图片处理模块

**实现方式：**
- **图片压缩**：使用Thumbnailator库，支持质量调节
- **格式转换**：支持PNG/JPEG/WebP等格式互转
- **证件照换底**：调用Rembg API实现AI智能去背

**涉及文件：**
```
toolbox-java/src/main/java/com/toolbox/
├── service/ImageService.java        # 图片处理服务
├── service/ImageConvertService.java # 格式转换服务
├── controller/ImageController.java   # 图片控制器
└── controller/ImageConvertController.java # 转换控制器
toolbox-rembg/
├── app.py                          # Rembg服务
└── requirements.txt               # Python依赖
```

---

## PDF工具模块

**实现方式：**
- 使用Apache PDFBox库
- 支持PDF合并（多文件合并为一个）
- 支持PDF拆分（按页码拆分为多个）
- 支持图片转PDF（多图合成PDF）

**涉及文件：**
```
toolbox-java/src/main/java/com/toolbox/
├── service/PdfService.java        # PDF基础服务
├── service/PdfMergeService.java   # PDF合并服务
├── service/PdfBatchService.java    # 批量转换服务
├── controller/PdfController.java   # PDF控制器
├── controller/PdfMergeController.java # 合并控制器
└── controller/PdfBatchController.java # 批量控制器
```

---

## 文件处理模块

**实现方式：**
- 文件上传：Spring Boot MultipartFile
- 文件存储：本地磁盘存储（可配置路径）
- 文件下载：HTTP静态资源访问
- 文件管理：上传记录、文件大小统计

**涉及文件：**
```
toolbox-java/src/main/java/com/toolbox/
├── service/FileService.java       # 文件服务
├── controller/FileController.java # 文件控制器
├── config/UploadProperties.java   # 上传配置
└── config/WebConfig.java          # 静态资源配置
toolbox-java/src/main/resources/
└── application.yml               # 上传路径配置
```

---

## 客服模块

**当前状态：**
- 已预留客服模块接口
- WebSocket通信架构已设计
- AI智能回复逻辑已实现

**预留接口：**
```java
// WebSocket连接
ws://localhost:8080/api/customer/connect

// 消息发送
POST /api/customer/send

// 转人工
POST /api/customer/transfer

// 结束会话
POST /api/customer/end
```

**后续方案：**
- 实现完整的WebSocket客服系统
- 支持AI+人工混合接待模式
- 工单流转和处理流程

**涉及文件：**
```
toolbox-java/src/main/java/com/toolbox/
├── service/customer/
│   ├── AiCustomerService.java    # AI客服服务
│   └── WebSocketHandler.java      # WebSocket处理器
└── controller/CustomerController.java # 客服控制器
toolbox-java/src/main/resources/
└── customer_service.sql           # 客服数据库脚本
```

---

# 数据库设计

## 用户相关表

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `wx_user` | 微信用户表 | openid, nickname, avatar, phone, is_vip, vip_expire_time |
| `wx_session` | 会话Token | openid, session_key, session_token, token_expire_time |

## 工具配置表

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `tool_category` | 工具分类 | name, icon, description, sort_order |
| `tool_config` | 工具配置 | tool_key, name, api_path, is_vip, daily_limit |

## PDF处理表

| 表名 | 说明 |
|------|------|
| `pdf_merge_record` | PDF合并记录 |
| `pdf_split_record` | PDF拆分记录 |
| `image_to_pdf_record` | 图片转PDF记录 |

## 图片处理表

| 表名 | 说明 |
|------|------|
| `image_compress_record` | 图片压缩记录 |
| `id_photo_edit_record` | 证件照换底记录 |

## 客服系统表

| 表名 | 说明 |
|------|------|
| `cs_agent` | 客服账号表 |
| `cs_session` | 客服会话表 |
| `cs_message` | 客服消息表 |
| `cs_work_order` | 客服工单表 |
| `cs_quick_reply` | 快捷回复表 |

---

# API接口

## 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 微信登录 |
| `/api/auth/refresh` | POST | 刷新Token |

## 文件处理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/pdf-merge/merge` | POST | PDF合并 |
| `/api/pdf-split/split` | POST | PDF拆分 |
| `/api/pdf-batch/convert` | POST | 图片转PDF |
| `/api/image-compress/compress` | POST | 图片压缩 |
| `/api/image-convert/convert` | POST | 图片格式转换 |
| `/api/simple-image/background` | POST | 证件照换底 |

## AI聊天接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/send` | POST | 发送消息 |
| `/api/chat/stream` | GET | 流式响应 |

## 客服接口（预留）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/customer/connect` | WebSocket | 建立连接 |
| `/api/customer/send` | WebSocket | 发送消息 |
| `/api/customer/end` | POST | 结束会话 |

---

# 项目优化记录

## 优化前

- 多数据库混合使用（MySQL + MongoDB）
- 复杂的微服务架构
- 大量的配置文件和启动脚本
- 文档分散在多个文件中
- 冗余的依赖和代码

## 优化后

- 统一使用MySQL数据库
- Spring Boot单体应用架构
- 精简配置文件
- 统一的数据库初始化脚本
- 集中化的技术文档
- 删除冗余依赖和代码

## 主要改进

1. **架构简化**：微服务 → 单体应用
2. **数据库统一**：移除MongoDB，统一MySQL
3. **代码精简**：移除未使用的功能
4. **文档整理**：删除冗余文档，统一技术报告
5. **部署优化**：提供Docker部署方案

---

# 部署指南

## 运行环境

| 环境 | 要求 |
|------|------|
| JDK | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Node.js | 18+ |
| 微信开发者工具 | 最新版本 |

## 第一步：启动数据库

**创建数据库：**
```sql
CREATE DATABASE toolbox DEFAULT CHARACTER SET utf8mb4;
```

**导入数据脚本：**
```bash
mysql -u root -p toolbox < src/main/resources/auth.sql
mysql -u root -p toolbox < src/main/resources/customer_service.sql
mysql -u root -p toolbox < src/main/resources/db-init.sql
```

**启动MySQL服务。**

## 第二步：启动后端服务

1. 打开项目：`toolbox-java`
2. 配置数据库连接：`src/main/resources/application.yml`
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/toolbox
       username: root
       password: your_password
   ```
3. 启动主类：`ToolboxApplication.java`
4. 验证启动成功：`http://localhost:8080`

## 第三步：启动Web后台

1. 打开项目：`toolbox-vue`
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
4. 访问：`http://localhost:5173`

## 第四步：启动微信小程序

1. 打开微信开发者工具
2. 选择项目：`toolbox-miniprogram`
3. 填写小程序 `AppID`
4. 点击"编译"运行

## 启动顺序

```
1. MySQL 数据库
2. toolbox-java 后端服务
3. toolbox-vue Web后台
4. toolbox-miniprogram 微信小程序
```

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |

> ⚠️ **注意**：首次使用请修改默认密码

## 常见问题

### 后端无法启动

**检查项：**
- ✅ JDK版本是否为17+
- ✅ 数据库连接配置是否正确
- ✅ `application.yml` 配置是否完整

### 小程序无法登录

**检查项：**
- ✅ AppID是否正确配置
- ✅ 微信登录配置是否完整
- ✅ 后端服务是否正常运行

### 后台无法访问

**检查项：**
- ✅ Node.js版本是否为18+
- ✅ `npm install` 是否已执行
- ✅ 后端接口是否正常响应

---

# 后续规划

## AI客服

- [ ] 接入更多LLM模型（Claude、GPT-4）
- [ ] 知识库向量检索
- [ ] 多轮对话上下文管理
- [ ] 情感分析和意图识别

## 人工客服

- [ ] 客服工作台开发
- [ ] 会话分配和管理
- [ ] 快捷回复语管理
- [ ] 敏感词过滤

## 工单系统

- [ ] 工单创建和流转
- [ ] 工单分配自动化
- [ ] SLA服务等级管理
- [ ] 工单数据分析

## 数据统计

- [ ] 用户行为分析
- [ ] 功能使用热度统计
- [ ] AI响应质量评估
- [ ] 运营数据看板
- [ ] 收益统计分析

---

**文档版本**: v1.0  
**最后更新**: 2024-01-01