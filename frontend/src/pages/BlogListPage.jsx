import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../lib/api';
import { getArticleSummary } from '../lib/article';
import { getPageUrl, updateSeoTags } from '../lib/seo';

function BlogListPage() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const location = useLocation();
  const navigate = useNavigate();

  const params = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const q = (params.get('q') || '').trim();
  const page = Math.max(1, Number(params.get('page') || '1'));
  const pageSize = 12;

  useEffect(() => {
    if (q) {
      fetchSearch(q, page);
    } else {
      fetchArticles();
    }
  }, [q, page]);

  useEffect(() => {
    const title = '文章列表 - 铃铛师兄大模型';
    const description = '浏览铃铛师兄大模型博客最新文章，涵盖大模型、生成式AI、NLP、机器学习等内容。';
    updateSeoTags({
      title,
      description,
      type: 'website',
      url: getPageUrl(location.pathname)
    });
  }, [location.pathname]);

  const fetchArticles = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/articles`);
      const result = await response.json();
      if (isApiSuccess(result)) {
        setArticles(result.data);
        setTotal(result.data?.length || 0);
      }
    } catch (error) {
      console.error('获取文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchSearch = async (keyword, p) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/articles/search?q=${encodeURIComponent(keyword)}&page=${p}&pageSize=${pageSize}`);
      const result = await response.json();
      if (isApiSuccess(result)) {
        const data = result.data || {};
        setArticles(data.items || []);
        setTotal(Number(data.total || 0));
      }
    } catch (error) {
      console.error('搜索文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const value = (e.target.elements.q?.value || '').trim();
    if (!value) {
      navigate('/blog');
      return;
    }
    navigate(`/blog?q=${encodeURIComponent(value)}&page=1`);
  };

  const renderSummary = (article) => {
    if (article?.snippet) {
      return (
        <p
          className="summary is-snippet"
          dangerouslySetInnerHTML={{ __html: article.snippet }}
        />
      );
    }
    return <p className="summary">{getArticleSummary(article)}</p>;
  };

  if (loading) return <div className="loading">加载中</div>;

  return (
    <div className="blog-list-page">
      <h1>文章列表</h1>

      <form className="blog-search" onSubmit={handleSearchSubmit}>
        <input
          name="q"
          type="search"
          placeholder="搜索文章：标题 / 正文 / 标签"
          defaultValue={q}
        />
        <button type="submit" className="btn btn-primary">搜索</button>
        {q && (
          <button type="button" className="btn" onClick={() => navigate('/blog')}>清空</button>
        )}
      </form>

      {q && (
        <div className="blog-search-meta">
          关键词 “{q}” ，共 {total} 篇结果
        </div>
      )}

      {articles.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon" aria-hidden="true">🔎</div>
          <h1>{q ? '没有找到相关文章' : '暂无文章'}</h1>
          <p>{q ? '换个关键词试试，或清空搜索返回全部文章。' : '这里还没有发布的文章，先去首页看看吧。'}</p>
          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
            <Link to="/" className="btn btn-primary">返回首页</Link>
            {q && <button type="button" className="btn" onClick={() => navigate('/blog')}>清空搜索</button>}
          </div>
        </div>
      ) : (
        <>
          <div className="articles-grid">
            {articles.map(article => (
              <Link key={article.id} to={`/blog/${article.slug}`} className="article-card">
                <h2>{article.title}</h2>
                {renderSummary(article)}
                <div className="meta">
                  <span>{new Date(article.publishedAt).toLocaleDateString()}</span>
                  <span>{article.viewCount} 次浏览</span>
                </div>
              </Link>
            ))}
          </div>

          {q && (
            <div className="blog-pagination">
              <button
                type="button"
                className="btn"
                disabled={page <= 1}
                onClick={() => navigate(`/blog?q=${encodeURIComponent(q)}&page=${page - 1}`)}
              >
                上一页
              </button>
              <span>第 {page} 页</span>
              <button
                type="button"
                className="btn"
                disabled={(page * pageSize) >= total}
                onClick={() => navigate(`/blog?q=${encodeURIComponent(q)}&page=${page + 1}`)}
              >
                下一页
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default BlogListPage;
