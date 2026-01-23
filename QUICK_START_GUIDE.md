# 🚀 快速启动指南

## 📋 开始前准备

### 环境要求

- **Docker** >= 20.10
- **Docker Compose** >= 2.0
- **OpenAI API Key**（或兼容服务的 API Key）

### 系统要求

- **CPU**: 2 核+
- **内存**: 4GB+
- **硬盘**: 20GB+
- **操作系统**: Linux / macOS / Windows (WSL2)

---

## ⚡ 3 步快速启动

### 步骤 1：配置 API Key

编辑项目根目录的 `.env` 文件：

```bash
vim .env
```

**最少修改 1 项**（必须）：

```bash
LLM_API_KEY=sk-your-real-openai-api-key-here
```

**生产环境建议修改 3 项**：

```bash
LLM_API_KEY=sk-your-real-api-key-here
ADMIN_PASSWORD=YourStr0ng!P@ssw0rd
JWT_SECRET=$(openssl rand -hex 32)
```

### 步骤 2：启动服务

```bash
# 一键启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

预期输出：
```
NAME                  STATUS          PORTS
lingdang-mysql        Up              0.0.0.0:3306->3306/tcp
lingdang-elasticsearch Up            0.0.0.0:9200->9200/tcp
lingdang-backend      Up              0.0.0.0:8080->8080/tcp
lingdang-frontend     Up              0.0.0.0:80->80/tcp
```

### 步骤 3：访问系统

- **前端首页**: http://localhost
- **管理后台**: http://localhost/studio/login
  - 用户名：`admin`
  - 密码：`.env` 中配置的 `ADMIN_PASSWORD`（默认 `admin123456`）
- **AI 助手**: http://localhost/assistant

---

## 📝 完整使用流程

### 1. 登录 Studio

访问 http://localhost/studio/login

- 用户名：`admin`
- 密码：`admin123456`（或你配置的密码）

### 2. 创建第一篇文章

1. 点击「新建文章」
2. 填写信息：
   - **标题**：如何理解 Transformer 架构
   - **Slug**：understanding-transformer
   - **摘要**：深入解析 Transformer 的自注意力机制
   - **标签**：深度学习,NLP,Transformer
   - **Markdown 内容**：
     ```markdown
     # Transformer 架构
     
     ## 自注意力机制
     
     Transformer 的核心是自注意力机制...
     
     ## 位置编码
     
     由于没有循环结构，需要位置编码...
     ```

3. 点击「保存草稿」

### 3. 发布文章

1. 在文章列表中找到刚创建的文章
2. 点击「发布」
3. 系统会自动：
   - 切分文章为 chunks
   - 生成 embeddings
   - 写入 Elasticsearch
   - 更新索引版本

### 4. 使用 AI 助手

1. 访问 http://localhost/assistant
2. 输入问题：「什么是 Transformer 的自注意力机制？」
3. 点击「提问」
4. 系统会：
   - 检索相关文章片段
   - 调用 LLM 生成回答
   - 返回带引用的答案
   - 显示参考文章链接

### 5. 查看文章

1. 访问 http://localhost/blog
2. 浏览已发布的文章列表
3. 点击文章查看详情
4. 浏览次数自动统计

---

## 🔍 验证安装

### 健康检查

```bash
curl http://localhost:8080/api/health
```

预期返回：
```json
{
  "status": "UP",
  "service": "lingdang-blog-backend",
  "database": "UP",
  "elasticsearch": "CONFIGURED"
}
```

### 测试登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123456"}'
```

预期返回：
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "expiresIn": 86400000
  }
}
```

### 测试 Assistant

```bash
curl -X POST http://localhost:8080/api/assistant/query \
  -H "Content-Type: application/json" \
  -d '{"question":"测试问题","mode":"ARTICLE_ONLY"}'
```

---

## 📊 查看日志

### 查看所有服务日志

```bash
docker-compose logs -f
```

### 查看特定服务日志

```bash
# 后端
docker-compose logs -f backend

# 前端
docker-compose logs -f frontend

# MySQL
docker-compose logs -f mysql

# Elasticsearch
docker-compose logs -f elasticsearch
```

---

## 🛑 停止服务

### 停止但保留数据

```bash
docker-compose down
```

### 停止并删除所有数据

```bash
docker-compose down -v
```

---

## 🔧 常见问题

### Q1: 启动失败，提示端口被占用

**解决**：修改 `docker-compose.yml` 中的端口映射

```yaml
services:
  frontend:
    ports:
      - "8000:80"  # 改为 8000
```

### Q2: Elasticsearch 启动失败

**解决**：增加虚拟内存限制

```bash
# Linux
sudo sysctl -w vm.max_map_count=262144

# macOS (Docker Desktop)
# 在 Docker Desktop 设置中增加内存分配到 4GB+
```

### Q3: 索引任务失败

**原因**：LLM API Key 无效或网络问题

**解决**：
1. 检查 API Key：`docker-compose exec backend env | grep LLM_API_KEY`
2. 测试连接：`curl https://api.openai.com/v1/models -H "Authorization: Bearer $LLM_API_KEY"`
3. 查看详细错误：`docker-compose logs backend | grep -i error`

### Q4: 前端白屏

**解决**：
1. 检查前端是否启动：`docker-compose ps frontend`
2. 检查后端 API：`curl http://localhost:8080/api/health`
3. 查看浏览器控制台错误

---

## 📱 移动端访问

### 局域网访问

1. 查找服务器 IP：
   ```bash
   # Linux/macOS
   ifconfig | grep inet
   
   # Windows
   ipconfig
   ```

2. 在移动端浏览器访问：
   ```
   http://192.168.x.x
   ```

### 注意事项

- 确保移动设备与服务器在同一局域网
- 防火墙需开放 80 端口
- 生产环境建议配置 HTTPS

---

## 🌐 生产部署

### 使用云服务器

1. **购买云服务器**（阿里云/腾讯云/AWS）
   - 配置：2核4G，40GB 硬盘
   - 系统：Ubuntu 20.04+

2. **安装 Docker**
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER
   ```

3. **上传项目**
   ```bash
   scp -r demo/ user@server-ip:/home/user/
   ```

4. **配置环境变量**
   ```bash
   cd /home/user/demo
   vim .env  # 修改生产配置
   ```

5. **启动服务**
   ```bash
   docker-compose up -d
   ```

6. **配置域名**（可选）
   - 将域名解析到服务器 IP
   - 配置 Nginx SSL（参考 `deploy/nginx-ssl.conf`）

---

## 📚 相关文档

- **完整配置说明**: `CONFIGURATION_CHECKLIST.md`
- **项目总结**: `PROJECT_SUMMARY.md`
- **部署指南**: `DEPLOYMENT_GUIDE.md`
- **前端说明**: `frontend/FRONTEND_README.md`

---

## 💡 小贴士

1. **首次启动较慢**
   - MySQL 和 Elasticsearch 需要初始化
   - 等待约 1-2 分钟后再访问

2. **开发环境配置**
   - 可以使用默认密码
   - LLM_API_KEY 是唯一必须配置的

3. **生产环境配置**
   - 必须修改所有密码和密钥
   - 建议配置 HTTPS
   - 定期备份数据库

4. **性能优化**
   - 根据流量调整限流参数
   - 监控 ES 内存使用
   - 定期清理日志

---

## 🎉 开始使用

现在你可以：

1. ✅ 登录 Studio 创建文章
2. ✅ 发布文章并自动索引
3. ✅ 使用 AI 助手提问
4. ✅ 查看文章和统计数据

**祝你使用愉快！** 🔔

---

© 2026 铃铛师兄大模型 - 快速启动指南
