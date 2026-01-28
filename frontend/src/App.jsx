import { Routes, Route, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import Header from './components/Header';
import Footer from './components/Footer';
import HomePage from './pages/HomePage';
import BlogListPage from './pages/BlogListPage';
import BlogDetailPage from './pages/BlogDetailPage';
import AssistantPage from './pages/AssistantPage';
import StudioLogin from './pages/studio/StudioLogin';
import StudioArticleList from './pages/studio/StudioArticleList';
import StudioArticleEdit from './pages/studio/StudioArticleEdit';
import StudioRagSettings from './pages/studio/StudioRagSettings';
import StudioRagLogs from './pages/studio/StudioRagLogs';
import StudioRagLogDetail from './pages/studio/StudioRagLogDetail';
import StudioChunks from './pages/studio/StudioChunks';
import StudioPrompts from './pages/studio/StudioPrompts';
import './App.css';

function App() {
  const location = useLocation();
  const isAssistant = location.pathname.startsWith('/assistant');
  const isStudioActive = location.pathname.startsWith('/studio');

  // 路由切换后回到顶部（避免从首页进入助手时保留滚动位置导致“下滑一下”）
  useEffect(() => {
    if (typeof window === 'undefined') return;
    window.scrollTo(0, 0);
  }, [location.pathname]);

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;

    const mediaQuery = window.matchMedia('(max-width: 768px)');
    const updateBodyClass = () => {
      if (isAssistant && mediaQuery.matches) {
        document.body.classList.add('assistant-lock-scroll');
      } else {
        document.body.classList.remove('assistant-lock-scroll');
      }
    };

    updateBodyClass();
    mediaQuery.addEventListener('change', updateBodyClass);

    return () => {
      mediaQuery.removeEventListener('change', updateBodyClass);
      document.body.classList.remove('assistant-lock-scroll');
    };
  }, [isAssistant]);

  return (
    <div className="app">
      <Header isAssistant={isAssistant} isStudioActive={isStudioActive} />

      <main className={`main${isAssistant ? ' main-assistant' : ''}`}>
        <div className="container">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/blog" element={<BlogListPage />} />
            <Route path="/blog/:slug" element={<BlogDetailPage />} />
            <Route path="/assistant" element={<AssistantPage />} />
            <Route path="/studio/login" element={<StudioLogin />} />
            <Route path="/studio/articles" element={<StudioArticleList />} />
            <Route path="/studio/settings" element={<StudioRagSettings />} />
            <Route path="/studio/rag-logs" element={<StudioRagLogs />} />
            <Route path="/studio/rag-logs/:requestId" element={<StudioRagLogDetail />} />
            <Route path="/studio/chunks" element={<StudioChunks />} />
            <Route path="/studio/prompts" element={<StudioPrompts />} />
            <Route path="/studio/articles/new" element={<StudioArticleEdit />} />
            <Route path="/studio/articles/:id/edit" element={<StudioArticleEdit />} />
          </Routes>
        </div>
      </main>

      <Footer isAssistant={isAssistant} />
    </div>
  );
}

export default App;
