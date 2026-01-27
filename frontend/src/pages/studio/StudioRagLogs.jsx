import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../../lib/api';
import { handleStudioWriteResponse } from '../../lib/studioApi';

function StudioRagLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchLogs = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/rag-logs`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        setLogs(Array.isArray(result.data) ? result.data : []);
      } else {
        alert(result.message || '获取日志失败');
      }
    } catch (e) {
      console.error(e);
      alert('获取日志失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  return (
    <div className="studio-settings">
      <div className="studio-header">
        <h1>RAG 查询日志（近 7 天）</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/studio/settings')}>返回配置</button>
          <button onClick={fetchLogs} disabled={loading}>刷新</button>
        </div>
      </div>

      <div className="settings-card">
        {loading ? (
          <div className="loading">加载中…</div>
        ) : logs.length === 0 ? (
          <p className="form-hint">暂无数据。去 AI 助手问几个问题后再来看。</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="studio-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>问题</th>
                  <th>命中</th>
                  <th>topK</th>
                  <th>minScore</th>
                  <th>chunkSize</th>
                  <th>vec</th>
                  <th>bm25</th>
                  <th>引用</th>
                  <th>耗时(ms)</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((row) => (
                  <tr
                    key={row.requestId}
                    style={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/studio/rag-logs/${row.requestId}`)}
                  >
                    <td>{row.createdAt ? String(row.createdAt).replace('T', ' ').slice(0, 19) : '-'}</td>
                    <td title={row.question}>{String(row.question || '').slice(0, 40)}</td>
                    <td>{row.hasArticles ? '是' : '否'}</td>
                    <td>{row.topK ?? '-'}</td>
                    <td>{row.minScore ?? '-'}</td>
                    <td>{row.chunkSize ?? '-'}</td>
                    <td>{row.vectorCandidates ?? '-'}</td>
                    <td>{row.bm25Candidates ?? '-'}</td>
                    <td>{row.citationsCount ?? 0}</td>
                    <td>{row.latencyMs ?? '-'}</td>
                    <td style={{ color: row.success ? 'var(--success-color)' : 'var(--danger-color)' }}>
                      {row.success ? 'OK' : 'FAIL'}
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

export default StudioRagLogs;
