import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../../lib/api';
import { handleStudioWriteResponse } from '../../lib/studioApi';

function StudioRagSettings() {
  const [form, setForm] = useState({
    topK: '5',
    minScore: '0',
    chunkSize: '900',
    returnCitations: true
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  const fetchConfig = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/rag-config`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        const data = result.data || {};
        setForm({
          topK: String(data.topK ?? 5),
          minScore: String(data.minScore ?? 0),
          chunkSize: String(data.chunkSize ?? 900),
          returnCitations: data.returnCitations !== false
        });
      } else {
        alert(result.message || '获取配置失败');
      }
    } catch (error) {
      console.error('获取配置失败:', error);
      alert('获取配置失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfig();
  }, []);

  const parseNumber = (value, fallback) => {
    if (value === '' || value === null || value === undefined) return fallback;
    const num = Number(value);
    return Number.isFinite(num) ? num : fallback;
  };

  const handleSave = async () => {
    const topK = parseNumber(form.topK, 5);
    const minScore = parseNumber(form.minScore, 0);

    if (topK < 1 || topK > 50) {
      alert('topK 需在 1 ~ 50 之间');
      return;
    }
    if (minScore < 0 || minScore > 1) {
      alert('minScore 需在 0 ~ 1 之间');
      return;
    }

    setSaving(true);
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/rag-config`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          topK,
          minScore,
          returnCitations: !!form.returnCitations
        })
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        const data = result.data || {};
        setForm(prev => ({
          ...prev,
          topK: String(data.topK ?? topK),
          minScore: String(data.minScore ?? minScore),
          returnCitations: data.returnCitations !== false,
          chunkSize: String(data.chunkSize ?? prev.chunkSize)
        }));
        alert('保存成功！');
      } else {
        alert(result.message || '保存失败');
      }
    } catch (error) {
      console.error('保存配置失败:', error);
      alert('保存失败，请稍后重试');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading">加载中</div>;

  return (
    <div className="studio-settings">
      <div className="studio-header">
        <h1>RAG 配置</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/studio/articles')}>返回文章管理</button>
        </div>
      </div>

      <div className="settings-card">
        <div className="form-group">
          <label>topK</label>
          <input
            type="number"
            min="1"
            max="50"
            step="1"
            value={form.topK}
            onChange={(e) => setForm(prev => ({ ...prev, topK: e.target.value }))}
          />
        </div>
        <div className="form-group">
          <label>minScore（0 ~ 1，可选）</label>
          <input
            type="number"
            min="0"
            max="1"
            step="0.01"
            value={form.minScore}
            onChange={(e) => setForm(prev => ({ ...prev, minScore: e.target.value }))}
          />
        </div>
        <div className="form-group">
          <label>
            chunkSize
            <span className="form-hint">（修改后将自动触发全量重建索引，成功后才会生效）</span>
          </label>
          <input
            type="number"
            min="200"
            max="2000"
            step="10"
            value={form.chunkSize}
            onChange={(e) => setForm(prev => ({ ...prev, chunkSize: e.target.value }))}
            disabled={saving}
          />
          <div className="form-hint">本次保存会自动全量重建索引，可能需要几十秒～数分钟，请耐心等待。</div>
        </div>
        <div className="form-group form-toggle">
          <label>
            <input
              type="checkbox"
              checked={!!form.returnCitations}
              onChange={(e) => setForm(prev => ({ ...prev, returnCitations: e.target.checked }))}
            />
            返回引用
          </label>
        </div>
        <div className="form-actions">
          <button onClick={handleSave} disabled={saving}>
            {saving ? '保存中…' : '保存配置'}
          </button>
          <button onClick={() => navigate('/studio/articles')}>取消</button>
        </div>
      </div>
    </div>
  );
}

export default StudioRagSettings;
