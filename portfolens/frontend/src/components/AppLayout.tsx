import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const signOut = () => { logout(); navigate('/login', { replace: true }) }
  return <div className="app-shell">
    <aside className="sidebar">
      <a className="brand" href="/dashboard"><span className="brand-mark">P</span><span>portfo<span className="accent">lens</span></span></a>
      <div className="nav-label">WORKSPACE</div>
      <nav className="side-links">
        <NavLink to="/dashboard" className={({ isActive }) => isActive ? 'side-link active' : 'side-link'}><span>◫</span> Overview</NavLink>
        <NavLink to="/portfolios" className={({ isActive }) => isActive ? 'side-link active' : 'side-link'}><span>◈</span> Portfolios</NavLink>
        <NavLink to="/market" className={({ isActive }) => isActive ? 'side-link active' : 'side-link'}><span>⌁</span> Market explorer</NavLink>
        <NavLink to="/profile" className={({ isActive }) => isActive ? 'side-link active' : 'side-link'}><span>◎</span> Profile</NavLink>
      </nav>
      <div className="sidebar-bottom"><div className="user-mini"><div className="avatar">{user?.fullName.slice(0, 1).toUpperCase()}</div><div className="user-mini-info"><strong>{user?.fullName}</strong><span>{user?.email}</span></div><button className="icon-button" onClick={signOut} aria-label="Log out" title="Log out">↗</button></div></div>
    </aside>
    <main className="main-area"><header className="mobile-header"><a className="brand" href="/dashboard"><span className="brand-mark">P</span><span>portfo<span className="accent">lens</span></span></a><nav><NavLink to="/dashboard">Overview</NavLink><NavLink to="/portfolios">Portfolios</NavLink><NavLink to="/market">Market</NavLink><NavLink to="/profile">Profile</NavLink><button onClick={signOut}>Log out</button></nav></header><Outlet /></main>
  </div>
}
