import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'

type Portfolio = { id: string; name: string; description: string | null }
type Holding = { id: string }
type Transaction = { id: string; symbol: string; transactionType: 'BUY' | 'SELL'; transactionDate: string }

export function DashboardPage() {
  const { user } = useAuth()
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [holdingCount, setHoldingCount] = useState<number | null>(null)
  const [latestTransaction, setLatestTransaction] = useState<Transaction | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const firstName = user?.fullName.trim().split(/\s+/)[0] || 'investor'
  const today = new Intl.DateTimeFormat('en-IN', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }).format(new Date()).toUpperCase()

  useEffect(() => {
    let current = true
    async function loadOverview() {
      setLoading(true)
      setError('')
      try {
        const { data } = await api.get<Portfolio[]>('/api/portfolios')
        if (!current) return
        setPortfolios(data)
        const mostRecent = data[0]
        if (!mostRecent) {
          setHoldingCount(0)
          setLatestTransaction(null)
          return
        }
        const [holdings, transactions] = await Promise.all([
          api.get<Holding[]>(`/api/portfolios/${mostRecent.id}/holdings`),
          api.get<Transaction[]>(`/api/portfolios/${mostRecent.id}/transactions`),
        ])
        if (current) {
          setHoldingCount(holdings.data.length)
          setLatestTransaction(transactions.data[0] || null)
        }
      } catch {
        if (current) setError('Could not load your portfolio summary. Try again.')
      } finally {
        if (current) setLoading(false)
      }
    }
    void loadOverview()
    return () => { current = false }
  }, [refreshKey])

  return <div className="page-content">
    <div className="page-heading"><div><div className="eyebrow">{today} <span className="live-dot" /></div><h1>Your financial picture, <span className="accent">in focus.</span></h1><p>A clear view of your BSE portfolios and market history.</p></div><div className="date-chip"><span>◷</span> BSE · INDIA</div></div>
    <section className="welcome-panel"><div className="welcome-copy"><div className="eyebrow">YOUR WORKSPACE</div><h2>Good to have you here, {firstName}.</h2><p>Review your holdings, record buys and sells, and explore historical market prices.</p><Link to="/portfolios" className="text-link">Manage portfolios <span>→</span></Link></div><div className="welcome-art" aria-hidden="true"><div className="art-orbit orbit-a"/><div className="art-orbit orbit-b"/><div className="art-core">P<span>✳</span></div><div className="art-dot dot-a"/><div className="art-dot dot-b"/></div></section>
    <div className="section-title"><div><h2>Workspace overview</h2><p>Summary for your most recently created portfolio</p></div><Link to="/market" className="text-link">Explore market data <span>→</span></Link></div>
    {error && <div className="form-error" role="alert">{error} <button className="text-button" onClick={() => setRefreshKey(key => key + 1)}>Retry</button></div>}
    <section className="stat-grid" aria-label="Portfolio summary">
      <article className="stat-card"><div className="stat-icon green">◈</div><div className="stat-title">Portfolios</div><strong className="stat-value">{loading ? '…' : portfolios.length}</strong><p>{portfolios.length ? <Link to="/portfolios">View your portfolios →</Link> : 'Create your first portfolio'}</p><div className="card-progress"><i/></div></article>
      <article className="stat-card"><div className="stat-icon blue">▤</div><div className="stat-title">Holdings in recent portfolio</div><strong className="stat-value">{loading ? '…' : holdingCount ?? '—'}</strong><p>{portfolios[0]?.name || 'Holdings appear after your first buy'}</p><div className="card-progress"><i/></div></article>
      <article className="stat-card"><div className="stat-icon amber">↗</div><div className="stat-title">Latest activity</div><strong className="stat-value">{loading ? '…' : latestTransaction ? latestTransaction.transactionType : '—'}</strong><p>{latestTransaction ? `${latestTransaction.symbol} · ${new Date(latestTransaction.transactionDate).toLocaleDateString('en-IN')}` : 'No transactions recorded yet'}</p><div className="card-progress"><i/></div></article>
    </section>
    <section className="bottom-note"><span className="note-icon">✳</span><div><strong>Your holdings stay private.</strong><p>Portfolio details are scoped to your authenticated account. Market prices are historical and may be delayed.</p></div><span className="note-spark" aria-hidden="true">↗</span></section>
  </div>
}
