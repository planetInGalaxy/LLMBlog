#!/bin/bash

###############################################
# Docker 安装脚本 - 使用国内镜像源
# 适用于：阿里云、腾讯云等国内服务器
###############################################

echo "========================================"
echo "  安装 Docker（使用国内镜像）"
echo "========================================"

# 检查是否已安装
if command -v docker &> /dev/null; then
    echo "✅ Docker 已安装"
    docker --version
    
    # 检查 Docker 是否运行
    if systemctl is-active --quiet docker; then
        echo "✅ Docker 服务正在运行"
        exit 0
    else
        echo "🔄 启动 Docker 服务..."
        systemctl start docker
        systemctl enable docker
        exit 0
    fi
fi

echo ""
echo "步骤 1/5: 卸载旧版本..."
apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

echo ""
echo "步骤 2/5: 安装依赖..."
apt-get update
apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

echo ""
echo "步骤 3/5: 添加 Docker GPG 密钥（阿里云镜像）..."
mkdir -p /etc/apt/keyrings
curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo ""
echo "步骤 4/5: 添加 Docker 软件源（阿里云镜像）..."
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

echo ""
echo "步骤 5/5: 安装 Docker..."
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo ""
echo "启动 Docker 服务..."
systemctl start docker
systemctl enable docker

echo ""
echo "验证安装..."
docker --version
docker compose version

echo ""
echo "========================================"
echo "✅ Docker 安装完成！"
echo "========================================"
echo ""
echo "测试运行:"
echo "  docker run hello-world"
echo ""
