# 🚀 Ollama 本地 Embedding 部署指南

## 📋 部署概述

**目标**：使用本地 Ollama 替代云端 Embedding API，实现：
- ✅ 完全免费（无 API 费用）
- ✅ 隐私安全（数据不出本地）
- ✅ 稳定可靠（不依赖外网）

**架构**：
```
Chat（回答）    → 火山引擎豆包 Seed1.6 Flash（云端）
Embedding（向量） → Ollama nomic-embed-text（本地）
```

---

## 🔧 首次部署步骤

### 步骤 1：推送代码到 GitHub

```bash
# 在本地项目目录
git add .
git commit -m "feat: 集成 Ollama 本地 Embedding"
git push origin main
```

### 步骤 2：在服务器上拉取代码

```bash
# SSH 登录服务器
ssh user@your-server-ip

# 进入项目目录（如果是首次部署，先 clone）
cd /path/to/demo
# 或首次：git clone https://github.com/your-username/demo.git

# 拉取最新代码
git pull origin main
```

### 步骤 3：启动所有服务

```bash
# 启动服务（包括新增的 Ollama）
docker-compose up -d

# 查看服务状态
docker-compose ps
```

预期输出：
```
NAME                      STATUS
lingdang-mysql            Up
lingdang-elasticsearch    Up
lingdang-ollama           Up  ← 新增
lingdang-backend          Up
lingdang-frontend         Up
```

### 步骤 4：初始化 Ollama 模型（⭐ 仅首次需要）

```bash
# 执行初始化脚本
chmod +x init-ollama.sh
./init-ollama.sh
```

**说明**：
- 该脚本会下载 `nomic-embed-text` 模型（约 270MB）
- 下载时间：1-5 分钟（取决于网速）
- **仅首次部署需要执行**
- 模型保存在 Docker Volume 中，后续无需重复下载

### 步骤 5：重启后端服务

```bash
# 重启后端以应用新配置
docker-compose restart backend

# 查看后端日志
docker-compose logs -f backend
```

### 步骤 6：验证部署

```bash
# 1. 健康检查
curl http://localhost:8080/api/health

# 2. 检查 Ollama 服务
curl http://localhost:11434/api/version

# 3. 查看已下载的模型
docker exec lingdang-ollama ollama list
```

预期看到：
```
NAME                    SIZE
nomic-embed-text:latest 270MB
```

---

## 🔄 后续推送（无需重新部署 Ollama）

### 代码更新流程

```bash
# 本地修改代码后
git add .
git commit -m "your message"
git push origin main

# 服务器上拉取
ssh user@your-server-ip
cd /path/to/demo
git pull origin main

# 仅重启后端（无需重启 Ollama/ES/MySQL）
docker-compose restart backend
```

**重要**：
- Ollama 模型已保存在 `ollama-data` Volume 中
- 只要不删除 Volume，模型永久保留
- 后续部署**无需**再次运行 `init-ollama.sh`

---

## 📊 配置说明

### .env 文件配置（已自动配置好）

```bash
# ✅ 使用本地 Ollama
LLM_USE_OLLAMA_EMBEDDING=true
LLM_OLLAMA_BASE_URL=http://ollama:11434
LLM_EMBEDDING_MODEL=nomic-embed-text

# ✅ Chat 仍使用火山引擎
LLM_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
LLM_API_KEY=12df7044-2100-46fb-bcf5-cff4c5f051c8
LLM_CHAT_MODEL=ep-20250915111522-f87sr
```

### 如何切换回云端 Embedding

如果将来想切换回云端（如 OpenAI）：

```bash
# 修改 .env
LLM_USE_OLLAMA_EMBEDDING=false
LLM_BASE_URL=https://api.openai.com/v1
LLM_API_KEY=sk-your-openai-key
LLM_EMBEDDING_MODEL=text-embedding-3-small

# 重启后端
docker-compose restart backend
```

---

## 🧪 功能测试

### 测试 Embedding 生成

1. **登录 Studio**
   ```
   http://your-server-ip/studio/login
   用户名：admin
   密码：（你配置的密码）
   ```

2. **创建并发布文章**
   - 点击「新建文章」
   - 填写标题、内容
   - 点击「发布」
   - 观察后端日志

3. **预期日志**
   ```bash
   docker-compose logs backend | grep -i "ollama"
   ```
   应看到：
   ```
   INFO - 使用 Ollama 生成 embeddings: 15 个文本
   INFO - Chunk 索引成功: 15 个片段
   ```

### 测试 AI 助手

1. **访问助手页面**
   ```
   http://your-server-ip/assistant
   ```

2. **提问测试**
   - 输入问题：「文章中提到了什么？」
   - 点击「提问」
   - 应该返回带引用的答案

