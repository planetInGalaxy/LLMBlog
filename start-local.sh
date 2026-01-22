#!/bin/bash

echo "=================================="
echo "铃铛师兄大模型博客系统 - 本地启动脚本"
echo "=================================="

# 检查 Java 版本
echo "检查 Java 环境..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    echo "检测到 Java 版本: $java_version"
    if [ "$java_version" -lt 17 ]; then
        echo "❌ 错误: 需要 Java 17 或更高版本"
        echo "请运行: brew install openjdk@17"
        exit 1
    fi
else
    echo "❌ 错误: 未检测到 Java"
    echo "请运行: brew install openjdk@17"
    exit 1
fi

# 检查 Node.js
echo "检查 Node.js 环境..."
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 未检测到 Node.js"
    echo "请运行: brew install node"
    exit 1
fi
echo "检测到 Node.js 版本: $(node -v)"

# 检查 Maven
echo "检查 Maven 环境..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: 未检测到 Maven"
    echo "请运行: brew install maven"
    exit 1
fi
echo "检测到 Maven 版本: $(mvn -v | head -n 1)"

echo ""
echo "✅ 环境检查通过！"
echo ""

# 启动后端
echo "=================================="
echo "1. 启动后端服务..."
echo "=================================="
cd backend
echo "构建后端项目..."
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ 后端构建成功！"
    echo "启动后端服务..."
    java -jar target/blog-backend-1.0.0.jar &
    BACKEND_PID=$!
    echo "后端服务 PID: $BACKEND_PID"
else
    echo "❌ 后端构建失败！"
    exit 1
fi

cd ..

# 等待后端启动
echo "等待后端服务启动..."
sleep 10

# 启动前端
echo ""
echo "=================================="
echo "2. 启动前端服务..."
echo "=================================="
cd frontend

if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi

echo "启动前端开发服务器..."
npm run dev &
FRONTEND_PID=$!
echo "前端服务 PID: $FRONTEND_PID"

cd ..

echo ""
echo "=================================="
echo "✅ 启动完成！"
echo "=================================="
echo "前端地址: http://localhost:3000"
echo "后端地址: http://localhost:8080"
echo "API文档: http://localhost:8080/api/posts"
echo ""
echo "按 Ctrl+C 停止所有服务"
echo ""

# 保存 PID 到文件
echo $BACKEND_PID > .backend.pid
echo $FRONTEND_PID > .frontend.pid

# 等待用户中断
trap "echo '停止服务...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; rm -f .backend.pid .frontend.pid; exit" INT

wait
