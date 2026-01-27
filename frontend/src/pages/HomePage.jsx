import { Link } from 'react-router-dom';

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
              <strong>自然语言处理</strong>、<strong>RAG</strong>、<strong>Agent</strong>等领域的深度文章和技术教程。
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
            <span className="keyword-tag">RAG</span>
            <span className="keyword-tag">Agent</span>
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

export default HomePage;