---

## 📦 Docker Volume 管理

### 查看 Volumes

```bash
docker volume ls
```

应看到：
```
VOLUME NAME
demo_mysql-data
demo_es-data
demo_ollama-data  ← Ollama 模型存储
```

### Volume 大小

```bash
docker system df -v
```

### 清理 Volume（⚠️ 慎用）

```bash
# 删除所有数据（包括 Ollama 模型）
docker-compose down -v

# 仅删除 Ollama 数据
docker volume rm demo_ollama-data
```

---

## 🔍 故障排查

### 问题 1：Ollama 服务启动失败

**症状**：`docker-compose ps` 显示 ollama 状态异常

**解决**：
```bash
# 查看日志
docker-compose logs ollama

# 重启服务
docker-compose restart ollama

# 检查健康状态
curl http://localhost:11434/api/version
```

### 问题 2：模型下载失败

**症状**：`init-ollama.sh` 执行报错

**解决**：
```bash
# 手动下载模型
docker exec -it lingdang-ollama ollama pull nomic-embed-text

# 如果网络问题，可以多次重试
# 或使用代理：
docker exec -it lingdang-ollama sh -c "HTTP_PROXY=http://proxy:port ollama pull nomic-embed-text"
```

### 问题 3：Embedding 生成失败

**症状**：发布文章时索引任务失败

**检查清单**：
```bash
# 1. 检查 Ollama 是否运行
curl http://localhost:11434/api/version

# 2. 检查模型是否存在
docker exec lingdang-ollama ollama list

# 3. 检查后端配置
docker-compose exec backend env | grep LLM

# 4. 查看详细错误
docker-compose logs backend | tail -100
```

### 问题 4：后端无法连接 Ollama

**症状**：后端日志显示 "Connection refused"

**解决**：
```bash
# 1. 确认 Ollama 服务名正确
docker-compose ps ollama

# 2. 测试网络连通性
docker-compose exec backend curl http://ollama:11434/api/version

# 3. 检查 docker-compose 网络
docker network ls
docker network inspect demo_lingdang-network
```

---

## 📈 性能对比

| 指标 | Ollama (本地) | OpenAI (云端) | 火山引擎 (云端) |
|------|--------------|--------------|---------------|
| **Embedding 速度** | 100-300ms | 200-500ms | 200-500ms |
| **成本** | 免费 | $0.0001/1K tokens | 按调用计费 |
| **隐私** | 完全本地 | 云端传输 | 云端传输 |
| **依赖** | 本地资源 | 外网稳定性 | 外网稳定性 |
| **质量** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## 🔄 升级 Ollama 模型

### 更换为其他模型

如果想用其他 Embedding 模型：

```bash
# 1. 下载新模型（如 all-minilm）
docker exec lingdang-ollama ollama pull all-minilm

# 2. 修改 .env
LLM_EMBEDDING_MODEL=all-minilm

# 3. 重启后端
docker-compose restart backend
```

### 推荐模型

| 模型名称 | 大小 | 支持语言 | 推荐度 |
|---------|------|---------|--------|
| **nomic-embed-text** | 270MB | 中英文 | ⭐⭐⭐⭐⭐ 推荐 |
| **all-minilm** | 45MB | 英文为主 | ⭐⭐⭐ |
| **bge-large-zh** | 1.3GB | 中文优秀 | ⭐⭐⭐⭐ |

---

## 💡 最佳实践

### 1. 首次部署

```bash
# 完整流程（首次）
git pull
docker-compose up -d
./init-ollama.sh          # ⭐ 仅首次
docker-compose restart backend
```

### 2. 日常更新

```bash
# 代码更新流程（日常）
git pull
docker-compose restart backend  # 仅重启后端
```

### 3. 备份策略

```bash
# 备份 Ollama 模型（可选）
docker volume inspect demo_ollama-data

# 如果需要迁移，可以备份整个 Volume
docker run --rm -v demo_ollama-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/ollama-backup.tar.gz -C /data .
```

---

## 🎯 总结

### ✅ 优势

1. **完全免费**：无 API 调用费用
2. **隐私安全**：数据不离开服务器
3. **稳定可靠**：不依赖外网 API
4. **性能优秀**：本地推理，延迟低

### ⚠️ 注意事项

1. **首次部署需下载模型**（约 5 分钟）
2. **增加资源占用**（+2GB 内存，+1GB 硬盘）
3. **后续部署无需重复下载**（Volume 持久化）

### 🚀 下一步

- 部署完成后测试发布文章
- 验证 AI 助手检索功能
- 监控 Ollama 服务状态

---

© 2026 铃铛师兄大模型 - Ollama 部署指南
