import { Routes, Route, Link, useNavigate, useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import './App.css';

// API 配置
const API_URL = import.meta.env.VITE_API_URL || '/api';

// ==================== 主页 ====================
function HomePage() {
  return (
    <div className="home-page">
      <div className="hero-section">
        <h1>🔔 铃铛师兄大模型博客</h1>
        <p>专注 AI 技术分享 + 智能学习助手</p>
        <div className="hero-buttons">
          <Link to="/blog" className="btn btn-primary">浏览文章</Link>
          <Link to="/assistant" className="btn btn-secondary">AI 助手</Link>
        </div>
      </div>

      {/* 关于介绍区 */}
      <section className="about-section">
        <div className="section-header">
          <h2>关于铃铛师兄大模型</h2>
        </div>
        <div className="about-content">
          <div className="about-card">
            <h3>💡 专注领域</h3>
            <p>
              <strong>铃铛师兄大模型</strong>是一个专注于人工智能和大模型技术的专业博客平台。
              我们致力于分享最新的AI技术动态、大模型应用实践、机器学习算法解析以及行业前沿见解。
            </p>
          </div>
          
          <div className="about-card">
            <h3>🎯 内容覆盖</h3>
            <p>
              在这里，您可以找到关于<strong>大语言模型（LLM）</strong>、<strong>生成式AI</strong>、
              <strong>自然语言处理（NLP）</strong>、<strong>计算机视觉</strong>、<strong>深度学习</strong>等领域的深度文章和技术教程。
              我们不仅关注理论研究，更注重实际应用和工程实践。
            </p>
          </div>
          
          <div className="about-card">
            <h3>🚀 我们的使命</h3>
            <p>
              铃铛师兄大模型博客致力于成为AI技术爱好者和从业者的知识分享平台，
              通过高质量的技术内容，推动AI技术在中国的发展和应用。
              无论您是AI初学者还是资深工程师，都能在这里找到有价值的内容。
            </p>
          </div>
        </div>
        
        <div className="keywords-section">
          <strong>核心关键词：</strong>
          <div className="keyword-tags">
            <span className="keyword-tag">大模型</span>
            <span className="keyword-tag">AI技术</span>
            <span className="keyword-tag">人工智能</span>
            <span className="keyword-tag">机器学习</span>
            <span className="keyword-tag">深度学习</span>
            <span className="keyword-tag">自然语言处理</span>
            <span className="keyword-tag">生成式AI</span>
            <span className="keyword-tag">技术博客</span>
          </div>
        </div>
      </section>

      {/* 联系方式区 */}
      <section className="contact-section">
        <h3>📱 联系我们</h3>
        <p className="contact-highlight">请小红书搜索：<strong>铃铛师兄大模型求职辅导</strong>，获取更多干货</p>
      </section>
    </div>
  );
}

// ==================== 文章列表页 ====================
function BlogListPage() {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchArticles();
  }, []);

  const fetchArticles = async () => {
    try {
      const response = await fetch(`${API_URL}/articles`);
      const result = await response.json();
      if (result.success) {
        setArticles(result.data);
      }
    } catch (error) {
      console.error('获取文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">加载中</div>;

  return (
    <div className="blog-list-page">
      <h1>文章列表</h1>
      <div className="articles-grid">
        {articles.map(article => (
          <Link key={article.id} to={`/blog/${article.slug}`} className="article-card">
            <h2>{article.title}</h2>
            <p className="summary">{article.summary}</p>
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

// ==================== 文章详情页 ====================
function BlogDetailPage() {
  const { slug } = useParams();
  const [article, setArticle] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchArticle();
  }, [slug]);

  const fetchArticle = async () => {
    try {
      const response = await fetch(`${API_URL}/articles/${slug}`);
      const result = await response.json();
      if (result.success) {
        setArticle(result.data);
      }
    } catch (error) {
      console.error('获取文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">加载中</div>;
  if (!article) return <div className="error">文章不存在</div>;

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
              code({ node, inline, className, children, ...props }) {
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

// ==================== Assistant 页面 ====================
function AssistantPage() {
  const [question, setQuestion] = useState('');
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/assistant/query`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question, mode: 'ARTICLE_ONLY' })
      });
      const result = await res.json();
      if (result.success) {
        setResponse(result.data);
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('查询失败:', error);
      alert('查询失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="assistant-page">
      <h1>🤖 AI 学习助手</h1>
      <p>基于已发布文章库回答你的问题</p>
      
      <form onSubmit={handleSubmit} className="query-form">
        <textarea
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="请输入你的问题..."
          rows="4"
          disabled={loading}
        />
        <button type="submit" disabled={loading || !question.trim()}>
          {loading ? '查询中...' : '提问'}
        </button>
      </form>

      {response && (
        <div className="response-section">
          <h2>回答</h2>
          <div className="answer">{response.answer}</div>
          
          {response.citations && response.citations.length > 0 && (
            <div className="citations">
              <h3>参考文章</h3>
              {response.citations.map((citation, idx) => (
                <div key={idx} className="citation-item">
                  <a href={citation.url} target="_blank" rel="noopener noreferrer">
                    [{idx + 1}] {citation.title}
                  </a>
                  <p className="quote">{citation.quote}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ==================== Studio 登录页 ====================
function StudioLogin() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      const response = await fetch(`${API_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const result = await response.json();
      
      if (result.success) {
        localStorage.setItem('token', result.data.token);
        localStorage.setItem('username', result.data.username);
        navigate('/studio/articles');
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('登录失败:', error);
      alert('登录失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="studio-login">
      <div className="login-box">
        <h1>Studio 管理后台</h1>
        <form onSubmit={handleLogin}>
          <input
            type="text"
            placeholder="用户名"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={loading}
          />
          <input
            type="password"
            placeholder="密码"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading}
          />
          <button type="submit" disabled={loading}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </div>
  );
}

// ==================== Studio 文章列表 ====================
function StudioArticleList() {
  const [articles, setArticles] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    fetchArticles();
  }, []);

  const fetchArticles = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (result.success) {
        setArticles(result.data);
      } else if (response.status === 401) {
        navigate('/studio/login');
      }
    } catch (error) {
      console.error('获取文章失败:', error);
    }
  };

  const handlePublish = async (id) => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}/publish`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (result.success) {
        alert('发布成功！');
        fetchArticles();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('发布失败:', error);
    }
  };

  return (
    <div className="studio-article-list">
      <div className="studio-header">
        <h1>文章管理</h1>
        <button onClick={() => navigate('/studio/articles/new')}>新建文章</button>
      </div>
      
      <table className="article-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {articles.map(article => (
            <tr key={article.id}>
              <td>{article.title}</td>
              <td><span className={`status status-${article.status.toLowerCase()}`}>{article.status}</span></td>
              <td>{new Date(article.updatedAt).toLocaleString()}</td>
              <td>
                <button onClick={() => navigate(`/studio/articles/${article.id}/edit`)}>编辑</button>
                {article.status === 'DRAFT' && (
                  <button onClick={() => handlePublish(article.id)}>发布</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ==================== Studio 文章编辑 ====================
function StudioArticleEdit() {
  const [article, setArticle] = useState({
    title: '',
    slug: '',
    summary: '',
    contentMarkdown: '',
    tags: '',
    author: '铃铛师兄'
  });
  const navigate = useNavigate();

  const handleSave = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(article)
      });
      const result = await response.json();
      if (result.success) {
        alert('保存成功！');
        navigate('/studio/articles');
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('保存失败:', error);
    }
  };

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

// ==================== 主应用 ====================
function App() {
  return (
    <div className="app">
      <header className="header">
        <div className="container">
          <Link to="/" className="logo">🔔 铃铛师兄大模型</Link>
          <nav>
            <Link to="/">首页</Link>
            <Link to="/blog">博客</Link>
            <Link to="/assistant">AI助手</Link>
            <Link to="/studio/login">Studio</Link>
          </nav>
        </div>
      </header>

      <main className="main">
        <div className="container">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/blog" element={<BlogListPage />} />
            <Route path="/blog/:slug" element={<BlogDetailPage />} />
            <Route path="/assistant" element={<AssistantPage />} />
            <Route path="/studio/login" element={<StudioLogin />} />
            <Route path="/studio/articles" element={<StudioArticleList />} />
            <Route path="/studio/articles/new" element={<StudioArticleEdit />} />
            <Route path="/studio/articles/:id/edit" element={<StudioArticleEdit />} />
          </Routes>
        </div>
      </main>

      <footer className="footer">
        <div className="container">
          <p>© 2026 铃铛师兄大模型 | 专注AI技术分享</p>
        </div>
      </footer>
    </div>
  );
}

export default App;
