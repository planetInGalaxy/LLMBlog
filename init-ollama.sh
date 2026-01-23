#!/bin/bash
# ================================================================
# Ollama 模型初始化脚本
# ================================================================
# 用途：首次部署时下载 Ollama Embedding 模型
# 执行时机：docker-compose up -d 后首次运行
# 后续部署：不需要再次执行（模型已保存在 Volume 中）
# ================================================================

set -e

echo "🔔 铃铛师兄博客系统 - Ollama 初始化"
echo "========================================"

# 检查 Ollama 服务是否运行
echo "📡 检查 Ollama 服务状态..."
if ! docker-compose ps ollama | grep -q "Up"; then
    echo "❌ Ollama 服务未运行，请先执行: docker-compose up -d"
    exit 1
fi

echo "✅ Ollama 服务运行中"

# 等待 Ollama 完全启动
echo "⏳ 等待 Ollama 完全启动（约 10 秒）..."
sleep 10

# 检查 Ollama 健康状态
echo "🔍 检查 Ollama 健康状态..."
if curl -f http://localhost:11434/api/version > /dev/null 2>&1; then
    echo "✅ Ollama API 可访问"
else
    echo "⚠️  Ollama API 暂不可访问，再等待 10 秒..."
    sleep 10
fi

# 下载 Embedding 模型
echo ""
echo "📥 开始下载 Embedding 模型..."
echo "模型：nomic-embed-text"
echo "大小：约 270MB"
echo "预计时间：1-5 分钟（取决于网速）"
echo ""
echo "⏳ 下载中，请耐心等待..."
echo "----------------------------------------"

docker exec lingdang-ollama ollama pull nomic-embed-text

echo "----------------------------------------"
echo ""
echo "✅ 模型下载完成！"
echo ""

# 验证模型
echo "🧪 验证模型是否可用..."
docker exec lingdang-ollama ollama list

echo ""
echo "========================================"
echo "🎉 Ollama 初始化完成！"
echo ""
echo "📝 说明："
echo "  - 模型已下载到 Docker Volume：ollama-data"
echo "  - 后续重启/重新部署无需再次下载"
echo "  - 模型将持久保存，除非删除 Volume"
echo ""
echo "🚀 下一步："
echo "  1. 重启后端服务：docker-compose restart backend"
echo "  2. 登录 Studio 发布一篇文章测试索引"
echo "  3. 使用 AI 助手测试检索"
echo ""
echo "🔧 故障排查："
echo "  - 查看 Ollama 日志：docker-compose logs ollama"
echo "  - 查看后端日志：docker-compose logs backend"
echo "  - 重新下载模型：docker exec lingdang-ollama ollama pull nomic-embed-text"
echo "========================================"
