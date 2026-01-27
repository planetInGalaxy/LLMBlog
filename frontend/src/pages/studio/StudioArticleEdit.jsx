import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../../lib/api';
import { handleStudioWriteResponse } from '../../lib/studioApi';

function StudioArticleEdit() {
  const { id } = useParams();
  const [article, setArticle] = useState({
    title: '',
    slug: '',
    summary: '',
    contentMarkdown: '',
    tags: '',
    author: '铃铛师兄'
  });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // 如果是编辑模式，加载文章数据
  useEffect(() => {
    if (id) {
      fetchArticle();
    }
  }, [id]);

  const fetchArticle = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const result = await response.json();
      if (isApiSuccess(result)) {
        const data = result.data;
        setArticle({
          title: data.title || '',
          slug: data.slug || '',
          summary: data.summary || '',
          contentMarkdown: data.contentMarkdown || '',
          tags: data.tags || '',
          author: data.author || '铃铛师兄'
        });
      }
    } catch (error) {
      console.error('获取文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    const token = localStorage.getItem('token');
    try {
      const url = id ? `${API_URL}/studio/articles/${id}` : `${API_URL}/studio/articles`;
      const method = id ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(article)
      });
      const result = await handleStudioWriteResponse(response, navigate);
      if (!result) return;
      if (isApiSuccess(result)) {
        alert('保存成功！');
        navigate('/studio/articles');
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('保存失败:', error);
    }
  };

  if (loading) return <div className="loading">加载中</div>;

  return (
    <div className="studio-article-edit">
      <h1>编辑文章</h1>
      <div className="form-group">
        <label>标题</label>
        <input
          type="text"
          value={article.title}
          onChange={(e) => setArticle({...article, title: e.target.value})}
        />
      </div>
      <div className="form-group">
        <label>Slug</label>
        <input
          type="text"
          value={article.slug}
          onChange={(e) => setArticle({...article, slug: e.target.value})}
        />
      </div>
      <div className="form-group">
        <label>摘要</label>
        <textarea
          value={article.summary}
          onChange={(e) => setArticle({...article, summary: e.target.value})}
          rows="3"
        />
      </div>
      <div className="form-group">
        <label>Markdown 内容</label>
        <textarea
          value={article.contentMarkdown}
          onChange={(e) => setArticle({...article, contentMarkdown: e.target.value})}
          rows="20"
          style={{fontFamily: 'monospace'}}
        />
      </div>
      <div className="form-group">
        <label>标签（逗号分隔）</label>
        <input
          type="text"
          value={article.tags}
          onChange={(e) => setArticle({...article, tags: e.target.value})}
        />
      </div>
      <div className="form-actions">
        <button onClick={handleSave}>保存草稿</button>
        <button onClick={() => navigate('/studio/articles')}>取消</button>
      </div>
    </div>
  );
}

export default StudioArticleEdit;
