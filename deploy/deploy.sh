#!/bin/bash

# 铃铛师兄大模型博客 - 云服务器部署脚本
# 用途：在云服务器上快速部署或更新应用

set -e

echo "=================================="
echo "铃铛师兄大模型博客 - 云服务器部署"
echo "=================================="

# 配置变量
PROJECT_DIR="/home/demo"
BACKUP_DIR="/home/backup"
DATE=$(date +%Y%m%d_%H%M%S)

# 检查是否在服务器上
if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ 错误: 项目目录不存在: $PROJECT_DIR"
    echo "请先上传项目或使用 git clone"
    exit 1
fi

# 进入项目目录
cd $PROJECT_DIR

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，正在安装..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    echo "✅ Docker 安装完成"
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装，正在安装..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "✅ Docker Compose 安装完成"
fi

# 备份当前运行的容器（如果存在）
if docker-compose ps | grep -q "Up"; then
    echo "📦 备份当前数据..."
    mkdir -p $BACKUP_DIR
    docker-compose exec -T mysql mysqldump -uroot -proot123456 lingdang_blog > $BACKUP_DIR/backup_$DATE.sql || true
    echo "✅ 数据已备份到: $BACKUP_DIR/backup_$DATE.sql"
fi

# 拉取最新代码（如果使用 Git）
if [ -d ".git" ]; then
    echo "📥 拉取最新代码..."
    git pull origin master
    echo "✅ 代码已更新"
fi

# 停止旧容器
echo "🛑 停止旧容器..."
docker-compose down

# 清理旧镜像（可选）
echo "🧹 清理旧镜像..."
docker system prune -f

# 构建并启动新容器
echo "🚀 构建并启动服务..."
docker-compose up -d --build

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "=================================="
echo "📊 服务状态"
echo "=================================="
docker-compose ps

# 检查服务健康
echo ""
echo "🔍 健康检查..."
if curl -f http://localhost:8080/api/posts > /dev/null 2>&1; then
    echo "✅ 后端服务正常"
else
    echo "⚠️  后端服务可能未就绪，请稍后检查"
fi

if curl -f http://localhost > /dev/null 2>&1; then
    echo "✅ 前端服务正常"
else
    echo "⚠️  前端服务可能未就绪，请稍后检查"
fi

echo ""
echo "=================================="
echo "✅ 部署完成！"
echo "=================================="
echo "访问地址: http://$(curl -s ifconfig.me)"
echo ""
echo "查看日志: docker-compose logs -f"
echo "查看状态: docker-compose ps"
echo "停止服务: docker-compose down"
echo ""
