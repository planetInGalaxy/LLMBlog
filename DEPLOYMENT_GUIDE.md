# 🚀 云服务器部署完整指南

## 📋 目录

- [云服务商选择](#云服务商选择)
- [方案一：一键部署（推荐）](#方案一一键部署推荐)
- [方案二：手动部署](#方案二手动部署)
- [方案三：GitHub Actions 自动部署](#方案三github-actions-自动部署)
- [域名绑定](#域名绑定)
- [HTTPS 配置](#https-配置)
- [常见问题](#常见问题)

---

## 🌩️ 云服务商选择

### 推荐：阿里云或腾讯云

| 对比项 | 阿里云 | 腾讯云 |
|--------|--------|--------|
| **新手优惠** | 2核2G ¥88/年 | 2核2G ¥88/年 |
| **推荐配置** | 2核4G ¥300/年 | 2核4G ¥280/年 |
| **网络质量** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **文档** | 非常全 | 比较全 |
| **客服** | 好 | 好 |

### 购买建议

**最低配置（测试用）：**
- CPU: 1核
- 内存: 2GB
- 硬盘: 20GB
- 带宽: 1Mbps

**推荐配置（正式使用）：**
- CPU: 2核
- 内存: 4GB
- 硬盘: 40GB
- 带宽: 3Mbps
- **预估费用：¥280-350/年**

**操作系统选择：**
- ✅ Ubuntu 20.04 LTS（推荐新手）
- ✅ Ubuntu 22.04 LTS
- ✅ CentOS 7.9

---

## 🎯 方案一：一键部署（推荐）

### 适合人群
- 首次部署
- 不熟悉 Linux 命令
- 想要快速上线

### 步骤

#### 1. 购买云服务器

在阿里云或腾讯云购买服务器后，记录：
- 服务器公网IP
- SSH 登录密码

#### 2. 连接服务器

Mac 终端：
```bash
ssh root@你的服务器IP
# 输入密码
```

#### 3. 上传项目

**方法 A：使用 Git（推荐）**
```bash
# 在服务器上安装 Git
apt-get update
apt-get install -y git

# 克隆项目（先把项目推送到 GitHub）
cd /root
git clone https://github.com/你的用户名/demo.git
cd demo
```

**方法 B：使用 SCP 上传**

在本地 Mac 终端：
```bash
cd /Users/tangjiaguo/code
scp -r demo root@你的服务器IP:/root/
```

#### 4. 运行一键部署脚本

```bash
cd /root/demo
chmod +x deploy.sh
sudo bash deploy.sh
```

脚本会自动：
- ✅ 安装 Docker
- ✅ 安装 Docker Compose
- ✅ 配置防火墙
- ✅ 启动所有服务

#### 5. 访问网站

部署完成后，打开浏览器访问：
```
http://你的服务器IP
```

**🎉 完成！你的博客已上线！**

---

## 🔧 方案二：手动部署

### 适合人群
- 想要了解部署细节
- 需要自定义配置

### 详细步骤

#### 1. 连接服务器
```bash
ssh root@你的服务器IP
```

#### 2. 安装 Docker
```bash
# 下载安装脚本
curl -fsSL https://get.docker.com -o get-docker.sh

# 运行安装
sh get-docker.sh

# 启动 Docker
systemctl start docker
systemctl enable docker

# 验证安装
docker --version
```

#### 3. 安装 Docker Compose
```bash
# 下载最新版本
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

#### 4. 上传项目代码
```bash
# 方法1: Git（推荐）
cd /root
git clone https://github.com/你的用户名/demo.git

# 方法2: 在本地用 SCP
# scp -r demo root@服务器IP:/root/
```

#### 5. 配置防火墙

**Ubuntu/Debian:**
```bash
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 22/tcp
ufw enable
```

**CentOS/RHEL:**
```bash
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --permanent --add-service=ssh
firewall-cmd --reload
```

**阿里云/腾讯云安全组：**
在云控制台添加安全规则：
- 开放端口 80 (HTTP)
- 开放端口 443 (HTTPS)
- 开放端口 22 (SSH)

#### 6. 启动服务
```bash
cd /root/demo

# 构建并启动
docker-compose up -d --build

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

#### 7. 验证部署
```bash
# 检查容器状态
docker ps

# 测试后端
curl http://localhost:8080/api/posts

# 测试前端
curl http://localhost
```

---

## 🤖 方案三：GitHub Actions 自动部署

### 适合人群
- 有 GitHub 仓库
- 想要自动化部署
- 推送代码后自动更新

### 配置步骤

#### 1. 创建 GitHub 仓库
```bash
# 在本地推送代码
cd /Users/tangjiaguo/code/demo
git remote add origin https://github.com/你的用户名/demo.git
git push -u origin master
```

#### 2. 生成 SSH 密钥对

在本地 Mac：
```bash
ssh-keygen -t rsa -b 4096 -C "deploy-key"
# 保存到: ~/.ssh/deploy_key
```

#### 3. 配置服务器

在服务器上：
```bash
# 添加公钥到服务器
cat ~/.ssh/deploy_key.pub >> ~/.ssh/authorized_keys

# 测试连接
ssh -i ~/.ssh/deploy_key root@服务器IP
```

#### 4. 配置 GitHub Secrets

在 GitHub 仓库设置中添加 Secrets：
- `SERVER_HOST`: 你的服务器IP
- `SERVER_USER`: root
- `SERVER_SSH_KEY`: 私钥内容（`cat ~/.ssh/deploy_key`）

#### 5. 创建 GitHub Actions 工作流

创建文件 `.github/workflows/deploy.yml`:
```yaml
name: 部署到云服务器

on:
  push:
    branches: [ master, main ]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    
    steps:
    - name: 检出代码
      uses: actions/checkout@v3
    
    - name: 部署到服务器
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SERVER_SSH_KEY }}
        script: |
          cd /root/demo
          git pull origin master
          docker-compose down
          docker-compose up -d --build
```

#### 6. 推送并自动部署
```bash
git add .
git commit -m "添加自动部署"
git push
```

**🎉 以后每次推送代码，都会自动部署到服务器！**

---

## 🌐 域名绑定

### 1. 购买域名
在阿里云、腾讯云或其他域名商购买域名。

### 2. 配置 DNS 解析
添加 A 记录：
```
类型: A
主机记录: @
记录值: 你的服务器IP
TTL: 600
```

### 3. 等待生效
通常 5-30 分钟内生效。

### 4. 修改 Nginx 配置（可选）
```bash
# 编辑 frontend/nginx.conf
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    ...
}
```

---

## 🔒 HTTPS 配置

### 使用 Let's Encrypt 免费证书

#### 1. 安装 Certbot
```bash
apt-get update
apt-get install -y certbot python3-certbot-nginx
```

#### 2. 停止前端容器
```bash
cd /root/demo
docker-compose stop frontend
```

#### 3. 获取证书
```bash
certbot certonly --standalone -d your-domain.com -d www.your-domain.com
```

#### 4. 修改 docker-compose.yml
```yaml
frontend:
  ...
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - /etc/letsencrypt:/etc/letsencrypt:ro
```

#### 5. 更新 Nginx 配置
```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;
    
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    
    ...
}
```

#### 6. 重启服务
```bash
docker-compose up -d
```

---

## 🛠️ 日常维护命令

### 查看服务状态
```bash
docker-compose ps
```

### 查看日志
```bash
# 所有服务
docker-compose logs -f

# 特定服务
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 重启服务
```bash
docker-compose restart
```

### 停止服务
```bash
docker-compose down
```

### 更新代码并重启
```bash
git pull
docker-compose down
docker-compose up -d --build
```

### 清理 Docker 资源
```bash
# 清理未使用的镜像
docker image prune -a

# 清理所有未使用资源
docker system prune -af --volumes
```

### 备份数据库
```bash
# 导出数据库
docker exec lingdang-mysql mysqldump -uroot -proot123456 lingdang_blog > backup.sql

# 恢复数据库
docker exec -i lingdang-mysql mysql -uroot -proot123456 lingdang_blog < backup.sql
```

---

## ❓ 常见问题

### Q1: 无法访问网站？

**检查清单：**
```bash
# 1. 检查容器状态
docker-compose ps

# 2. 检查防火墙
ufw status  # Ubuntu
firewall-cmd --list-all  # CentOS

# 3. 检查云服务商安全组
# 登录控制台查看 80 端口是否开放

# 4. 检查日志
docker-compose logs
```

### Q2: 数据库连接失败？

```bash
# 检查 MySQL 容器
docker-compose ps mysql

# 查看 MySQL 日志
docker-compose logs mysql

# 进入 MySQL 容器
docker exec -it lingdang-mysql mysql -uroot -proot123456
```

### Q3: 内存不足？

```bash
# 查看内存使用
free -h

# 创建 swap 交换空间
dd if=/dev/zero of=/swapfile bs=1G count=2
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

### Q4: 磁盘空间不足？

```bash
# 查看磁盘使用
df -h

# 清理 Docker
docker system prune -af --volumes

# 清理日志
journalctl --vacuum-size=100M
```

### Q5: 如何更新博客内容？

**方法 1: 修改代码**
```bash
# 编辑 DataInitializer.java
# 重新构建部署
docker-compose up -d --build
```

**方法 2: 通过 API**
```bash
# 使用 Postman 或 curl 调用 API
curl -X POST http://服务器IP/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"新文章","content":"内容","author":"铃铛师兄","published":true}'
```

---

## 📊 监控和优化

### 安装监控工具（可选）
```bash
# 安装 htop
apt-get install -y htop

# 安装 Docker 监控
docker run -d --name=cadvisor \
  -p 8888:8080 \
  -v /:/rootfs:ro \
  -v /var/run:/var/run:ro \
  -v /sys:/sys:ro \
  -v /var/lib/docker/:/var/lib/docker:ro \
  google/cadvisor:latest
```

### 性能优化建议
1. 使用 CDN 加速静态资源
2. 启用 Nginx gzip 压缩
3. 配置 MySQL 慢查询日志
4. 定期备份数据
5. 监控服务器资源

---

## 🎯 下一步

- [ ] 绑定自己的域名
- [ ] 配置 HTTPS 证书
- [ ] 设置自动备份
- [ ] 添加监控告警
- [ ] 优化 SEO
- [ ] 提交到搜索引擎

---

**需要帮助？** 查看 [README.md](README.md) 或提交 Issue

**铃铛师兄大模型** 🔔 - 让部署变简单
