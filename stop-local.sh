#!/bin/bash

echo "=================================="
echo "铃铛师兄大模型博客系统 - 停止脚本"
echo "=================================="

# 停止后端
if [ -f .backend.pid ]; then
    BACKEND_PID=$(cat .backend.pid)
    echo "停止后端服务 (PID: $BACKEND_PID)..."
    kill $BACKEND_PID 2>/dev/null
    rm -f .backend.pid
    echo "✅ 后端服务已停止"
else
    echo "未找到后端服务 PID 文件"
fi

# 停止前端
if [ -f .frontend.pid ]; then
    FRONTEND_PID=$(cat .frontend.pid)
    echo "停止前端服务 (PID: $FRONTEND_PID)..."
    kill $FRONTEND_PID 2>/dev/null
    rm -f .frontend.pid
    echo "✅ 前端服务已停止"
else
    echo "未找到前端服务 PID 文件"
fi

# 清理可能残留的 Java 和 Node 进程
echo "清理残留进程..."
pkill -f "blog-backend-1.0.0.jar" 2>/dev/null
pkill -f "vite" 2>/dev/null

echo ""
echo "✅ 所有服务已停止"
