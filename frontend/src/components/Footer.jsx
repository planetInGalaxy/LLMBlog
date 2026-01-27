const APP_VERSION = String(
  import.meta.env.VITE_APP_VERSION
  || (typeof window !== 'undefined' && window.__APP_VERSION__)
  || (import.meta.env.DEV ? 'dev' : '')
).trim();

const DISPLAY_VERSION = APP_VERSION ? APP_VERSION.slice(0, 7) : '';

function Footer({ isAssistant }) {
  return (
    <footer className={`footer${isAssistant ? ' footer-assistant' : ''}`}>
      <div className="container footer-content">
        <p>© 2026 铃铛师兄大模型 | 专注AI技术分享</p>
        {DISPLAY_VERSION ? (
          <span className="footer-version" title={APP_VERSION}>v{DISPLAY_VERSION}</span>
        ) : null}
      </div>
    </footer>
  );
}

export default Footer;
