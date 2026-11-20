import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

type Portfolio = { id: string; name: string; description: string | null; createdAt: string }
type Stock = { id: string; symbol: string; companyName: string; exchange: string; sector: string | null }
type Holding = { id: string; stockId: string; symbol: string; companyName: string; quantity: number; averageBuyPrice: number }
type Transaction = { id: string; symbol: string; companyName: string; transactionType: 'BUY' | 'SELL'; quantity: number; price: number; transactionDate: string }
const money = (value: number) => `₹${value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

export function PortfolioPage() {
  const { portfolioId } = useParams()
  const navigate = useNavigate()
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [stocks, setStocks] = useState<Stock[]>([])
  const [holdings, setHoldings] = useState<Holding[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [query, setQuery] = useState('')
  const [stockId, setStockId] = useState('')
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [type, setType] = useState<'BUY' | 'SELL'>('BUY')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const loadPortfolios = useCallback(async () => { const { data } = await api.get<Portfolio[]>('/api/portfolios'); setPortfolios(data) }, [])
  const loadDetails = useCallback(async () => {
    if (!portfolioId) return
    const [h, t] = await Promise.all([api.get<Holding[]>(`/api/portfolios/${portfolioId}/holdings`), api.get<Transaction[]>(`/api/portfolios/${portfolioId}/transactions`)]); setHoldings(h.data); setTransactions(t.data)
  }, [portfolioId])
  useEffect(() => { loadPortfolios().catch(() => setError('Could not load portfolios.')) }, [loadPortfolios])
  useEffect(() => { loadDetails().catch(() => setError('Could not load this portfolio.')) }, [loadDetails])
  useEffect(() => { let current = true; api.get<Stock[]>('/api/stocks', { params: { q: query } }).then(({ data }) => { if (current) setStocks(data) }).catch(() => { if (current) setError('Could not load BSE stocks.') }); return () => { current = false } }, [query])

  async function createPortfolio(event: React.FormEvent) { event.preventDefault(); setBusy(true); setError(''); try { const { data } = await api.post<Portfolio>('/api/portfolios', { name, description }); await loadPortfolios(); setName(''); setDescription(''); navigate(`/portfolios/${data.id}`) } catch { setError('Could not create portfolio. Check the name and try again.') } finally { setBusy(false) } }
  async function record(event: React.FormEvent) { event.preventDefault(); if (!portfolioId) return; setBusy(true); setError(''); try { await api.post(`/api/portfolios/${portfolioId}/transactions`, { stockId, transactionType: type, quantity: Number(quantity), price: Number(price) }); setQuantity(''); setPrice(''); await loadDetails() } catch (e) { const message = (e as { response?: { data?: { message?: string } } }).response?.data?.message; setError(message || 'Could not record transaction.') } finally { setBusy(false) } }
  async function removePortfolio() { if (!portfolioId || !window.confirm('Delete this portfolio, its holdings, and transaction history?')) return; try { await api.delete(`/api/portfolios/${portfolioId}`); await loadPortfolios(); navigate('/portfolios') } catch { setError('Could not delete portfolio.') } }

  return <div className="page-content portfolio-page">
    <div className="page-heading"><div><div className="eyebrow">PORTFOLIO MANAGEMENT <span className="live-dot" /></div><h1>Your <span className="accent">portfolios.</span></h1><p>Organize your BSE holdings and track your transactions.</p></div><div className="date-chip">BSE · INDIA</div></div>
    <div className="portfolio-layout">
      <section className="portfolio-panel"><div className="section-title"><div><h2>My portfolios</h2><p>{portfolios.length} portfolio{portfolios.length === 1 ? '' : 's'}</p></div></div>
        {portfolios.length === 0 && <p className="portfolio-empty">Create a portfolio to get started.</p>}
        <div className="portfolio-list">{portfolios.map(p => <button key={p.id} className={`portfolio-choice ${p.id === portfolioId ? 'selected' : ''}`} onClick={() => navigate(`/portfolios/${p.id}`)}><strong>{p.name}</strong><span>{p.description || 'No description'}</span></button>)}</div>
        <form className="portfolio-form" onSubmit={createPortfolio}><h3>New portfolio</h3><label>Name<input required maxLength={100} value={name} onChange={e => setName(e.target.value)} placeholder="Long-term investments" /></label><label>Description<input maxLength={1000} value={description} onChange={e => setDescription(e.target.value)} placeholder="Optional note" /></label><button className="primary-button" disabled={busy || !name.trim()}>Create portfolio <span>+</span></button></form>
      </section>
      <section className="portfolio-main">
        {!portfolioId ? <div className="portfolio-empty-card"><div className="stat-icon green">◈</div><h2>Select a portfolio</h2><p>Choose one from the list or create a new portfolio to view holdings and transactions.</p></div> : <>
          <div className="portfolio-panel"><div className="portfolio-panel-heading"><div><div className="eyebrow">SELECTED PORTFOLIO</div><h2>{portfolios.find(p => p.id === portfolioId)?.name || 'Portfolio'}</h2><p>{portfolios.find(p => p.id === portfolioId)?.description || 'Your BSE holdings'}</p></div><button className="danger-button" onClick={removePortfolio}>Delete</button></div>
            <div className="section-title"><div><h2>Holdings</h2><p>{holdings.length} stocks</p></div></div>
            {holdings.length ? <div className="table-wrap"><table><thead><tr><th>Stock</th><th>Quantity</th><th>Average cost</th><th>Cost basis</th></tr></thead><tbody>{holdings.map(h => <tr key={h.id}><td><strong>{h.symbol}</strong><small>{h.companyName}</small></td><td>{h.quantity}</td><td>{money(h.averageBuyPrice)}</td><td>{money(h.quantity * h.averageBuyPrice)}</td></tr>)}</tbody></table></div> : <p className="portfolio-empty">No holdings yet. Record a BUY transaction below.</p>}
          </div>
          <div className="portfolio-panel"><div className="section-title"><div><h2>Record transaction</h2><p>BUY updates weighted average cost. SELL reduces quantity at the existing average cost.</p></div></div>
            <form className="transaction-form" onSubmit={record}><label>Action<select value={type} onChange={e => setType(e.target.value as 'BUY'|'SELL')}><option value="BUY">Buy</option><option value="SELL">Sell</option></select></label><label>Find BSE stock<input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search symbol or company" /></label><label>Stock<select required value={stockId} onChange={e => setStockId(e.target.value)}><option value="">Choose stock</option>{stocks.map(s => <option key={s.id} value={s.id}>{s.symbol} · {s.companyName}</option>)}</select></label><label>Quantity<input required min="0.00000001" step="any" type="number" value={quantity} onChange={e => setQuantity(e.target.value)} /></label><label>Price per share<input required min="0.0001" step="any" type="number" value={price} onChange={e => setPrice(e.target.value)} /></label><button className="primary-button" disabled={busy || !stockId || !quantity || !price}>Record {type.toLowerCase()}</button></form>
          </div>
          <div className="portfolio-panel"><div className="section-title"><div><h2>Transaction history</h2><p>Latest transactions first</p></div></div>{transactions.length ? <div className="table-wrap"><table><thead><tr><th>Date</th><th>Type</th><th>Stock</th><th>Quantity</th><th>Price</th></tr></thead><tbody>{transactions.map(t => <tr key={t.id}><td>{new Date(t.transactionDate).toLocaleDateString('en-IN')}</td><td><span className={`transaction-pill ${t.transactionType.toLowerCase()}`}>{t.transactionType}</span></td><td><strong>{t.symbol}</strong><small>{t.companyName}</small></td><td>{t.quantity}</td><td>{money(t.price)}</td></tr>)}</tbody></table></div> : <p className="portfolio-empty">No transactions recorded.</p>}</div>
        </>}
      </section>
    </div>{error && <div className="form-error portfolio-error">{error}</div>}
  </div>
}
