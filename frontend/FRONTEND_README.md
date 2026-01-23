# 前端使用说明

## 切换到新前端

1. 备份原有文件：
```bash
mv src/main.jsx src/main-old.jsx
mv src/App.jsx src/App-old.jsx
```

2. 使用新文件：
```bash
mv src/main-new.jsx src/main.jsx
mv src/AppNew.jsx src/App.jsx
```

3. 安装依赖：
```bash
npm install
```

4. 启动开发服务器：
```bash
npm run dev
```

## 路由结构

- `/` - 首页
- `/blog` - 文章列表
- `/blog/:slug` - 文章详情
- `/assistant` - AI 助手
- `/studio/login` - 后台登录
- `/studio/articles` - 文章管理
- `/studio/articles/new` - 新建文章
- `/studio/articles/:id/edit` - 编辑文章

## 功能说明

### 公开页面
- **首页**：展示系统简介
- **博客列表**：浏览已发布文章
- **AI 助手**：基于文章库的 RAG 问答

### Studio 后台（需登录）
- **文章管理**：CRUD 操作
- **发布文章**：触发索引流水线
- **Markdown 编辑**：直接编辑 Markdown

## 注意事项

1. 本前端是简化版 MVP，仅包含核心功能
2. 样式需要根据 App.css 调整
3. 需要后端 API 正常运行
4. JWT Token 存储在 localStorage
