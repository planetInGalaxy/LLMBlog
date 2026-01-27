import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { API_URL, isApiSuccess } from '../lib/api';
import { getArticleDescription } from '../lib/article';
import { getPageUrl, updateSeoTags } from '../lib/seo';

function BlogDetailPage() {
  const { slug } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [articles, setArticles] = useState([]);
  const [showBackToTop, setShowBackToTop] = useState(false);
  const location = useLocation();

  useEffect(() => {
    fetchArticle();
    fetchArticles();
  }, [slug]);

  useEffect(() => {
    const url = getPageUrl(location.pathname);
    if (notFound) {
      updateSeoTags({
        title: '文章不存在 - 铃铛师兄大模型',
        description: '你访问的文章可能已下线或链接有误，请返回博客列表浏览。',
        type: 'website',
        url
      });
      return;
    }
    if (!article) return;
    updateSeoTags({
      title: `${article.title} - 铃铛师兄大模型`,
      description: getArticleDescription(article),
      type: 'article',
      url
    });
  }, [article, notFound, location.pathname]);

  useEffect(() => {
    const onScroll = () => {
      setShowBackToTop(window.scrollY > 480);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const { prevArticle, nextArticle } = useMemo(() => {
    if (!article || !Array.isArray(articles) || articles.length === 0) {
      return { prevArticle: null, nextArticle: null };
    }
    // /api/articles 当前返回的列表是按发布时间倒序（最新在前）
    const idx = articles.findIndex(a => String(a.slug) === String(article.slug));
    if (idx < 0) return { prevArticle: null, nextArticle: null };

    const newer = idx > 0 ? articles[idx - 1] : null;
    const older = idx < articles.length - 1 ? articles[idx + 1] : null;

    // 约定：上一条 = 列表中的上一条（通常是更新更近/更新更“新”）；下一条 = 更新更早
    return { prevArticle: newer, nextArticle: older };
  }, [article, articles]);

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const fetchArticles = async () => {
    try {
      const response = await fetch(`${API_URL}/articles`);
      const result = await response.json();
      if (isApiSuccess(result) && Array.isArray(result.data)) {
        setArticles(result.data);
      }
    } catch (error) {
      console.warn('获取文章列表失败（用于上一篇/下一篇）:', error);
    }
  };

  const fetchArticle = async () => {
    try {
      setLoading(true);
      setNotFound(false);
      setArticle(null);
      const response = await fetch(`${API_URL}/articles/${slug}`);
      if (response.status === 404) {
        setNotFound(true);
        return;
      }
      const result = await response.json();
      if (response.ok && isApiSuccess(result) && result.data) {
        setArticle(result.data);
      } else {
        setNotFound(true);
      }
    } catch (error) {
      console.error('获取文章失败:', error);
      setNotFound(true);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">加载中</div>;
  if (notFound) {
    return (
      <div className="blog-detail-page">
        <div className="empty-state empty-state--detail">
          <div className="empty-icon" aria-hidden="true">🔍</div>
          <h1>文章不存在</h1>
          <p>你访问的文章可能已被下线或链接有误。</p>
          <Link to="/blog" className="btn btn-primary">返回博客列表</Link>
        </div>
      </div>
    );
  }
  if (!article) return null;

  return (
    <div className="blog-detail-page">
      <article className="article-content">
        <header className="article-header">
          <h1>{article.title}</h1>
          <div className="article-meta">
            <span>作者：{article.author}</span>
            <span>{new Date(article.publishedAt).toLocaleDateString()}</span>
            <span>{article.viewCount} 次浏览</span>
          </div>
          {article.tags && (
            <div className="article-tags">
              {article.tags.split(',').map((tag, idx) => (
                <span key={idx} className="tag">{tag.trim()}</span>
              ))}
            </div>
          )}
        </header>

        <div className="markdown-body">
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            components={{
              code({ inline, className, children, ...props }) {
                const match = /language-(\w+)/.exec(className || '');
                return !inline && match ? (
                  <SyntaxHighlighter
                    style={vscDarkPlus}
                    language={match[1]}
                    PreTag="div"
                    {...props}
                  >
                    {String(children).replace(/\n$/, '')}
                  </SyntaxHighlighter>
                ) : (
                  <code className={className} {...props}>
                    {children}
                  </code>
                );
              }
            }}
          >
            {article.contentMarkdown}
          </ReactMarkdown>
        </div>

        <footer className="article-footer">
          <div className="article-actions">
            <Link to="/blog" className="article-action-link">← 返回文章列表</Link>
          </div>

          {(prevArticle || nextArticle) && (
            <nav className="article-nav" aria-label="上一篇/下一篇">
              {prevArticle ? (
                <Link className="article-nav-card" to={`/blog/${prevArticle.slug}`}>
                  <div className="article-nav-label">上一篇</div>
                  <div className="article-nav-title">{prevArticle.title}</div>
                </Link>
              ) : (
                <div className="article-nav-card article-nav-card--empty" />
              )}

              {nextArticle ? (
                <Link className="article-nav-card" to={`/blog/${nextArticle.slug}`}>
                  <div className="article-nav-label">下一篇</div>
                  <div className="article-nav-title">{nextArticle.title}</div>
                </Link>
              ) : (
                <div className="article-nav-card article-nav-card--empty" />
              )}
            </nav>
          )}
        </footer>
      </article>

      <button
        type="button"
        className={`back-to-top ${showBackToTop ? 'back-to-top--visible' : ''}`}
        onClick={scrollToTop}
        aria-label="返回顶部"
        title="返回顶部"
      >
        ↑
      </button>
    </div>
  );
}

export default BlogDetailPage;
