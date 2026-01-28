import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { API_URL, isApiSuccess } from '../lib/api';

function HomePage() {
  const [articles, setArticles] = useState([]);
  const [showBackToTop, setShowBackToTop] = useState(false);

  useEffect(() => {
    const fetchArticles = async () => {
      try {
        const resp = await fetch(`${API_URL}/articles`);
        const result = await resp.json();
        if (isApiSuccess(result) && Array.isArray(result.data)) {
          setArticles(result.data);
        }
      } catch (e) {
        // 首页不阻塞：失败时保持静态内容
        console.warn('首页获取文章列表失败:', e);
      }
    };
    fetchArticles();
  }, []);

  useEffect(() => {
    const onScroll = () => {
      setShowBackToTop(window.scrollY > 480);
    };
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const featured = useMemo(() => {
    if (!Array.isArray(articles) || articles.length === 0) return [];
    // 精选文章：取阅读量最高的 3 篇（降序）
    return [...articles]
      .sort((a, b) => (Number(b.viewCount || 0) - Number(a.viewCount || 0)))
      .slice(0, 4);
  }, [articles]);

  return (
    <div className="home-page">
      {/* Hero */}
      <section className="home-hero" aria-label="首页首屏">
        <div className="home-hero-inner">
          <div className="home-hero-left">
            <h1>把大模型技术，变成你能拿到 offer 的能力</h1>
            <p className="home-hero-subtitle">
              这里有高质量技术文章 + 可引用的 AI 学习助手。<br />
              不只讲原理，更讲怎么做、怎么调、怎么上线。
            </p>

            <div className="home-hero-actions">
              <Link to="/blog" className="btn btn-primary">开始学习（看文章）</Link>
              <Link to="/assistant" className="btn btn-secondary">直接问助手</Link>
            </div>
          </div>

          <div className="home-hero-right">
            <div className="home-proof-card">
              <div className="home-proof-title">你会在这里得到什么</div>
              <ul className="home-proof-list">
                <li>文章库驱动回答：引用到具体段落</li>
                <li>RAG 可调参、可观测（Studio）</li>
                <li>面向转型与面试：方法论 + 实战项目</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* 价值卡 */}
      <section className="home-section" aria-label="立即获得">
        <div className="home-section-header">
          <h2>你能立刻获得什么</h2>
          <p>先给你确定收益，再谈长期路线。</p>
        </div>

        <div className="home-cards">
          <div className="home-card">
            <h3>把“会做”讲成“能过面”的答案</h3>
            <p>用工程视角讲清取舍、指标与风险，让面试官听懂你在做什么。</p>
          </div>
          <div className="home-card">
            <h3>RAG / Agent：从概念到可落地实现</h3>
            <p>切分、索引、召回、重排、调参，一条龙做成可复用能力。</p>
          </div>
          <div className="home-card">
            <h3>一条不绕弯的学习路径</h3>
            <p>从基础 → RAG → Agent → 工程化，把能力一步步拼起来。</p>
          </div>
        </div>
      </section>

      {/* 精选文章 */}
      <section className="home-section" aria-label="精选文章">
        <div className="home-section-header">
          <h2>精选文章（阅读量最高 Top 4）</h2>
          <p>先从这 4 篇开始，最快建立体系。</p>
        </div>

        {featured.length > 0 ? (
          <div className="home-featured-grid">
            {featured.map((a) => (
              <Link key={a.id} to={`/blog/${a.slug}`} className="home-featured-card">
                <div className="home-featured-title">{a.title}</div>
                <div className="home-featured-meta">
                  <span>{new Date(a.publishedAt).toLocaleDateString()}</span>
                  <span>{a.viewCount} 次浏览</span>
                </div>
                <div className="home-featured-summary">{(a.summary && a.summary.trim()) ? a.summary : '...'}</div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="home-featured-empty">
            <p>正在加载精选内容…</p>
            <Link to="/blog" className="btn btn-primary">去文章列表</Link>
          </div>
        )}

        <div className="home-more">
          <Link to="/blog" className="home-more-link">查看更多文章 →</Link>
        </div>
      </section>

      {/* 学习路线 */}
      <section className="home-section" aria-label="学习路线">
        <div className="home-section-header">
          <h2>学习路径（从 0 到可落地）</h2>
          <p>把知识变成能力：能做出来，也能讲清楚。</p>
        </div>

        <div className="home-steps">
          <div className="home-step">
            <div className="home-step-index">1</div>
            <div className="home-step-body">
              <div className="home-step-title">基础知识补齐</div>
              <div className="home-step-desc">LLM 基础、提示词、常见误区与边界。</div>
            </div>
          </div>
          <div className="home-step">
            <div className="home-step-index">2</div>
            <div className="home-step-body">
              <div className="home-step-title">RAG：召回、重排、引用</div>
              <div className="home-step-desc">降低幻觉、提升可控性，建立“可解释的答案”。</div>
            </div>
          </div>
          <div className="home-step">
            <div className="home-step-index">3</div>
            <div className="home-step-body">
              <div className="home-step-title">Agent：工具调用与流程编排</div>
              <div className="home-step-desc">从“聊天”走向“做事”，把能力接入真实系统。</div>
            </div>
          </div>
          <div className="home-step">
            <div className="home-step-index">4</div>
            <div className="home-step-body">
              <div className="home-step-title">工程化与可观测</div>
              <div className="home-step-desc">上线、稳定、可调参、可复盘，让效果持续迭代。</div>
            </div>
          </div>
        </div>
      </section>

      {/* 作者与信任 */}
      <section className="home-section" aria-label="关于我">
        <div className="home-section-header">
          <h2>关于我</h2>
        </div>

        <div className="home-about-grid">
          <div className="home-about-card">
            <h3>客观背景</h3>
            <p>985 本科，人工智能专业毕业。大厂面试官 / 大模型开发工程师。</p>
          </div>

          <div className="home-about-card">
            <h3>我更关注什么</h3>
            <p>把知识变成可复用的能力，把项目做成能讲清楚的面试案例，而不是刷概念。</p>
          </div>

          <div className="home-about-card">
            <h3>你可能适合这里</h3>
            <ul>
              <li>校招 / 社招：想转向大模型方向</li>
              <li>后端 / 前端 / 测试：想补齐知识并做出可落地项目</li>
              <li>想系统提升简历与面试表达</li>
            </ul>
          </div>
        </div>
      </section>

      {/* 联系方式 */}
      <section className="home-cta" aria-label="联系方式">
        <h2>联系我</h2>
        <p className="home-cta-contact">
          想获取更多干货：
          <a
            className="home-cta-link"
            href="https://xhslink.com/m/7hzXlmKpfXR"
            target="_blank"
            rel="noopener noreferrer"
          >
            铃铛师兄大模型求职辅导
          </a>
        </p>
      </section>

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

export default HomePage;
