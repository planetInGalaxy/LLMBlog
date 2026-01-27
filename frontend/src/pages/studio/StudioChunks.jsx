import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../../lib/api';
import { handleStudioWriteResponse } from '../../lib/studioApi';

function StudioChunks() {
  const [chunks, setChunks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [articleId, setArticleId] = useState('');
  const navigate = useNavigate();

  const fetchChunks = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    const params = new URLSearchParams();
    if (articleId) params.set('articleId', articleId);
    params.set('page', '1');
    params.set('pageSize', '50');

    try {
      const response = await fetch(`${API_URL}/studio/chunks?${params.toString()}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        setChunks(Array.isArray(result.data) ? result.data : []);
      } else {
        alert(result.message || '获取 chunks 失败');
      }
    } catch (e) {
      console.error(e);
      alert('获取 chunks 失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchChunks();
  }, []);

  return (
    <div className="studio-settings">
      <div className="studio-header">
        <h1>文章 Chunks 切片</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/studio/articles')}>返回文章管理</button>
          <button onClick={fetchChunks} disabled={loading}>刷新</button>
        </div>
      </div>

      <div className="settings-card">
        <div className="form-group">
          <label>按 articleId 过滤（可选）</label>
          <input
            type="number"
            value={articleId}
            onChange={(e) => setArticleId(e.target.value)}
            placeholder="例如 1"
            disabled={loading}
          />
          <div className="form-hint">默认展示最近写入的 50 条（按 createdAt 倒序）。</div>
          <button onClick={fetchChunks} disabled={loading} style={{ marginTop: '0.75rem' }}>
            {loading ? '加载中…' : '查询'}
          </button>
        </div>

        {loading ? (
          <div className="loading">加载中…</div>
        ) : chunks.length === 0 ? (
          <p className="form-hint">暂无 chunks 数据。先对文章执行索引/重建索引后再查看。</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="studio-table">
              <thead>
                <tr>
                  <th>articleId</th>
                  <th>seq</th>
                  <th>chunkId</th>
                  <th>indexVersion</th>
                  <th>heading</th>
                  <th>token</th>
                  <th>text</th>
                </tr>
              </thead>
              <tbody>
                {chunks.map((c) => (
                  <tr key={c.chunkId}>
                    <td>{c.articleId}</td>
                    <td>{c.sequenceNumber}</td>
                    <td title={c.chunkId}>{c.chunkId}</td>
                    <td>{c.indexVersion}</td>
                    <td title={c.headingText}>{c.headingText || '-'}</td>
                    <td>{c.tokenCount ?? '-'}</td>
                    <td style={{ maxWidth: 520, whiteSpace: 'pre-wrap' }}>
                      {String(c.chunkText || '').slice(0, 300)}{String(c.chunkText || '').length > 300 ? '…' : ''}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default StudioChunks;
