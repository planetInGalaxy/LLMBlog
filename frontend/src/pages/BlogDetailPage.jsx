import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { API_URL } from '../lib/api';
import { getArticleDescription } from '../lib/article';
import { getPageUrl, updateSeoTags } from '../lib/seo';

function BlogDetailPage() {
  const { slug } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const location = useLocation();

  useEffect(() => {
    fetchArticle();
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
      if (response.ok && result.success && result.data) {
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
      </article>
    </div>
  );
}

export default BlogDetailPage;
