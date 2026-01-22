# 🚀 快速启动指南 - 铃铛师兄大模型博客系统

## 📋 环境检查

### Mac 用户必备工具

你的 Mac 已经安装了以下工具：

✅ **已安装：**
- Git 2.50.1
- Node.js v25.4.0
- Docker 28.3.0
- Maven 3.8.1
- **JDK 17** (已通过 Homebrew 安装在 `/opt/homebrew/opt/openjdk@17`)

### ⚠️ 重要：配置 JDK 17

你的系统当前默认使用 JDK 8，需要切换到 JDK 17。有两种方法：

#### 方法 1：临时切换（推荐用于测试）

每次运行前执行：
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
java -version  # 验证版本
```

#### 方法 2：永久切换（推荐）

编辑 `~/.zshrc` 文件：
```bash
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version  # 验证版本
```

## 🎯 方式一：一键启动（推荐新手）

### 使用启动脚本

```bash
# 配置 JDK 17（如果还没配置）
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"

# 一键启动
./start-local.sh
```

启动后：
- 🌐 前端：http://localhost:3000
- 🔧 后端：http://localhost:8080
- 📡 API：http://localhost:8080/api/posts

### 停止服务

```bash
./stop-local.sh
```

## 🎯 方式二：手动启动（适合开发）

### 步骤 1：启动后端

```bash
cd backend

# 确保使用 JDK 17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# 构建项目
mvn clean package -DskipTests

# 运行（开发模式，使用 H2 内存数据库）
mvn spring-boot:run

# 或者运行打包后的 jar
java -jar target/blog-backend-1.0.0.jar
```

后端启动后访问：http://localhost:8080/api/posts

### 步骤 2：启动前端

在新终端窗口：

```bash
cd frontend

# 首次运行需要安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后访问：http://localhost:3000

## 🐳 方式三：Docker 部署（一键启动全栈）

### 使用 Docker Compose

```bash
# 构建并启动所有服务（MySQL + 后端 + 前端）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

Docker 启动后：
- 🌐 完整网站：http://localhost
- 🔧 后端 API：http://localhost:8080
- 💾 MySQL：localhost:3306

### 停止 Docker 服务

```bash
# 停止服务
docker-compose down

# 停止并删除数据
docker-compose down -v
```

## 🎨 功能演示

启动成功后，你可以：

1. **浏览文章** - 查看预设的 3 篇示例博客文章
2. **搜索功能** - 在搜索框输入关键词搜索文章
3. **查看详情** - 点击文章卡片查看完整内容
4. **浏览统计** - 每次查看文章会自动增加浏览次数

## 🔧 常见问题

### Q1: Maven 构建失败，提示 Java 版本错误

**解决方案：**
```bash
# 设置 JDK 17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
# 验证
java -version  # 应该显示 17.x.x
mvn -version   # 应该显示使用 Java 17
```

### Q2: 前端无法连接后端

**检查清单：**
1. 确保后端服务在运行（访问 http://localhost:8080/api/posts 应该返回数据）
2. 检查浏览器控制台是否有跨域错误
3. 确认前端配置的 API 地址正确

### Q3: Docker 启动失败

**解决方案：**
```bash
# 查看详细日志
docker-compose logs

# 重新构建镜像
docker-compose build --no-cache

# 清理并重启
docker-compose down -v
docker-compose up -d
```

### Q4: 端口被占用

**解决方案：**
```bash
# 查找占用端口的进程
lsof -i :8080  # 后端端口
lsof -i :3000  # 前端端口
lsof -i :80    # Docker 前端端口

# 杀死进程（替换 PID）
kill -9 <PID>
```

## 📊 开发数据库访问

### H2 控制台（本地开发）

后端启动后访问：http://localhost:8080/h2-console

连接信息：
- JDBC URL: `jdbc:h2:mem:blogdb`
- 用户名: `sa`
- 密码: （留空）

### MySQL（Docker 生产环境）

```bash
# 连接 Docker MySQL
docker exec -it lingdang-mysql mysql -uroot -proot123456

# 查看数据库
use lingdang_blog;
show tables;
select * from blog_posts;
```

## ☁️ 云服务器部署步骤

### 1. 准备云服务器

推荐配置：
- 2核 4GB 内存
- 40GB 硬盘
- Ubuntu 20.04 或 CentOS 7+

### 2. 安装 Docker

```bash
# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 3. 上传项目

```bash
# 方法 1: 使用 Git
git clone <your-repo-url>

# 方法 2: 使用 SCP
scp -r demo/ user@server-ip:/home/user/
```

### 4. 启动服务

```bash
cd demo
docker-compose up -d
```

### 5. 配置防火墙

```bash
# Ubuntu/Debian
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# CentOS/RHEL
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 6. 配置域名（可选）

1. 在域名提供商处添加 A 记录指向服务器 IP
2. 等待 DNS 生效（通常几分钟）
3. 访问你的域名查看网站

## 📚 下一步

- 📖 阅读 [README.md](README.md) 了解完整项目文档
- 🔍 查看 API 文档了解接口详情
- 🎨 自定义前端样式和内容
- ✍️ 添加自己的博客文章
- 🔐 添加用户认证功能（扩展）
- 📊 集成数据分析（扩展）

## 💡 SEO 优化建议

网站标题已包含"铃铛师兄大模型"关键词，为了让搜索引擎更好地发现：

1. **提交到搜索引擎**
   - Google Search Console: https://search.google.com/search-console
   - 百度站长平台: https://ziyuan.baidu.com

2. **定期发布内容**
   - 每周发布 1-2 篇高质量原创文章
   - 文章标题包含相关关键词

3. **社交媒体分享**
   - 在微博、知乎等平台分享文章链接
   - 增加外部链接

---

**🔔 铃铛师兄大模型** - 专注 AI 技术分享

需要帮助？查看 [README.md](README.md) 或提交 Issue
