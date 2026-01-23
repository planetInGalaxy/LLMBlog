# ✅ 部署清单（GitHub 推送一键部署）

## 📋 首次部署流程

### 🔧 本地准备

- [ ] 1. 确认所有代码改动已完成
- [ ] 2. 测试本地运行正常（可选）
- [ ] 3. 提交代码到 Git

```bash
git add .
git commit -m "feat: 集成 Ollama 本地 Embedding"
git push origin main
```

### 🚀 服务器部署

#### A. 拉取代码

```bash
# SSH 登录服务器
ssh user@your-server-ip

# 首次部署：克隆仓库
git clone https://github.com/your-username/demo.git
cd demo

# 后续部署：拉取更新
cd demo
git pull origin main
```

#### B. 首次部署（⭐ 仅第一次）

```bash
# 1. 启动所有服务
docker-compose up -d

# 2. 等待服务启动（约 30 秒）
docker-compose ps

# 3. 初始化 Ollama 模型（⭐ 重要！）
chmod +x init-ollama.sh
./init-ollama.sh

# 预计时间：1-5 分钟（下载模型）

# 4. 重启后端
docker-compose restart backend

# 5. 验证部署
curl http://localhost:8080/api/health
```

#### C. 后续部署（日常更新）

```bash
# 1. 拉取代码
git pull origin main

# 2. 仅重启后端（无需重启其他服务）
docker-compose restart backend

# 3. 查看日志
docker-compose logs -f backend
```

---

## 📝 关键说明

### ✅ 首次部署需要

1. **下载 Ollama 模型**（运行 `init-ollama.sh`）
   - 时间：1-5 分钟
   - 大小：270MB
   - **仅首次需要**

2. **启动所有服务**
   - MySQL
   - Elasticsearch
   - Ollama（新增）
   - Backend
   - Frontend

### 🔄 后续部署无需

1. ❌ 无需重新下载 Ollama 模型
2. ❌ 无需重启 MySQL/ES/Ollama
3. ✅ 仅需重启 backend（代码更新）

### 💾 数据持久化

所有数据保存在 Docker Volumes：
- `mysql-data` - MySQL 数据库
- `es-data` - Elasticsearch 索引
- `ollama-data` - Ollama 模型（⭐ 持久保存）

只要不执行 `docker-compose down -v`，数据永久保留。

---

## 🧪 验证清单

### 1. 服务状态检查

```bash
docker-compose ps
```

预期全部 `Up`：
```
lingdang-mysql          Up
lingdang-elasticsearch  Up
lingdang-ollama         Up  ← 新增
lingdang-backend        Up
lingdang-frontend       Up
```

### 2. Ollama 模型检查

```bash
docker exec lingdang-ollama ollama list
```

预期输出：
```
NAME                    SIZE
nomic-embed-text:latest 270MB
```

### 3. 健康检查

```bash
# 后端
curl http://localhost:8080/api/health

# Ollama
curl http://localhost:11434/api/version

# Elasticsearch
curl http://localhost:9200
```

### 4. 功能测试

- [ ] 登录 Studio: `http://your-ip/studio/login`
- [ ] 创建并发布一篇文章
- [ ] 观察后端日志（应看到 "使用 Ollama 生成 embeddings"）
- [ ] 访问 AI 助手: `http://your-ip/assistant`
- [ ] 提问测试

---

## 🔍 故障排查

### 问题 1：Ollama 服务未启动

```bash
# 查看日志
docker-compose logs ollama

# 手动启动
docker-compose up -d ollama
```

### 问题 2：模型未下载

```bash
# 检查模型
docker exec lingdang-ollama ollama list

# 手动下载
docker exec lingdang-ollama ollama pull nomic-embed-text
```

### 问题 3：Embedding 生成失败

```bash
# 查看后端日志
docker-compose logs backend | grep -i "ollama\|embedding\|error"

# 检查配置
docker-compose exec backend env | grep LLM
```

---

## 📊 配置概览

当前配置（.env）：

```bash
# ✅ 使用本地 Ollama Embedding
LLM_USE_OLLAMA_EMBEDDING=true
LLM_OLLAMA_BASE_URL=http://ollama:11434
LLM_EMBEDDING_MODEL=nomic-embed-text

# ✅ 使用火山引擎 Chat
LLM_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
LLM_API_KEY=12df7044-2100-46fb-bcf5-cff4c5f051c8
LLM_CHAT_MODEL=ep-20250915111522-f87sr
```

---

## 🎯 部署时间预估

### 首次部署

- 拉取代码：< 1 分钟
- 启动服务：1-2 分钟
- 下载模型：2-5 分钟（网速相关）
- 测试验证：2-3 分钟

**总计：5-10 分钟**

### 后续部署

- 拉取代码：< 1 分钟
- 重启后端：< 30 秒
- 测试验证：1-2 分钟

**总计：2-3 分钟**

---

## 🔐 安全提醒

### 生产环境必须修改

```bash
# .env 文件
ADMIN_PASSWORD=your-strong-password  # ⚠️ 修改
JWT_SECRET=random-64-char-string     # ⚠️ 修改
DB_PASSWORD=strong-db-password       # ⚠️ 修改
```

生成密钥：
```bash
# 管理员密码
openssl rand -base64 24

# JWT 密钥
openssl rand -hex 32
```

---

## 💡 快速命令参考

```bash
# ===== 首次部署 =====
git clone <repo> && cd demo
docker-compose up -d
./init-ollama.sh
docker-compose restart backend

# ===== 后续部署 =====
git pull
docker-compose restart backend

# ===== 查看状态 =====
docker-compose ps
docker-compose logs -f backend
docker exec lingdang-ollama ollama list

# ===== 重启服务 =====
docker-compose restart backend    # 仅后端
docker-compose restart ollama     # 仅 Ollama
docker-compose restart           # 全部

# ===== 清理（慎用）=====
docker-compose down              # 停止（保留数据）
docker-compose down -v           # 停止并删除数据
```

---

## 📞 需要帮助？

- 查看详细文档：`OLLAMA_DEPLOYMENT_GUIDE.md`
- 查看配置说明：`CONFIGURATION_CHECKLIST.md`
- 查看日志：`docker-compose logs <service>`

---

© 2026 铃铛师兄大模型 - 部署清单
