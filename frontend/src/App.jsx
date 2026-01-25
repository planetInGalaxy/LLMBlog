import { Routes, Route, Link, useNavigate, useParams, useLocation } from 'react-router-dom';
import { useState, useEffect, useRef } from 'react';
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
        <p className="contact-highlight">
          请小红书搜索：
          <a
            className="contact-link"
            href="https://xhslink.com/m/7hzXlmKpfXR"
            target="_blank"
            rel="noopener noreferrer"
          >
            <strong>铃铛师兄大模型求职辅导</strong>
          </a>
          ，获取更多干货
        </p>
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

// ==================== Assistant 页面 ====================
function AssistantPage() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const messagesEndRef = useRef(null);

  // 规范化 Markdown：修复流式输出导致的换行缺失问题（避免把多个标题/列表粘到一行）
  // 只处理代码块之外的内容，尽量不影响 ``` fenced code block
  const normalizeMarkdown = (text) => {
    if (!text) return text;

    // 统一换行符（SSE/代理有时会带 \r\n）
    const normalized = String(text).replace(/\r\n/g, '\n').replace(/\r/g, '\n');

    // 以 ``` 为界拆分，偶数段为非代码块，奇数段为代码块内容
    const parts = normalized.split(/```/);
    for (let i = 0; i < parts.length; i += 2) {
      let t = parts[i];

      // 1) 修复 “上一行文本#### 下一节” 这种标题粘连：在非行首出现的 ##~###### 前补空行
      //    例：xxx#### 一、...  => xxx\n\n#### 一、...
      t = t.replace(/([^\n])\s*(#{2,6}\s)/g, '$1\n\n$2');

      // 2) 修复 “#### 三、xxx- 列表项” 这种标题和列表粘连：标题后强制空行
      //    例：#### 三、xxx- a  => #### 三、xxx\n\n- a
      t = t.replace(/^(#{2,6}[^\n]*?)(\s*)(- |\d+\. )/gm, '$1\n\n$3');

      // 3) 修复 “句子- 列表项” 同行粘连：仅在同一行内插入换行，避免吃掉下一行缩进
      //    例：...。[1]。- 要点  => ...。[1]。\n- 要点
      //    注意：不要用 \s* 跨行，否则会破坏嵌套列表缩进
      t = t.replace(/([。！？.!?;；:：])[ \t]*((?:[-*+]|\d+\.)\s+)/g, '$1\n$2');

      parts[i] = t;
    }

    return parts.join('```');
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const mediaQuery = window.matchMedia('(max-width: 768px)');
    if (!mediaQuery.matches) return;

    const header = document.querySelector('.header');
    if (!header) return;

    const scrollToContent = () => {
      const headerHeight = header.getBoundingClientRect().height;
      if (headerHeight > 0) {
        window.scrollTo({ top: Math.ceil(headerHeight) + 1, behavior: 'auto' });
      }
    };

    const rafId = requestAnimationFrame(() => {
      setTimeout(scrollToContent, 0);
    });

    return () => cancelAnimationFrame(rafId);
  }, []);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 480px)');
    const handleChange = (event) => setIsMobile(event.matches);
    handleChange(mediaQuery);
    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const userMessage = input.trim();
    setInput('');
    
    // 添加用户消息
    const newMessages = [...messages, { role: 'user', content: userMessage }];
    setMessages(newMessages);
    
    // 添加助手占位消息
    setMessages([...newMessages, { role: 'assistant', content: '', citations: [], streaming: true }]);
    setLoading(true);

    try {
      // 构建历史对话
      const history = messages.map(msg => ({
        role: msg.role,
        content: msg.content
      }));

      // 使用 EventSource 或 fetch 流式接收
      const response = await fetch(`${API_URL}/assistant/query/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          question: userMessage,
          mode: 'FLEXIBLE',
          history: history
        })
      });

      if (!response.ok) {
        throw new Error('网络请求失败');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let fullAnswer = '';
      let citations = [];
      
      // 使用 ref 存储内容，减少 React 渲染次数
      let pendingUpdate = false;
      
      const scheduleUpdate = () => {
        if (!pendingUpdate) {
          pendingUpdate = true;
          requestAnimationFrame(() => {
            setMessages(prev => {
              const updated = [...prev];
              updated[updated.length - 1].content = fullAnswer;
              return updated;
            });
            pendingUpdate = false;
          });
        }
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        // 解码新数据
        buffer += decoder.decode(value, { stream: true });
        // 统一处理 CRLF，避免 \r 影响 SSE/Markdown 解析
        buffer = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
        
        // SSE 格式：event:xxx\ndata:xxx\n\n
        // 按双换行分割事件
        const events = buffer.split('\n\n');
        buffer = events.pop() || ''; // 保留未完成的事件
        
        for (const eventBlock of events) {
          if (!eventBlock.trim()) continue;
          
          const lines = eventBlock.split('\n');
          let eventType = 'message';
          const dataLines = [];

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              // SSE 标准：逐行 data 以 \n 连接，空行也必须保留
              dataLines.push(line.slice(5));
            }
          }

          const eventData = dataLines.join('\n');
          
          // 处理不同类型的事件
          if (eventType === 'message') {
            // 直接追加内容，保留原始格式
            fullAnswer += eventData;
            scheduleUpdate();
          } else if (eventType === 'citations') {
            try {
              citations = JSON.parse(eventData);
              setMessages(prev => {
                const updated = [...prev];
                updated[updated.length - 1].citations = citations;
                return updated;
              });
            } catch (e) {
              console.warn('解析 citations 失败:', e);
            }
          } else if (eventType === 'done') {
            setMessages(prev => {
              const updated = [...prev];
              updated[updated.length - 1].content = fullAnswer;
              updated[updated.length - 1].streaming = false;
              return updated;
            });
            setLoading(false);
          } else if (eventType === 'error') {
            throw new Error(eventData || '服务器错误');
          }
        }
      }

      // 确保最终状态正确
      setMessages(prev => {
        const updated = [...prev];
        updated[updated.length - 1].content = fullAnswer;
        updated[updated.length - 1].streaming = false;
        return updated;
      });
      setLoading(false);

    } catch (error) {
      console.error('查询失败:', error);
      setMessages(prev => {
        const updated = [...prev];
        updated[updated.length - 1] = {
          role: 'assistant',
          content: '抱歉，查询失败了，请稍后重试。',
          error: true,
          streaming: false
        };
        return updated;
      });
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <div className="assistant-page">
      <div className="chat-header">
        <h1>🤖 AI 学习助手</h1>
        <p>基于您的文章知识库，智能回答问题</p>
      </div>

      <div className="chat-messages">
        {messages.length === 0 && (
          <div className="welcome-message">
            <h2>👋 欢迎使用 AI 学习助手</h2>
            <p>您可以问我任何关于文章内容的问题，我会基于知识库为您解答。</p>
            <div className="example-questions">
              <p><strong>示例问题：</strong></p>
              <button onClick={() => setInput('文章主要讲了什么内容？')}>文章主要讲了什么内容？</button>
              <button onClick={() => setInput('有哪些关键技术点？')}>有哪些关键技术点？</button>
            </div>
          </div>
        )}

        {messages.map((msg, idx) => (
          <div key={idx} className={`message message-${msg.role}`}>
            {msg.role === 'user' ? (
              <div className="message-content">
                <div className="message-avatar avatar-user" aria-hidden="true">
                  <span>你</span>
                </div>
                <div className="message-text">{msg.content}</div>
              </div>
            ) : (
              <div className="message-content">
                <div className="message-avatar avatar-assistant" aria-hidden="true">
                  <span>AI</span>
                </div>
                <div className="message-text">
                  {msg.error ? (
                    <p className="error-text">{msg.content}</p>
                  ) : (
                    <>
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
                            },
                            // 确保段落、标题等元素正确渲染
                            p: ({children}) => <p>{children}</p>,
                            h1: ({children}) => <h1>{children}</h1>,
                            h2: ({children}) => <h2>{children}</h2>,
                            h3: ({children}) => <h3>{children}</h3>,
                            ul: ({children}) => <ul>{children}</ul>,
                            ol: ({children}) => <ol>{children}</ol>,
                            li: ({children}) => <li>{children}</li>,
                            strong: ({children}) => <strong>{children}</strong>,
                            em: ({children}) => <em>{children}</em>,
                          }}
                        >
                          {normalizeMarkdown(msg.content || '思考中...')}
                        </ReactMarkdown>
                      </div>
                      
                      {msg.citations && msg.citations.length > 0 && (
                        <div className="citations">
                          <h4>📚 参考文章：</h4>
                          {msg.citations.map((cite, i) => (
                            <div key={i} className="citation-card">
                              <span className="citation-ref-index">[{cite.refIndex || (i + 1)}]</span>
                              <a href={cite.url} target="_blank" rel="noopener noreferrer">
                                <strong>{cite.title}</strong>
                              </a>
                              {cite.quote && cite.quote.trim() && (
                                <p className="citation-quote">"{cite.quote}"</p>
                              )}
                              <span className="citation-score">相关度: {(cite.score * 100).toFixed(0)}%</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      <form onSubmit={handleSubmit} className="chat-input-form">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder={isMobile ? '输入问题...' : '输入问题... (Enter 发送，Shift+Enter 换行)'}
          rows={3}
          disabled={loading}
        />
        <button type="submit" disabled={loading || !input.trim()}>
          {loading ? '思考中...' : '发送'}
        </button>
      </form>
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
  const [indexHealth, setIndexHealth] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchArticles();
    fetchIndexHealth();
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

  const fetchIndexHealth = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/index-health`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (result.success) {
        setIndexHealth(result.data);
      }
    } catch (error) {
      console.error('获取索引健康状态失败:', error);
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
        alert('发布成功！索引任务已提交');
        fetchArticles();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('发布失败:', error);
    }
  };

  const handleOffline = async (id) => {
    if (!confirm('确定要下线这篇文章吗？')) return;
    
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}/offline`, {
        method: 'PUT',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (result.success) {
        alert('下线成功！');
        fetchArticles();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('下线失败:', error);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('确定要删除这篇文章吗？此操作不可恢复！')) return;
    
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (result.success) {
        alert('删除成功！');
        fetchArticles();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('删除失败:', error);
    }
  };

  const handleReindex = async (id) => {
    if (!confirm('确定要重新索引这篇文章吗？')) return;
    
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/articles/${id}/reindex`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (result.success) {
        alert('索引任务已提交！');
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('重新索引失败:', error);
    }
  };

  const handleReindexAll = async () => {
    if (!confirm('确定要对所有已发布文章重建索引吗？这可能需要一些时间。')) return;
    
    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const response = await fetch(`${API_URL}/studio/reindex-all`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const result = await response.json();
      if (result.success) {
        alert(result.message || '索引任务已全部提交！');
        fetchIndexHealth();
      } else {
        alert(result.message);
      }
    } catch (error) {
      console.error('全量重建索引失败:', error);
      alert('操作失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="studio-article-list">
      <div className="studio-header">
        <h1>文章管理</h1>
        <div className="header-actions">
          <button onClick={() => navigate('/studio/articles/new')}>新建文章</button>
          <button 
            onClick={handleReindexAll} 
            disabled={loading}
            className="btn-warning"
          >
            {loading ? '执行中...' : '🔄 全量重建索引'}
          </button>
        </div>
      </div>

      {/* 索引健康状态卡片 */}
      {indexHealth && (
        <div className={`index-health-card ${indexHealth.healthy ? 'healthy' : 'unhealthy'}`}>
          <h3>📊 索引健康状态</h3>
          <div className="health-info">
            <div className="health-item">
              <span>ES 连接：</span>
              <strong>{indexHealth.esConnected ? '✅ 正常' : '❌ 失败'}</strong>
            </div>
            <div className="health-item">
              <span>索引存在：</span>
              <strong>{indexHealth.indexExists ? '✅ 是' : '❌ 否'}</strong>
            </div>
            <div className="health-item">
              <span>文章数量：</span>
              <strong>{indexHealth.articleCount || 0}</strong>
            </div>
            <div className="health-item">
              <span>文档数量（chunks）：</span>
              <strong>{indexHealth.documentCount}</strong>
            </div>
            <div className="health-item">
              <span>状态：</span>
              <strong className={indexHealth.healthy ? 'text-success' : 'text-error'}>
                {indexHealth.message}
              </strong>
            </div>
          </div>
        </div>
      )}
      
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
                  <button onClick={() => handlePublish(article.id)} className="btn-success">发布</button>
                )}
                {article.status === 'PUBLISHED' && (
                  <>
                    <button onClick={() => handleReindex(article.id)} className="btn-info">重新索引</button>
                    <button onClick={() => handleOffline(article.id)} className="btn-warning">下线</button>
                  </>
                )}
                <button onClick={() => handleDelete(article.id)} className="btn-danger">删除</button>
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
      if (result.success) {
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

// ==================== 主应用 ====================
function App() {
  const location = useLocation();
  const isAssistant = location.pathname.startsWith('/assistant');

  return (
    <div className="app">
      <header className={`header${isAssistant ? ' header-assistant' : ''}`}>
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

      <main className={`main${isAssistant ? ' main-assistant' : ''}`}>
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

      <footer className={`footer${isAssistant ? ' footer-assistant' : ''}`}>
        <div className="container">
          <p>© 2026 铃铛师兄大模型 | 专注AI技术分享</p>
        </div>
      </footer>
    </div>
  );
}

export default App;
