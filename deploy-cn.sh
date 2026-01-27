#!/bin/bash

##############################################
# 铃铛师兄大模型博客系统 - 云服务器部署脚本（国内优化版）
##############################################

echo "========================================"
echo "  铃铛师兄大模型博客 - 一键部署脚本"
echo "  (国内网络优化版)"
echo "========================================"

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then 
    echo "❌ 请使用 root 用户或 sudo 运行此脚本"
    echo "运行: sudo bash deploy-cn.sh"
    exit 1
fi

# 1. 更新系统
echo ""
echo "步骤 1/6: 更新系统..."
export DEBIAN_FRONTEND=noninteractive
export NEEDRESTART_MODE=a
apt-get update -y
apt-get upgrade -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold"

# 2. 安装 Docker（使用阿里云镜像）
echo ""
echo "步骤 2/6: 安装 Docker（使用国内镜像源）..."
if ! command -v docker &> /dev/null; then
    echo "正在安装 Docker..."
    
    # 卸载旧版本
    apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true
    
    # 安装依赖
    apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
    
    # 添加阿里云 Docker GPG 密钥
    curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    
    # 添加阿里云 Docker 源
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu \
      $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    
    # 更新并安装 Docker
    apt-get update -y
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    # 启动 Docker
    systemctl start docker
    systemctl enable docker
    
    # 配置 Docker 国内镜像加速
    mkdir -p /etc/docker
    cat > /etc/docker/daemon.json <<EOF
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://registry.docker-cn.com",
    "https://docker.mirrors.ustc.edu.cn"
  ]
}
EOF
    
    systemctl daemon-reload
    systemctl restart docker
    
    echo "✅ Docker 安装完成"
    docker --version
else
    echo "✅ Docker 已安装"
    docker --version
fi

# 3. 安装 Docker Compose（如果没有）
echo ""
echo "步骤 3/6: 检查 Docker Compose..."
if ! docker compose version &> /dev/null; then
    if ! command -v docker-compose &> /dev/null; then
        echo "正在安装 Docker Compose..."
        # 使用国内镜像加速
        curl -L "https://get.daocloud.io/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        chmod +x /usr/local/bin/docker-compose
        echo "✅ Docker Compose 安装完成"
    fi
else
    echo "✅ Docker Compose 已安装"
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
docker compose down 2>/dev/null || docker-compose down 2>/dev/null || echo "没有运行中的服务"

# 6. 启动服务
echo ""
echo "步骤 6/6: 启动服务..."
APP_VERSION="dev"
if [ -d ".git" ]; then
    APP_VERSION=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
fi
export APP_VERSION
echo "构建版本: $APP_VERSION"
docker compose up -d --build 2>/dev/null || docker-compose up -d --build

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 15

# 检查服务状态
echo ""
echo "========================================"
echo "  服务状态检查"
echo "========================================"
docker compose ps 2>/dev/null || docker-compose ps

# 获取服务器 IP
SERVER_IP=$(curl -s ip.sb || curl -s ifconfig.me || hostname -I | awk '{print $1}')

echo ""
echo "========================================"
echo "  🎉 部署完成！"
echo "========================================"
echo ""
echo "🌐 访问地址:"
echo "   http://$SERVER_IP"
echo ""
echo "🔧 管理命令:"
echo "   查看日志: docker compose logs -f"
echo "   重启服务: docker compose restart"
echo "   停止服务: docker compose down"
echo "   更新代码: git pull && docker compose up -d --build"
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
echo "   4. 已配置 Docker 国内镜像加速"
echo ""
echo "🔍 验证部署:"
echo "   后端API: curl http://localhost:8080/api/posts"
echo "   前端: curl http://localhost"
echo ""
echo "========================================"
