import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../../lib/api';
import { handleStudioWriteResponse } from '../../lib/studioApi';

function StudioArticleList() {
  const [articles, setArticles] = useState([]);
  const [indexHealth, setIndexHealth] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchArticles();
    fetchIndexHealth();
  }, []);

  const fetchArticles = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (isApiSuccess(result)) {
        setArticles(result.data);
      } else if (response.status === 401) {
        navigate('/studio/login');
      }
    } catch (error) {
      console.error('获取文章失败:', error);
    }
  };

  const fetchIndexHealth = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/index-health`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (isApiSuccess(result)) {
        setIndexHealth(result.data);
      }
    } catch (error) {
      console.error('获取索引健康状态失败:', error);
    }
  };

  const handlePublish = async (id) => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}/publish`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        alert('发布成功！索引任务已提交');
        fetchArticles();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('发布失败:', error);
    }
  };

  const handleOffline = async (id) => {
    if (!confirm('确定要下线这篇文章吗？')) return;

    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}/offline`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        alert('下线成功！');
        fetchArticles();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('下线失败:', error);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('确定要删除这篇文章吗？此操作不可恢复！')) return;

    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        alert('删除成功！');
        fetchArticles();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('删除失败:', error);
    }
  };

  const handleReindex = async (id) => {
    if (!confirm('确定要重新索引这篇文章吗？')) return;

    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}/reindex`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        alert('索引任务已提交！');
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('重新索引失败:', error);
    }
  };

  const handleReindexAll = async () => {
    if (!confirm('确定要对所有已发布文章重建索引吗？这可能需要一些时间。')) return;

    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/reindex-all`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        alert(result.message || '索引任务已全部提交！');
        fetchIndexHealth();
      } else {
        alert(result.message || '全量重建索引失败，请稍后重试');
      }
    } catch (error) {
      console.error('全量重建索引失败:', error);
      alert('全量重建索引失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="studio-article-list">
      <div className="studio-header">
        <h1>文章管理</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/studio/articles/new')}>新建文章</button>
          <button onClick={() => navigate('/studio/settings')} className="btn-info">RAG 配置</button>
          <button
            onClick={handleReindexAll}
            disabled={loading}
            className="btn-warning"
          >
            {loading ? '重建中…' : '🔄 全量重建索引'}
          </button>
        </div>
      </div>

      {/* 索引健康状态卡片 */}
      {indexHealth && (
        <div className={`index-health-card ${indexHealth.healthy ? 'healthy' : 'unhealthy'}`}>
          <h3>📊 索引健康状态</h3>
          <div className="health-info">
            <div className="health-item">
              <span>ES 连接：</span>
              <strong>{indexHealth.esConnected ? '✅ 正常' : '❌ 失败'}</strong>
            </div>
            <div className="health-item">
              <span>索引存在：</span>
              <strong>{indexHealth.indexExists ? '✅ 是' : '❌ 否'}</strong>
            </div>
            <div className="health-item">
              <span>文章数量：</span>
              <strong>{indexHealth.articleCount || 0}</strong>
            </div>
            <div className="health-item">
              <span>文档数量（chunks）：</span>
              <strong>{indexHealth.documentCount}</strong>
            </div>
            <div className="health-item">
              <span>状态：</span>
              <strong className={indexHealth.healthy ? 'text-success' : 'text-error'}>
                {indexHealth.message}
              </strong>
            </div>
          </div>
          {(!indexHealth.esConnected || !indexHealth.indexExists) && (
            <div className="health-diagnosis">
              <h4>诊断建议</h4>
              <ul>
                <li>检查 docker-compose 中的 Elasticsearch 容器是否正常运行</li>
                <li>确认后端 ES 地址/账号配置是否正确</li>
                <li>尝试运行项目根目录的 `fix-es-index.sh` 进行修复</li>
              </ul>
            </div>
          )}
        </div>
      )}

      <table className="article-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {articles.map(article => (
            <tr key={article.id}>
              <td>{article.title}</td>
              <td><span className={`status status-${article.status.toLowerCase()}`}>{article.status}</span></td>
              <td>{new Date(article.updatedAt).toLocaleString()}</td>
              <td>
                <button onClick={() => navigate(`/studio/articles/${article.id}/edit`)}>编辑</button>
                {article.status === 'DRAFT' && (
                  <button onClick={() => handlePublish(article.id)} className="btn-success">发布</button>
                )}
                {article.status === 'PUBLISHED' && (
                  <>
                    <button onClick={() => handleReindex(article.id)} className="btn-info">重新索引</button>
                    <button onClick={() => handleOffline(article.id)} className="btn-warning">下线</button>
                  </>
                )}
                <button onClick={() => handleDelete(article.id)} className="btn-danger">删除</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default StudioArticleList;
