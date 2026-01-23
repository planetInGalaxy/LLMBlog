#!/bin/bash

# ES 索引修复脚本
# 用于修复 Elasticsearch 索引问题

set -e

echo "========================================="
echo "🔧 Elasticsearch 索引修复脚本"
echo "========================================="

# 检查 ES 容器是否运行
if ! docker ps | grep -q lingdang-es; then
    echo "❌ Elasticsearch 容器未运行"
    exit 1
fi

echo "✅ Elasticsearch 容器正在运行"

# 等待 ES 启动
echo "⏳ 等待 Elasticsearch 完全启动..."
sleep 5

# 检查 ES 健康状态
echo "🔍 检查 Elasticsearch 健康状态..."
docker exec lingdang-es curl -s http://localhost:9200/_cluster/health || {
    echo "❌ Elasticsearch 未响应"
    exit 1
}

echo ""
echo "========================================="
echo "📊 当前索引状态"
echo "========================================="

# 检查索引是否存在
INDEX_EXISTS=$(docker exec lingdang-es curl -s -o /dev/null -w "%{http_code}" http://localhost:9200/lingdang_chunks_v1)

if [ "$INDEX_EXISTS" = "200" ]; then
    echo "✅ 索引 lingdang_chunks_v1 已存在"
    echo ""
    echo "📈 索引统计信息:"
    docker exec lingdang-es curl -s http://localhost:9200/lingdang_chunks_v1/_count | jq
    echo ""
    echo "🔧 是否要删除并重建索引？(y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "跳过重建索引"
        exit 0
    fi
    
    echo "🗑️  删除旧索引..."
    docker exec lingdang-es curl -X DELETE http://localhost:9200/lingdang_chunks_v1
    docker exec lingdang-es curl -X DELETE http://localhost:9200/lingdang_chunks_current 2>/dev/null || true
    echo "✅ 旧索引已删除"
else
    echo "⚠️  索引 lingdang_chunks_v1 不存在，将创建新索引"
fi

echo ""
echo "========================================="
echo "🔨 创建新索引"
echo "========================================="

# 读取索引配置
CHUNK_SETTINGS=$(cat <<'EOF'
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "ik_max_word_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "chunk_id": { "type": "keyword" },
      "article_id": { "type": "long" },
      "slug": { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "tags": { "type": "keyword" },
      "status": { "type": "keyword" },
      "index_version": { "type": "integer" },
      "anchor": { "type": "keyword" },
      "chunk_text": {
        "type": "text",
        "analyzer": "ik_max_word_analyzer"
      },
      "embedding": {
        "type": "dense_vector",
        "dims": 768,
        "index": true,
        "similarity": "cosine"
      }
    }
  }
}
EOF
)

# 创建索引
echo "$CHUNK_SETTINGS" | docker exec -i lingdang-es curl -X PUT -H "Content-Type: application/json" \
    http://localhost:9200/lingdang_chunks_v1 -d @- || {
    echo "❌ 索引创建失败"
    exit 1
}

echo ""
echo "✅ 索引 lingdang_chunks_v1 创建成功"

# 创建别名
echo "🔗 创建索引别名..."
docker exec lingdang-es curl -X POST -H "Content-Type: application/json" \
    http://localhost:9200/_aliases -d '{
  "actions": [
    {
      "add": {
        "index": "lingdang_chunks_v1",
        "alias": "lingdang_chunks_current"
      }
    }
  ]
}' || {
    echo "⚠️  别名创建失败（可能已存在）"
}

echo ""
echo "✅ 别名 lingdang_chunks_current 创建成功"

echo ""
echo "========================================="
echo "📊 验证索引"
echo "========================================="

# 验证索引
docker exec lingdang-es curl -s http://localhost:9200/lingdang_chunks_v1/_mapping | jq

echo ""
echo "========================================="
echo "🔄 重启后端服务"
echo "========================================="

echo "重启后端以重新初始化索引..."
docker restart lingdang-backend

echo "⏳ 等待后端启动..."
sleep 10

# 检查后端健康
for i in {1..30}; do
    if docker exec lingdang-backend curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        echo "✅ 后端服务启动成功"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "⚠️  后端启动超时，请检查日志: docker logs lingdang-backend"
    fi
    sleep 2
done

echo ""
echo "========================================="
echo "✅ 索引修复完成！"
echo "========================================="
echo ""
echo "📋 下一步操作："
echo "1. 登录 Studio: http://your-server/studio/login"
echo "2. 进入文章管理"
echo "3. 点击「全量重建索引」按钮"
echo ""
echo "或者使用 API："
echo "curl -X POST -H 'Authorization: Bearer YOUR_TOKEN' http://your-server/api/studio/reindex-all"
echo ""
