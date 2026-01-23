import { useState, useEffect } from 'react';
import './App.css';

const API_URL = import.meta.env.VITE_API_URL || '/api';

function App() {
  const [posts, setPosts] = useState([]);
  const [selectedPost, setSelectedPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [searchKeyword, setSearchKeyword] = useState('');

  // 获取所有文章
  useEffect(() => {
    fetchPosts();
  }, []);

  const fetchPosts = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_URL}/posts`);
      const data = await response.json();
      setPosts(data);
    } catch (error) {
      console.error('获取文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 搜索文章
  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      fetchPosts();
      return;
    }
    try {
      setLoading(true);
      const response = await fetch(`${API_URL}/posts/search?keyword=${encodeURIComponent(searchKeyword)}`);
      const data = await response.json();
      setPosts(data);
    } catch (error) {
      console.error('搜索失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 查看文章详情
  const viewPost = async (id) => {
    try {
      const response = await fetch(`${API_URL}/posts/${id}`);
      const data = await response.json();
      setSelectedPost(data);
    } catch (error) {
      console.error('获取文章详情失败:', error);
    }
  };

  // 格式化日期
  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  return (
    <div className="app">
      {/* 顶部导航栏 */}
      <header className="header">
        <div className="container">
          <h1 className="logo">🔔 铃铛师兄大模型</h1>
          <nav className="nav">
            <a href="#" onClick={() => setSelectedPost(null)}>首页</a>
            <a href="#about">关于</a>
          </nav>
        </div>
      </header>

      {/* 主要内容区 */}
      <main className="main">
        <div className="container">
          {!selectedPost ? (
            <>
              {/* 静态介绍区域 - SEO友好 */}
              <section className="intro-section">
                <h2>关于铃铛师兄大模型</h2>
                <div className="intro-content">
                  <p>
                    <strong>铃铛师兄大模型</strong>是一个专注于人工智能和大模型技术的专业博客平台。
                    我们致力于分享最新的AI技术动态、大模型应用实践、机器学习算法解析以及行业前沿见解。
                  </p>
                  <p>
                    在这里，您可以找到关于大语言模型（LLM）、生成式AI、自然语言处理（NLP）、
                    计算机视觉、深度学习等领域的深度文章和技术教程。我们不仅关注理论研究，
                    更注重实际应用和工程实践，帮助开发者更好地理解和应用AI技术。
                  </p>
                  <p>
                    铃铛师兄大模型博客致力于成为AI技术爱好者和从业者的知识分享平台，
                    通过高质量的技术内容，推动AI技术在中国的发展和应用。
                    无论您是AI初学者还是资深工程师，都能在这里找到有价值的内容。
                  </p>
                  <div className="intro-keywords">
                    <strong>核心关键词：</strong>
                    <span>大模型、AI技术、人工智能、机器学习、深度学习、自然语言处理、生成式AI、技术博客</span>
                  </div>
                </div>
              </section>

              {/* 搜索栏 */}
              <div className="search-bar">
                <input
                  type="text"
                  placeholder="搜索文章..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                />
                <button onClick={handleSearch}>搜索</button>
              </div>

              {/* 文章列表 */}
              {loading ? (
                <div className="loading">加载中...</div>
              ) : (
                <div className="posts-grid">
                  {posts.map((post) => (
                    <article key={post.id} className="post-card" onClick={() => viewPost(post.id)}>
                      <h2>{post.title}</h2>
                      <div className="post-meta">
                        <span className="author">👤 {post.author}</span>
                        <span className="date">📅 {formatDate(post.createdAt)}</span>
                        <span className="views">👁 {post.viewCount} 次浏览</span>
                      </div>
                      <p className="summary">{post.summary}</p>
                      {post.tags && (
                        <div className="tags">
                          {post.tags.split(',').map((tag, index) => (
                            <span key={index} className="tag">#{tag.trim()}</span>
                          ))}
                        </div>
                      )}
                    </article>
                  ))}
                </div>
              )}

              {posts.length === 0 && !loading && (
                <div className="no-posts">暂无文章</div>
              )}
            </>
          ) : (
            /* 文章详情 */
            <div className="post-detail">
              <button className="back-button" onClick={() => setSelectedPost(null)}>
                ← 返回列表
              </button>
              <article>
                <h1>{selectedPost.title}</h1>
                <div className="post-meta">
                  <span className="author">👤 {selectedPost.author}</span>
                  <span className="date">📅 {formatDate(selectedPost.createdAt)}</span>
                  <span className="views">👁 {selectedPost.viewCount} 次浏览</span>
                </div>
                {selectedPost.tags && (
                  <div className="tags">
                    {selectedPost.tags.split(',').map((tag, index) => (
                      <span key={index} className="tag">#{tag.trim()}</span>
                    ))}
                  </div>
                )}
                <div className="content">
                  {selectedPost.content.split('\n').map((paragraph, index) => (
                    <p key={index}>{paragraph}</p>
                  ))}
                </div>
              </article>
            </div>
          )}
        </div>
      </main>

      {/* 页脚 */}
      <footer className="footer">
        <div className="container">
          <p>© 2026 铃铛师兄大模型 | 专注AI技术分享</p>
          <p>关键词：AI技术、大模型应用、机器学习</p>
          <p>请小红书搜索：铃铛师兄大模型求职辅导，获取更多干货</p>
        </div>
      </footer>
    </div>
  );
}

export default App;
