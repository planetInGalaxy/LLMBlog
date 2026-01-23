# 🚀 自动化部署说明

## ✅ 已配置 GitHub Actions 自动部署

### 🎯 使用方法（超简单）

```bash
# 1. 修改代码
# 2. 提交推送
git add .
git commit -m "your message"
git push origin main

# 3. 完成！自动部署到服务器！
```

---

## 🤖 自动化流程

### 首次部署（约 5-10 分钟）

当系统检测到 **Ollama 模型不存在** 时：

```
✅ 拉取代码
✅ 构建所有镜像
✅ 启动所有服务
✅ 下载 Ollama 模型（270MB）
✅ 初始化完成
```

### 日常更新（约 2-3 分钟）⭐

当系统检测到 **Ollama 模型已存在** 时：

```
✅ 拉取代码
✅ 仅重新构建后端
✅ 仅重启后端容器
✅ MySQL/ES/Ollama 保持运行
✅ 数据不丢失，速度超快！
```

---

## 📊 部署状态查看

### 在 GitHub 查看

1. 进入仓库的 **Actions** 标签
2. 查看最新的 workflow 运行状态
3. 点击查看详细日志

### 在服务器查看

```bash
ssh root@your-server
cd /root/app/LLMBlog

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f backend
```

---

## 🎯 核心优势

| 优势 | 说明 |
|-----|------|
| **一键部署** | 推送代码即自动部署 |
| **智能识别** | 自动区分首次/日常部署 |
| **快速更新** | 日常更新仅需 2-3 分钟 |
| **数据安全** | 中间件不停止，数据不丢失 |
| **无需手动** | 无需 SSH 登录服务器操作 |

---

## 📚 详细文档

- **自动化指南**: `GITHUB_ACTIONS_GUIDE.md` ⭐
- **Ollama 部署**: `OLLAMA_DEPLOYMENT_GUIDE.md`
- **配置清单**: `CONFIGURATION_CHECKLIST.md`

---

## 🎉 开始使用

```bash
# 现在就可以开始了！
git add .
git commit -m "feat: 开始使用自动化部署"
git push origin main

# 🎉 自动部署启动！
```

---

© 2026 铃铛师兄大模型 - 自动化部署
