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
              {/* 搜索栏 */}
              <div className="search-bar">
                <input
                  type="text"
                  placeholder="搜索文章..."
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
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
          <p>关键词：铃铛师兄大模型、AI技术、大模型应用、机器学习</p>
        </div>
      </footer>
    </div>
  );
}

export default App;
