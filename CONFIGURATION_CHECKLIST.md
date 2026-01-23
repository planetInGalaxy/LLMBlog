# ✅ 配置清单

## 📋 必须配置项（3 项）

### 1. LLM API Key ⭐⭐⭐ 

```bash
LLM_API_KEY=sk-your-openai-api-key-here
```

- **作用**：用于调用 OpenAI API 生成 embedding 和回答问题
- **获取方式**：https://platform.openai.com/api-keys
- **测试方法**：
  ```bash
  curl https://api.openai.com/v1/models \
    -H "Authorization: Bearer $LLM_API_KEY"
  ```
- **重要性**：❌ 不配置则 RAG 功能完全不可用

---

### 2. 管理员密码 ⭐⭐⭐

```bash
ADMIN_PASSWORD=your-strong-password-here
```

- **作用**：Studio 后台登录密码
- **默认值**：`admin123456`（开发环境）
- **生产环境建议**：
  ```bash
  # 生成强密码
  openssl rand -base64 24
  # 示例：Kj8Hn2Qp9WxRv4Zm7Lt5Yf3B
  ```
- **重要性**：🚨 生产环境必须修改，否则有安全风险

---

### 3. JWT 密钥 ⭐⭐⭐

```bash
JWT_SECRET=random-64-character-string-here
```

- **作用**：JWT Token 签名密钥
- **默认值**：`lingdang-blog-jwt-secret-key-change-in-production`
- **生产环境建议**：
  ```bash
  # 生成随机密钥（64 字符）
  openssl rand -hex 32
  # 示例：a7f3e9d2c1b4a6f8e5d3c9b7a4f2e8d6c3b9a7f4e2d8c6b3a9f7e4d2c8b6a3f9
  ```
- **重要性**：🚨 生产环境必须修改，否则 Token 可被伪造

---

## 🔧 可选配置项（推荐使用默认值）

### LLM 配置

```bash
# Base URL（支持 OpenAI 兼容接口）
LLM_BASE_URL=https://api.openai.com/v1

# Embedding 模型
LLM_EMBEDDING_MODEL=text-embedding-3-small

# Chat 模型
LLM_CHAT_MODEL=gpt-4o-mini
```

**使用场景**：
- 使用 Azure OpenAI
- 使用国内第三方服务（DeepSeek、智谱等）
- 使用本地 Ollama

### 管理员配置

```bash
# 管理员用户名
ADMIN_USERNAME=admin
```

**使用场景**：自定义管理员用户名

### 数据库配置

```bash
# MySQL 密码
DB_PASSWORD=root123456
```

**使用场景**：生产环境建议修改

### Elasticsearch 配置

```bash
# ES 主机
ELASTICSEARCH_HOST=elasticsearch

# ES 端口
ELASTICSEARCH_PORT=9200
```

**使用场景**：使用外部 Elasticsearch 集群

### 限流配置

```bash
# Assistant 每小时请求限制（次/小时/IP）
RATE_LIMIT_ASSISTANT=30
```

**使用场景**：根据实际流量调整

---

## 📋 配置步骤

### 步骤 1：编辑 .env 文件

```bash
# 项目根目录已有 .env 文件示例
vim .env

# 或使用其他编辑器
nano .env
code .env
```

### 步骤 2：修改必须配置项

```bash
# 最少修改这 3 项：
LLM_API_KEY=sk-your-real-api-key-replace-this
ADMIN_PASSWORD=YourStr0ng!P@ssw0rd
JWT_SECRET=a7f3e9d2c1b4a6f8e5d3c9b7a4f2e8d6c3b9a7f4e2d8c6b3a9f7e4d2c8b6a3f9
```

### 步骤 3：启动服务

```bash
docker-compose up -d
```

### 步骤 4：验证配置

```bash
# 检查后端环境变量
docker-compose exec backend env | grep -E "LLM|ADMIN|JWT"

# 健康检查
curl http://localhost:8080/api/health

# 测试登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"YourStr0ng!P@ssw0rd"}'
```

---

## 🔐 安全建议

### 开发环境 ✅

