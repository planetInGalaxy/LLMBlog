#!/bin/bash

#############################################
# 铃铛师兄大模型博客 - 快速更新脚本
# 用途：日常代码更新和服务重启
#############################################

set -e

echo "========================================"
echo "  铃铛师兄大模型博客 - 快速更新"
echo "========================================"

# 检查是否在项目目录
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ 错误: 请在项目根目录运行此脚本"
    exit 1
fi

# 备份数据库
echo ""
echo "📦 步骤 1/4: 备份数据库..."
if docker-compose ps | grep -q "mysql"; then
    BACKUP_DIR="./backups"
    mkdir -p $BACKUP_DIR
    DATE=$(date +%Y%m%d_%H%M%S)
    docker-compose exec -T mysql mysqldump -uroot -proot123456 lingdang_blog > $BACKUP_DIR/backup_$DATE.sql 2>/dev/null || true
    echo "✅ 已备份到: $BACKUP_DIR/backup_$DATE.sql"
else
    echo "⚠️  数据库未运行，跳过备份"
fi

# 拉取最新代码
echo ""
echo "📥 步骤 2/4: 更新代码..."
if [ -d ".git" ]; then
    git pull
    echo "✅ 代码已更新"
else
    echo "⚠️  不是 Git 仓库，跳过"
fi

# 重新构建并启动
echo ""
echo "🚀 步骤 3/4: 重新部署..."
docker-compose down

# 生成前端构建版本号
APP_VERSION="dev"
if [ -d ".git" ]; then
    APP_VERSION=$(git rev-parse --short HEAD 2>/dev/null || echo "dev")
fi
export APP_VERSION
echo "🏷️  构建版本: $APP_VERSION"

docker-compose up -d --build

# 等待服务启动
echo ""
echo "⏳ 步骤 4/4: 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "========================================"
echo "📊 服务状态"
echo "========================================"
docker-compose ps

# 健康检查
echo ""
echo "🔍 健康检查..."
sleep 3

if curl -f http://localhost:8080/api/posts > /dev/null 2>&1; then
    echo "✅ 后端正常"
else
    echo "⚠️  后端可能未就绪"
fi

if curl -f http://localhost > /dev/null 2>&1; then
    echo "✅ 前端正常"
else
    echo "⚠️  前端可能未就绪"
fi

echo ""
echo "========================================"
echo "✅ 更新完成！"
echo "========================================"
echo ""
echo "🌐 访问地址: http://$(curl -s ifconfig.me 2>/dev/null || echo 'localhost')"
echo ""
echo "💡 常用命令:"
echo "   查看日志: docker-compose logs -f"
echo "   重启服务: docker-compose restart"
echo "   停止服务: docker-compose down"
echo ""
