import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../../lib/api';
import { handleStudioWriteResponse } from '../../lib/studioApi';

function StudioPrompts() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingKey, setSavingKey] = useState(null);
  const navigate = useNavigate();

  const fetchPrompts = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const resp = await fetch(`${API_URL}/studio/prompts`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await handleStudioWriteResponse(resp, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        setItems(result.data || []);
      } else {
        alert(result.message || '获取提示词失败');
      }
    } catch (e) {
      console.error(e);
      alert('获取提示词失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPrompts();
  }, []);

  const updateItem = (key, patch) => {
    setItems(prev => prev.map(it => (it.promptKey === key ? { ...it, ...patch } : it)));
  };

  const handleSave = async (it) => {
    setSavingKey(it.promptKey);
    const token = localStorage.getItem('token');
    try {
      const resp = await fetch(`${API_URL}/studio/prompts`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(it)
      });
      const result = await handleStudioWriteResponse(resp, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        alert('保存成功');
        await fetchPrompts();
      } else {
        alert(result.message || '保存失败');
      }
    } catch (e) {
      console.error(e);
      alert('保存失败');
    } finally {
      setSavingKey(null);
    }
  };

  if (loading) return <div className="loading">加载中</div>;

  return (
    <div className="studio-settings">
      <div className="studio-header">
        <h1>提示词配置</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/studio/settings')}>返回 RAG 配置</button>
          <button onClick={() => navigate('/studio/articles/new')} className="btn-info">新建文章</button>
        </div>
      </div>

      <div className="settings-card">
        <div className="form-hint" style={{ marginBottom: '1rem' }}>
          这些提示词会影响：意图识别、问候/无关问题回复、以及 RAG/灵活模式下的最终回答。
          修改后会立刻生效（无需重启）。
        </div>

        {items.map((it) => (
          <div key={it.promptKey} className="prompt-card">
            <div className="prompt-title">{it.name}</div>
            <div className="prompt-key">{it.promptKey}</div>
            {it.description ? <div className="prompt-desc">{it.description}</div> : null}
            <textarea
              value={it.content || ''}
              onChange={(e) => updateItem(it.promptKey, { content: e.target.value })}
              rows={10}
            />
            <div className="form-actions" style={{ marginTop: '1rem' }}>
              <button
                onClick={() => handleSave(it)}
                disabled={savingKey === it.promptKey}
              >
                {savingKey === it.promptKey ? '保存中…' : '保存'}
              </button>
              <button onClick={fetchPrompts} disabled={savingKey === it.promptKey}>重载</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default StudioPrompts;