```bash
# 可以使用默认值
ADMIN_PASSWORD=admin123456
JWT_SECRET=dev-secret-key-for-local-development
DB_PASSWORD=root123456
```

### 生产环境 🚨

```bash
# 必须使用强密码和随机密钥

# 管理员密码（至少 16 字符，包含大小写字母、数字、特殊符号）
ADMIN_PASSWORD=$(openssl rand -base64 24)

# JWT 密钥（64 字符随机字符串）
JWT_SECRET=$(openssl rand -hex 32)

# 数据库密码（至少 16 字符）
DB_PASSWORD=$(openssl rand -base64 24)
```

### 密钥管理 📝

1. **不要提交到 Git**
   - `.env` 文件已加入 `.gitignore`
   - 确认：`git status` 不应显示 `.env`

2. **备份密钥**
   - 将密钥保存到密码管理器
   - 或使用安全的密钥管理服务（如 HashiCorp Vault）

3. **定期轮换**
   - 建议每 3-6 个月轮换一次 JWT_SECRET
   - 建议每 6-12 个月修改一次 ADMIN_PASSWORD

---

## 🌐 不同环境配置示例

### 使用 OpenAI（默认）

```bash
LLM_BASE_URL=https://api.openai.com/v1
LLM_API_KEY=sk-proj-xxx...
LLM_EMBEDDING_MODEL=text-embedding-3-small
LLM_CHAT_MODEL=gpt-4o-mini
```

### 使用 Azure OpenAI

```bash
LLM_BASE_URL=https://your-resource.openai.azure.com/openai/deployments
LLM_API_KEY=your-azure-api-key
LLM_EMBEDDING_MODEL=your-embedding-deployment-name
LLM_CHAT_MODEL=your-chat-deployment-name
```

### 使用 DeepSeek

```bash
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_API_KEY=your-deepseek-api-key
LLM_EMBEDDING_MODEL=deepseek-embedding
LLM_CHAT_MODEL=deepseek-chat
```

### 使用本地 Ollama

```bash
LLM_BASE_URL=http://host.docker.internal:11434/v1
LLM_API_KEY=ollama
LLM_EMBEDDING_MODEL=nomic-embed-text
LLM_CHAT_MODEL=qwen2.5:7b
```

---

## ✅ 配置验证清单

启动服务前，请确认：

- [ ] `.env` 文件存在于项目根目录
- [ ] `LLM_API_KEY` 已填写真实有效的 API Key
- [ ] `ADMIN_PASSWORD` 已修改（生产环境）
- [ ] `JWT_SECRET` 已修改（生产环境）
- [ ] `DB_PASSWORD` 已修改（生产环境）
- [ ] 所有密钥已备份到安全位置
- [ ] `.env` 文件不在 Git 版本控制中

启动服务后，请确认：

- [ ] 后端健康检查通过：`curl http://localhost:8080/api/health`
- [ ] 可以正常登录 Studio
- [ ] Assistant 查询返回正常结果
- [ ] ES 连接正常：`curl http://localhost:9200`

---

## 📞 遇到问题？

### LLM API Key 无效

**症状**：索引任务失败，提示 API Key 错误

**解决**：
```bash
# 测试 API Key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $LLM_API_KEY"

# 如果返回 401，说明 API Key 无效
# 请前往 https://platform.openai.com/api-keys 检查或重新生成
```

### 管理员无法登录

**症状**：提示"用户名或密码错误"

**解决**：
```bash
# 检查配置
docker-compose exec backend env | grep ADMIN

# 确认用户名和密码正确
# 注意：密码区分大小写
```

### JWT Token 无效

**症状**：登录后立即提示未授权

**解决**：
```bash
# 检查 JWT_SECRET 是否配置
docker-compose exec backend env | grep JWT

# 重启后端服务
docker-compose restart backend
```

---

## 🎯 快速开始

**最快 3 步启动**：

```bash
# 1. 修改 .env 文件（至少修改 LLM_API_KEY）
vim .env

# 2. 启动服务
docker-compose up -d

# 3. 访问系统
open http://localhost
```

---

© 2026 铃铛师兄大模型 - 配置清单
