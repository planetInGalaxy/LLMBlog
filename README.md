# 🔔 铃铛师兄大模型博客系统

一个基于 Spring Boot + React 的现代化个人博客网站，专注于AI和大模型技术分享。

## 📋 项目简介

铃铛师兄大模型博客系统是一个全栈Web应用，提供了完整的博客文章管理功能，包括文章发布、浏览、搜索等。系统采用前后端分离架构，支持本地开发和Docker容器化部署。

### 核心特性

- 🎨 现代化响应式UI设计
- 📝 博客文章增删改查
- 🔍 文章搜索功能
- 👁 文章浏览次数统计
- 🏷 文章标签管理
- 🐳 Docker一键部署
- ☁️ 支持云服务器部署（阿里云/腾讯云）

## 🛠 技术栈

### 后端
- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- MySQL 8.0（生产环境）
- H2 Database（开发环境）
- Maven 3.8+

### 前端
- React 19.2
- Vite 7.2
- 原生 CSS（响应式设计）

### 部署
- Docker & Docker Compose
- Nginx（前端服务器）

## 🚀 快速开始

### 前置要求

确保你的系统已安装以下工具：
- JDK 17 或更高版本
- Node.js 18+ 
- Maven 3.8+
- Docker & Docker Compose（可选，用于容器化部署）

### 本地开发

#### 1. 克隆项目

```bash
git clone <your-repo-url>
cd demo
```

#### 2. 启动后端

```bash
cd backend
# 使用 Maven 构建并运行
mvn clean install
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

#### 3. 启动前端

```bash
cd frontend
# 安装依赖
npm install
# 启动开发服务器
npm run dev
```

前端服务将在 http://localhost:3000 启动

#### 4. 访问应用

打开浏览器访问 http://localhost:3000，即可看到铃铛师兄大模型博客首页。

## 🐳 Docker 部署

### 一键启动（推荐）

使用 Docker Compose 一键启动所有服务：

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

服务启动后：
- 前端：http://localhost
- 后端API：http://localhost:8080
- MySQL：localhost:3306

### 停止服务

```bash
docker-compose down

# 同时删除数据卷
docker-compose down -v
```

## ☁️ 云服务器部署

### 阿里云/腾讯云部署步骤

1. **购买云服务器**
   - 推荐配置：2核4G，40G硬盘
   - 操作系统：Ubuntu 20.04 或 CentOS 7+

2. **安装 Docker**

```bash
# Ubuntu
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

3. **上传项目文件**

```bash
# 使用 scp 或 git clone 上传项目
scp -r demo/ user@your-server-ip:/home/user/
```

4. **启动服务**

```bash
cd /home/user/demo
docker-compose up -d
```

5. **配置防火墙**

开放以下端口：
- 80 (HTTP)
- 443 (HTTPS，可选)
- 8080 (后端API，可选)

6. **配置域名（可选）**

将域名解析到服务器IP，并配置Nginx反向代理。

## 📁 项目结构

```
demo/
├── backend/                 # 后端服务
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/lingdang/blog/
│   │       │       ├── BlogApplication.java      # 主应用
│   │       │       ├── model/                    # 实体类
│   │       │       ├── repository/               # 数据访问层
│   │       │       ├── service/                  # 业务逻辑层
│   │       │       ├── controller/               # 控制器
│   │       │       └── config/                   # 配置类
│   │       └── resources/
│   │           ├── application.yml               # 开发配置
│   │           └── application-prod.yml          # 生产配置
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # 前端应用
│   ├── src/
│   │   ├── App.jsx         # 主组件
│   │   ├── App.css         # 样式
│   │   ├── main.jsx        # 入口文件
│   │   └── index.css       # 全局样式
│   ├── public/
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile
│   └── nginx.conf          # Nginx配置
├── docker-compose.yml       # Docker编排文件
├── .gitignore
└── README.md
```

## 🔧 环境变量配置

### 后端环境变量

开发环境使用 H2 内存数据库，生产环境使用 MySQL。

修改 `backend/src/main/resources/application-prod.yml` 配置生产环境数据库：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lingdang_blog
    username: root
    password: your_password
```

### 前端环境变量

前端会自动使用代理连接后端API，无需额外配置。

## 📚 API 文档

### 文章管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/posts | 获取所有已发布文章 |
| GET | /api/posts/{id} | 获取指定文章详情 |
| POST | /api/posts | 创建新文章 |
| PUT | /api/posts/{id} | 更新文章 |
| DELETE | /api/posts/{id} | 删除文章 |
| GET | /api/posts/search?keyword={keyword} | 搜索文章 |

### 文章实体结构

```json
{
  "id": 1,
  "title": "文章标题",
  "content": "文章内容",
  "author": "作者",
  "summary": "摘要",
  "tags": "标签1,标签2",
  "viewCount": 100,
  "published": true,
  "createdAt": "2026-01-22T10:00:00",
  "updatedAt": "2026-01-22T10:00:00"
}
```

## 🔍 SEO 优化

网站已针对搜索引擎进行优化：

- ✅ 语义化 HTML 标签
- ✅ Meta 标签优化（description, keywords）
- ✅ 页面标题包含"铃铛师兄大模型"关键词
- ✅ 响应式设计，移动端友好
- ✅ 页脚包含关键词强化

建议进一步优化：
- 提交sitemap到搜索引擎
- 使用百度站长工具/Google Search Console
- 定期发布高质量原创内容

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

---

**铃铛师兄大模型** - 专注AI技术分享 🔔
