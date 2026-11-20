import { useAuth } from '../auth/AuthContext'

export function DashboardPage() {
  const { user } = useAuth()
  const firstName = user?.fullName.split(' ')[0] || 'investor'
  return <div className="page-content"><div className="page-heading"><div><div className="eyebrow">SUNDAY, NOVEMBER 8, 2025 <span className="live-dot" /></div><h1>Your financial picture, <span className="accent">in focus.</span></h1><p>A calm place to understand your investing journey.</p></div><div className="date-chip"><span>◷</span> BSE · INDIA</div></div>
    <section className="welcome-panel"><div className="welcome-copy"><div className="eyebrow">YOUR WORKSPACE IS READY</div><h2>Good to have you here, {firstName}.</h2><p>Create portfolios, add BSE stocks and keep an accurate record of buys and sells.</p><a href="/portfolios" className="text-link">Manage your portfolios <span>→</span></a></div><div className="welcome-art"><div className="art-orbit orbit-a"/><div className="art-orbit orbit-b"/><div className="art-core">P<span>✳</span></div><div className="art-dot dot-a"/><div className="art-dot dot-b"/></div></section>
    <div className="section-title"><div><h2>Portfolio management</h2><p>Track BSE holdings and transactions</p></div><span className="coming-soon">PHASE 2</span></div>
    <section className="bottom-note"><span className="note-icon">✳</span><div><strong>Your holdings stay private.</strong><p>Every portfolio is scoped to your authenticated account, and stock selections come from the BSE sample catalog.</p></div><span className="note-spark">↗</span></section>
  </div>
}
