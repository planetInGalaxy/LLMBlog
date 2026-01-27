import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../lib/api';
import { getArticleSummary } from '../lib/article';
import { getPageUrl, updateSeoTags } from '../lib/seo';

function BlogListPage() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const location = useLocation();

  useEffect(() => {
    fetchArticles();
  }, []);

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
    try {
      const response = await fetch(`${API_URL}/articles`);
      const result = await response.json();
      if (isApiSuccess(result)) {
        setArticles(result.data);
      }
    } catch (error) {
      console.error('获取文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">加载中</div>;
  if (articles.length === 0) {
    return (
      <div className="blog-list-page">
        <div className="empty-state">
          <div className="empty-icon" aria-hidden="true">📭</div>
          <h1>暂无文章</h1>
          <p>这里还没有发布的文章，先去首页看看吧。</p>
          <Link to="/" className="btn btn-primary">返回首页</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="blog-list-page">
      <h1>文章列表</h1>
      <div className="articles-grid">
        {articles.map(article => (
          <Link key={article.id} to={`/blog/${article.slug}`} className="article-card">
            <h2>{article.title}</h2>
            <p className="summary">{getArticleSummary(article)}</p>
            <div className="meta">
              <span>{new Date(article.publishedAt).toLocaleDateString()}</span>
              <span>{article.viewCount} 次浏览</span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

export default BlogListPage;
