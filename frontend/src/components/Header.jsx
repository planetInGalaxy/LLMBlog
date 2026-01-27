import { Link, NavLink } from 'react-router-dom';

function Header({ isAssistant, isStudioActive }) {
  return (
    <header className={`header${isAssistant ? ' header-assistant' : ''}`}>
      <div className="container">
        <Link to="/" className="logo">🔔 铃铛师兄大模型</Link>
        <nav>
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'is-active' : '')}>首页</NavLink>
          <NavLink to="/blog" className={({ isActive }) => (isActive ? 'is-active' : '')}>博客</NavLink>
          <NavLink to="/assistant" className={({ isActive }) => (isActive ? 'is-active' : '')}>AI助手</NavLink>
          <NavLink
            to="/studio/login"
            className={isStudioActive ? 'is-active' : ''}
            aria-current={isStudioActive ? 'page' : undefined}
          >
            Studio
          </NavLink>
        </nav>
      </div>
    </header>
  );
}

export default Header;
