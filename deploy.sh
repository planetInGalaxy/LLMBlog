#!/bin/bash

##############################################
# 铃铛师兄大模型博客系统 - 云服务器部署脚本
##############################################

echo "========================================"
echo "  铃铛师兄大模型博客 - 一键部署脚本"
echo "========================================"

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo "❌ 请使用 root 用户或 sudo 运行此脚本"
    echo "运行: sudo bash deploy.sh"
    exit 1
fi

# 1. 更新系统
echo ""
echo "步骤 1/6: 更新系统..."
apt-get update -y
apt-get upgrade -y

# 2. 安装 Docker
echo ""
echo "步骤 2/6: 安装 Docker..."
if ! command -v docker &> /dev/null; then
    echo "正在安装 Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh
    systemctl start docker
    systemctl enable docker
    echo "✅ Docker 安装完成"
else
    echo "✅ Docker 已安装"
    docker --version
fi

# 3. 安装 Docker Compose
echo ""
echo "步骤 3/6: 安装 Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo "正在安装 Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    echo "✅ Docker Compose 安装完成"
else
    echo "✅ Docker Compose 已安装"
    docker-compose --version
fi

# 4. 配置防火墙
echo ""
echo "步骤 4/6: 配置防火墙..."
if command -v ufw &> /dev/null; then
    # Ubuntu/Debian
    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw allow 22/tcp
    echo "✅ UFW 防火墙已配置"
elif command -v firewall-cmd &> /dev/null; then
    # CentOS/RHEL
    firewall-cmd --permanent --add-service=http
    firewall-cmd --permanent --add-service=https
    firewall-cmd --permanent --add-service=ssh
    firewall-cmd --reload
    echo "✅ Firewalld 防火墙已配置"
else
    echo "⚠️  未检测到防火墙，请手动开放 80 和 443 端口"
fi

# 5. 停止旧服务
echo ""
echo "步骤 5/6: 停止旧服务..."
docker-compose down 2>/dev/null || echo "没有运行中的服务"

# 6. 启动服务
echo ""
echo "步骤 6/6: 启动服务..."
docker-compose up -d --build

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "========================================"
echo "  服务状态检查"
echo "========================================"
docker-compose ps

# 获取服务器 IP
SERVER_IP=$(curl -s ifconfig.me || hostname -I | awk '{print $1}')

echo ""
echo "========================================"
echo "  🎉 部署完成！"
echo "========================================"
echo ""
echo "🌐 访问地址:"
echo "   http://$SERVER_IP"
echo ""
echo "🔧 管理命令:"
echo "   查看日志: docker-compose logs -f"
echo "   重启服务: docker-compose restart"
echo "   停止服务: docker-compose down"
echo "   更新代码: git pull && docker-compose up -d --build"
echo ""
echo "📊 数据库信息:"
echo "   MySQL 端口: 3306"
echo "   用户名: root"
echo "   密码: root123456"
echo "   数据库: lingdang_blog"
echo ""
echo "💡 提示:"
echo "   1. 如需绑定域名，请修改 DNS A 记录指向: $SERVER_IP"
echo "   2. 建议配置 HTTPS 证书（使用 certbot）"
echo "   3. 定期备份 MySQL 数据"
echo ""
echo "========================================"
