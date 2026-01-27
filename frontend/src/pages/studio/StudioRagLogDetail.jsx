import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../../lib/api';
import { handleStudioWriteResponse } from '../../lib/studioApi';

function StudioRagLogDetail() {
  const { requestId } = useParams();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const fetchDetail = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/rag-logs/${requestId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        setData(result.data);
      } else {
        alert(result.message || '获取详情失败');
      }
    } catch (e) {
      console.error(e);
      alert('获取详情失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [requestId]);

  const log = data?.log;
  const hits = Array.isArray(data?.hits) ? data.hits : [];

  return (
    <div className="studio-settings">
      <div className="studio-header">
        <h1>RAG 日志详情</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/studio/rag-logs')}>返回列表</button>
          <button onClick={fetchDetail} disabled={loading}>刷新</button>
        </div>
      </div>

      <div className="settings-card">
        {loading ? (
          <div className="loading">加载中…</div>
        ) : !log ? (
          <p className="form-hint">未找到该 requestId 的日志。</p>
        ) : (
          <>
            <div className="form-hint">requestId: {requestId}</div>
            <h3 style={{ marginTop: '0.75rem' }}>问题</h3>
            <p style={{ whiteSpace: 'pre-wrap' }}>{log.question}</p>

            <h3>配置快照</h3>
            <ul>
              <li>topK: {log.topK ?? '-'}</li>
              <li>minScore: {log.minScore ?? '-'}</li>
              <li>chunkSize: {log.chunkSize ?? '-'}</li>
              <li>vectorWeight: {log.vectorWeight ?? '-'}</li>
              <li>bm25Weight: {log.bm25Weight ?? '-'}</li>
              <li>bm25Max: {log.bm25Max ?? '-'}</li>
              <li>returnCitations: {String(!!log.returnCitations)}</li>
            </ul>

            <h3>指标</h3>
            <ul>
              <li>hasArticles: {String(!!log.hasArticles)}</li>
              <li>vectorCandidates: {log.vectorCandidates ?? '-'}</li>
              <li>bm25Candidates: {log.bm25Candidates ?? '-'}</li>
              <li>filteredCandidates: {log.filteredCandidates ?? '-'}</li>
              <li>hitArticleIds: {log.hitArticleIds || '-'}</li>
              <li>citationsCount: {log.citationsCount ?? 0}</li>
              <li>retrievalMs: {log.retrievalMs ?? '-'}</li>
              <li>latencyMs: {log.latencyMs ?? '-'}</li>
              <li>success: {String(!!log.success)}</li>
              {!log.success && <li style={{ color: 'var(--danger-color)' }}>error: {log.errorMessage}</li>}
            </ul>

            <h3>命中 chunks（Top {hits.length}）</h3>
            {hits.length === 0 ? (
              <p className="form-hint">无命中记录。</p>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table className="studio-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>title</th>
                      <th>chunkId</th>
                      <th>score(final)</th>
                      <th>score(vec)</th>
                      <th>score(bm25)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {hits.map((h) => (
                      <tr key={`${h.requestId}-${h.rankNo}-${h.chunkId}`}>
                        <td>{h.rankNo}</td>
                        <td title={h.title}>{String(h.title || '').slice(0, 50)}</td>
                        <td title={h.chunkId}>{h.chunkId}</td>
                        <td>{h.finalScore?.toFixed?.(4) ?? h.finalScore ?? '-'}</td>
                        <td>{h.vectorScore?.toFixed?.(4) ?? h.vectorScore ?? '-'}</td>
                        <td>{h.bm25Score?.toFixed?.(4) ?? h.bm25Score ?? '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default StudioRagLogDetail;
