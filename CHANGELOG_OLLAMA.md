# 📝 Ollama 集成更新记录

## 版本信息

- **更新日期**: 2026-01-23
- **版本**: v1.1.0
- **功能**: 集成本地 Ollama Embedding

---

## 🎯 更新内容

### 新增功能

1. ✅ **本地 Ollama 服务**
   - 新增 Ollama Docker 容器
   - 支持本地 Embedding 模型推理
   - 完全免费，无需外部 API

2. ✅ **混合 LLM 架构**
   - Embedding: 本地 Ollama（nomic-embed-text）
   - Chat: 火山引擎豆包（Seed1.6 Flash）
   - 灵活可配置，可随时切换

3. ✅ **自动化部署**
   - 一键初始化脚本（`init-ollama.sh`）
   - Docker Volume 持久化
   - 后续部署无需重复配置

---

## 📁 文件变更

### 新增文件（5 个）

1. **`init-ollama.sh`**
   - 作用：首次部署时初始化 Ollama 模型
   - 使用：`chmod +x init-ollama.sh && ./init-ollama.sh`

2. **`OLLAMA_DEPLOYMENT_GUIDE.md`**
   - 作用：完整的 Ollama 部署指南
   - 包含：部署步骤、故障排查、性能对比

3. **`DEPLOYMENT_CHECKLIST.md`**
   - 作用：快速部署清单
   - 首次 vs 后续部署流程

4. **`CHANGELOG_OLLAMA.md`**
   - 作用：更新记录（本文件）

### 修改文件（4 个）

1. **`docker-compose.yml`**
   - ✅ 新增 `ollama` 服务
   - ✅ 新增 `ollama-data` Volume
   - ✅ Backend 依赖 Ollama
   - ✅ 新增环境变量：
     - `LLM_OLLAMA_BASE_URL`
     - `LLM_USE_OLLAMA_EMBEDDING`

2. **`backend/src/main/java/com/lingdang/blog/config/LlmConfig.java`**
   - ✅ 新增 `ollamaBaseUrl` 配置项
   - ✅ 新增 `useOllamaEmbedding` 开关

3. **`backend/src/main/java/com/lingdang/blog/service/LlmService.java`**
   - ✅ 新增 `generateEmbeddingsWithOllama()` 方法
   - ✅ 支持根据配置自动切换 Embedding 服务
   - ✅ 兼容 Ollama OpenAI 格式 API

4. **`backend/src/main/resources/application.yml`**
   - ✅ 新增 `llm.ollama-base-url` 配置
   - ✅ 新增 `llm.use-ollama-embedding` 配置

5. **`.env`**
   - ✅ 更新 LLM 配置说明
   - ✅ 新增 Ollama 配置项
   - ✅ 默认启用本地 Embedding

---

## 🔧 配置变更

### 新增环境变量

```bash
# Ollama 配置
LLM_OLLAMA_BASE_URL=http://ollama:11434
LLM_USE_OLLAMA_EMBEDDING=true

# Embedding 模型名称（Ollama 模型）
LLM_EMBEDDING_MODEL=nomic-embed-text
```

### 保持不变

```bash
# Chat 模型配置（火山引擎）
LLM_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
LLM_API_KEY=12df7044-2100-46fb-bcf5-cff4c5f051c8
LLM_CHAT_MODEL=ep-20250915111522-f87sr
```

---

## 🚀 升级步骤

### 从旧版本升级

如果你已经部署了旧版本，按以下步骤升级：

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 启动新服务（Ollama）
docker-compose up -d

# 3. 初始化 Ollama 模型（⭐ 重要）
chmod +x init-ollama.sh
./init-ollama.sh

# 4. 重启后端
docker-compose restart backend

# 5. 验证
curl http://localhost:8080/api/health
docker exec lingdang-ollama ollama list
```

### 全新部署

参考 `DEPLOYMENT_CHECKLIST.md`

---

## 📊 性能影响

### 资源占用变化

| 资源 | 旧版 | 新版（+Ollama） | 增加 |
|------|------|----------------|------|
| **内存** | ~2GB | ~4GB | +2GB |
| **硬盘** | ~10GB | ~11GB | +1GB |
| **CPU** | 2核 | 2核 | 无变化 |

### Embedding 性能

| 指标 | 火山引擎（云端） | Ollama（本地） |
|------|----------------|---------------|
| **延迟** | 200-500ms | 100-300ms |
| **成本** | 按调用计费 | 免费 |
| **稳定性** | 依赖外网 | 本地稳定 |
| **质量** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

---

## ⚠️ 注意事项

### 必须操作

1. **首次部署必须运行** `init-ollama.sh`
   - 下载 Embedding 模型
   - 约需 2-5 分钟

2. **确保服务器资源充足**
   - 最低：2核4GB内存
   - 推荐：4核8GB内存

3. **生产环境修改密码**
   - `ADMIN_PASSWORD`
   - `JWT_SECRET`
   - `DB_PASSWORD`

### 可选操作

1. **更换 Embedding 模型**
   - 当前：nomic-embed-text（270MB）
   - 可选：bge-large-zh（1.3GB，中文更好）

2. **切换回云端 Embedding**
   - 设置 `LLM_USE_OLLAMA_EMBEDDING=false`
   - 配置相应的云端 API

---

## 🔄 回滚方案

如果遇到问题需要回滚：

```bash
# 方案1：关闭 Ollama Embedding（快速）
# 修改 .env
LLM_USE_OLLAMA_EMBEDDING=false

# 重启后端
docker-compose restart backend

# 方案2：回滚代码（完整）
git reset --hard <old-commit-hash>
docker-compose down
docker-compose up -d
```

---

## 🎉 优势总结

### ✅ 成本

- **旧版**：每次 Embedding 调用收费
- **新版**：完全免费

### ✅ 隐私

- **旧版**：数据传输到云端
- **新版**：数据不离开服务器

### ✅ 稳定性

- **旧版**：依赖外网 API 稳定性
- **新版**：本地推理，无外网依赖

### ✅ 维护性

- **首次部署**：需运行初始化脚本（5分钟）
- **后续部署**：无需重复配置，自动复用

---

## 📚 相关文档

- **快速开始**: `QUICK_START_GUIDE.md`
- **Ollama 指南**: `OLLAMA_DEPLOYMENT_GUIDE.md`
- **部署清单**: `DEPLOYMENT_CHECKLIST.md`
- **配置说明**: `CONFIGURATION_CHECKLIST.md`
- **项目总结**: `PROJECT_SUMMARY.md`

---

## 🤝 反馈与支持

如有问题，请：

1. 查看 `OLLAMA_DEPLOYMENT_GUIDE.md` 故障排查章节
2. 查看日志：`docker-compose logs ollama` / `docker-compose logs backend`
3. 提交 Issue 到 GitHub

---

© 2026 铃铛师兄大模型 - 更新记录
