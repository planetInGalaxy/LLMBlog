const APP_VERSION = String(
  import.meta.env.VITE_APP_VERSION
  || (typeof window !== 'undefined' && window.__APP_VERSION__)
  || (import.meta.env.DEV ? 'dev' : '')
).trim();

function Footer({ isAssistant }) {
  return (
    <footer className={`footer${isAssistant ? ' footer-assistant' : ''}`}>
      <div className="container footer-content">
        <p>© 2026 铃铛师兄大模型 | 专注AI技术分享</p>
        {APP_VERSION ? <span className="footer-version">v{APP_VERSION}</span> : null}
      </div>
    </footer>
  );
}

export default Footer;
