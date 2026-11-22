import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

type Portfolio = { id: string; name: string; description: string | null; createdAt: string }
type Stock = { id: string; symbol: string; companyName: string; exchange: string; sector: string | null }
type Holding = { id: string; stockId: string; symbol: string; companyName: string; quantity: number; averageBuyPrice: number }
type Transaction = { id: string; symbol: string; companyName: string; transactionType: 'BUY' | 'SELL'; quantity: number; price: number; transactionDate: string }
type Analytics = { status: string; observations: number; annualizedCovariance: number | null; beta: number | null; annualizedAlpha: number | null; annualizedSharpe: number | null; riskFreeAnnualRate: number | null; message: string | null; windowStart: string | null; windowEnd: string | null }

const money = (value: number) => `₹${value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const errorMessage = (error: unknown, fallback: string) => {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
  return message || fallback
}

export function PortfolioPage() {
  const { portfolioId } = useParams()
  const navigate = useNavigate()
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [portfoliosLoading, setPortfoliosLoading] = useState(true)
  const [stocks, setStocks] = useState<Stock[]>([])
  const [stocksLoading, setStocksLoading] = useState(false)
  const [stocksError, setStocksError] = useState('')
  const [holdings, setHoldings] = useState<Holding[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [detailsLoading, setDetailsLoading] = useState(false)
  const [analytics, setAnalytics] = useState<Analytics | null>(null)
  const [analyticsError, setAnalyticsError] = useState('')
  const [analyticsLoading, setAnalyticsLoading] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [editingPortfolio, setEditingPortfolio] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [query, setQuery] = useState('')
  const [stockId, setStockId] = useState('')
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [transactionDate, setTransactionDate] = useState('')
  const [type, setType] = useState<'BUY' | 'SELL'>('BUY')
  const [busy, setBusy] = useState(false)
  const [updateBusy, setUpdateBusy] = useState(false)
  const [deleteBusy, setDeleteBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const detailsRequest = useRef(0)
  const idempotency = useRef<{ fingerprint: string; key: string } | null>(null)

  const activePortfolio = portfolios.find(portfolio => portfolio.id === portfolioId)
  const selectedHolding = useMemo(() => holdings.find(holding => holding.stockId === stockId), [holdings, stockId])
  const quantityNumber = Number(quantity)
  const priceNumber = Number(price)
  const tradeTotal = quantityNumber > 0 && priceNumber > 0 ? quantityNumber * priceNumber : null
  const sellExceedsHolding = type === 'SELL' && (!selectedHolding || quantityNumber > selectedHolding.quantity)

  const loadPortfolios = useCallback(async () => {
    setPortfoliosLoading(true)
    try {
      const { data } = await api.get<Portfolio[]>('/api/portfolios')
      setPortfolios(data)
      if (portfolioId && !data.some(portfolio => portfolio.id === portfolioId)) {
        setError('That portfolio could not be found in your account.')
        navigate('/portfolios', { replace: true })
      }
    } finally { setPortfoliosLoading(false) }
  }, [navigate, portfolioId])

  const loadDetails = useCallback(async () => {
    if (!portfolioId) return
    const requestId = ++detailsRequest.current
    setDetailsLoading(true)
    try {
      const [holdingResponse, transactionResponse] = await Promise.all([
        api.get<Holding[]>(`/api/portfolios/${portfolioId}/holdings`),
        api.get<Transaction[]>(`/api/portfolios/${portfolioId}/transactions`),
      ])
      if (requestId === detailsRequest.current) {
        setHoldings(holdingResponse.data)
        setTransactions(transactionResponse.data)
      }
    } finally {
      if (requestId === detailsRequest.current) setDetailsLoading(false)
    }
  }, [portfolioId])

  useEffect(() => {
    let current = true
    loadPortfolios().catch(() => { if (current) setError('Could not load your portfolios. Please retry.') })
    return () => { current = false }
  }, [loadPortfolios])

  useEffect(() => {
    setHoldings([])
    setTransactions([])
    if (!portfolioId) { setDetailsLoading(false); return }
    let current = true
    loadDetails().catch(() => { if (current) setError('Could not load this portfolio. Please retry.') })
    return () => { current = false; detailsRequest.current += 1 }
  }, [loadDetails, portfolioId])

  useEffect(() => {
    if (!portfolioId) { setAnalytics(null); setAnalyticsLoading(false); return }
    let current = true
    setAnalytics(null)
    setAnalyticsError('')
    setAnalyticsLoading(true)
    const timer = window.setTimeout(() => {
      api.get<Analytics>(`/api/analytics/portfolios/${portfolioId}`)
        .then(({ data }) => { if (current) setAnalytics(data) })
        .catch(() => { if (current) setAnalyticsError('Analytics could not be reached. Your holdings and trades are still available.') })
        .finally(() => { if (current) setAnalyticsLoading(false) })
    }, 700)
    return () => { current = false; window.clearTimeout(timer) }
  }, [portfolioId, transactions.length])

  useEffect(() => {
    let current = true
    const timer = window.setTimeout(() => {
      setStocksLoading(true)
      setStocksError('')
      api.get<Stock[]>('/api/stocks', { params: query.trim() ? { q: query.trim() } : undefined })
        .then(({ data }) => { if (current) setStocks(data) })
        .catch(() => { if (current) setStocksError('Stock search is unavailable. Try again in a moment.') })
        .finally(() => { if (current) setStocksLoading(false) })
    }, query ? 250 : 0)
    return () => { current = false; window.clearTimeout(timer) }
  }, [query])

  async function createPortfolio(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) return
    setBusy(true); setError(''); setNotice('')
    try {
      const { data } = await api.post<Portfolio>('/api/portfolios', { name: name.trim(), description: description.trim() || null })
      setPortfolios(current => [data, ...current])
      setName(''); setDescription('')
      navigate(`/portfolios/${data.id}`)
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not create the portfolio. Check the name and try again.'))
    } finally { setBusy(false) }
  }

  function startPortfolioEdit() {
    if (!activePortfolio) return
    setEditName(activePortfolio.name)
    setEditDescription(activePortfolio.description || '')
    setEditingPortfolio(true)
    setError('')
  }

  async function updatePortfolio(event: FormEvent) {
    event.preventDefault()
    if (!portfolioId || !editName.trim()) return
    setUpdateBusy(true); setError(''); setNotice('')
    try {
      const { data } = await api.put<Portfolio>(`/api/portfolios/${portfolioId}`, { name: editName.trim(), description: editDescription.trim() || null })
      setPortfolios(current => current.map(portfolio => portfolio.id === data.id ? data : portfolio))
      setEditingPortfolio(false)
      setNotice('Portfolio details updated.')
    } catch (requestError) { setError(errorMessage(requestError, 'Could not update this portfolio. Please try again.')) }
    finally { setUpdateBusy(false) }
  }

  async function record(event: FormEvent) {
    event.preventDefault()
    if (!portfolioId || !stockId) return
    if (!Number.isFinite(quantityNumber) || quantityNumber <= 0 || !Number.isFinite(priceNumber) || priceNumber <= 0) {
      setError('Enter a quantity and share price greater than zero.')
      return
    }
    if (sellExceedsHolding) {
      setError(selectedHolding ? `You can sell up to ${selectedHolding.quantity} shares of this stock.` : 'You do not currently hold this stock in this portfolio.')
      return
    }
    setBusy(true); setError(''); setNotice('')
    const body = { stockId, transactionType: type, quantity: quantityNumber, price: priceNumber, transactionDate: transactionDate ? new Date(transactionDate).toISOString() : undefined }
    const fingerprint = JSON.stringify(body)
    if (!idempotency.current || idempotency.current.fingerprint !== fingerprint) idempotency.current = { fingerprint, key: crypto.randomUUID() }
    try {
      const { data: saved } = await api.post<Transaction>(`/api/portfolios/${portfolioId}/transactions`, body, { headers: { 'Idempotency-Key': idempotency.current.key } })
      idempotency.current = null
      setQuantity(''); setPrice(''); setTransactionDate('')
      setTransactions(current => [saved, ...current])
      setNotice(`${type === 'BUY' ? 'Buy' : 'Sell'} recorded for ${saved.symbol}.`)
      try { await loadDetails() }
      catch { setError('The transaction was recorded, but the portfolio view could not refresh. Retry loading the details.') }
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not confirm the transaction. Retry to safely check whether it was recorded.'))
    } finally { setBusy(false) }
  }

  async function removePortfolio() {
    if (!portfolioId || !window.confirm('Delete this portfolio, its holdings, and transaction history?')) return
    setDeleteBusy(true); setError('')
    try {
      await api.delete(`/api/portfolios/${portfolioId}`)
      setPortfolios(current => current.filter(portfolio => portfolio.id !== portfolioId))
      navigate('/portfolios')
    } catch (requestError) { setError(errorMessage(requestError, 'Could not delete this portfolio. Please try again.')) }
    finally { setDeleteBusy(false) }
  }

  return <div className="page-content portfolio-page">
    <div className="page-heading"><div><div className="eyebrow">PORTFOLIO MANAGEMENT <span className="live-dot" /></div><h1>Your <span className="accent">portfolios.</span></h1><p>Organize your BSE holdings and track your transactions.</p></div><div className="date-chip">BSE · INDIA</div></div>
    {error && <div className="form-error portfolio-error" role="alert">{error}</div>}
    {notice && <div className="form-success portfolio-error" role="status">{notice}</div>}
    <div className="portfolio-layout">
      <section className="portfolio-panel" aria-label="Portfolio list and creation">
        <div className="section-title"><div><h2>My portfolios</h2><p>{portfoliosLoading ? 'Loading portfolios…' : `${portfolios.length} portfolio${portfolios.length === 1 ? '' : 's'}`}</p></div></div>
        {!portfoliosLoading && portfolios.length === 0 && <p className="portfolio-empty">Create a portfolio to get started.</p>}
        <div className="portfolio-list">{portfolios.map(portfolio => <button key={portfolio.id} type="button" aria-current={portfolio.id === portfolioId ? 'page' : undefined} className={`portfolio-choice ${portfolio.id === portfolioId ? 'selected' : ''}`} onClick={() => navigate(`/portfolios/${portfolio.id}`)}><strong>{portfolio.name}</strong><span>{portfolio.description || 'No description'}</span></button>)}</div>
        <form className="portfolio-form" onSubmit={createPortfolio}><h3>New portfolio</h3><label>Name<input required maxLength={100} value={name} onChange={event => setName(event.target.value)} placeholder="Long-term investments" /></label><label>Description<input maxLength={1000} value={description} onChange={event => setDescription(event.target.value)} placeholder="Optional note" /></label><button className="primary-button" disabled={busy || !name.trim()}>{busy ? 'Creating…' : <>Create portfolio <span>+</span></>}</button></form>
      </section>
      <section className="portfolio-main">
        {!portfolioId ? <div className="portfolio-empty-card"><div className="stat-icon green">◈</div><h2>{portfolios.length ? 'Select a portfolio' : 'Your portfolio workspace is ready'}</h2><p>{portfolios.length ? 'Choose one from the list or create a new portfolio to view holdings and transactions.' : 'Use the form to create your first portfolio.'}</p></div> : <>
          <div className="portfolio-panel"><div className="portfolio-panel-heading"><div><div className="eyebrow">SELECTED PORTFOLIO</div><h2>{activePortfolio?.name || (portfoliosLoading ? 'Loading portfolio…' : 'Portfolio')}</h2><p>{activePortfolio?.description || 'Your BSE holdings'}</p></div><div className="portfolio-actions"><button className="secondary-button" type="button" onClick={startPortfolioEdit} disabled={!activePortfolio || updateBusy}>Edit</button><button className="danger-button" type="button" onClick={removePortfolio} disabled={deleteBusy}>{deleteBusy ? 'Deleting…' : 'Delete'}</button></div></div>
            {editingPortfolio && <form className="portfolio-edit-form" onSubmit={updatePortfolio}><label>Name<input required maxLength={100} value={editName} onChange={event => setEditName(event.target.value)} autoFocus /></label><label>Description<input maxLength={1000} value={editDescription} onChange={event => setEditDescription(event.target.value)} /></label><div><button className="primary-button" disabled={updateBusy || !editName.trim()}>{updateBusy ? 'Saving…' : 'Save details'}</button><button className="secondary-button" type="button" onClick={() => setEditingPortfolio(false)} disabled={updateBusy}>Cancel</button></div></form>}
            <div className="section-title"><div><h2>Holdings</h2><p>{detailsLoading ? 'Refreshing…' : `${holdings.length} stocks`}</p></div></div>
            {detailsLoading && holdings.length === 0 ? <p className="portfolio-empty">Loading holdings…</p> : holdings.length ? <div className="table-wrap"><table><thead><tr><th>Stock</th><th>Quantity</th><th>Average cost</th><th>Cost basis</th></tr></thead><tbody>{holdings.map(holding => <tr key={holding.id}><td><strong>{holding.symbol}</strong><small>{holding.companyName}</small></td><td>{holding.quantity}</td><td>{money(holding.averageBuyPrice)}</td><td>{money(holding.quantity * holding.averageBuyPrice)}</td></tr>)}</tbody></table></div> : <p className="portfolio-empty">No holdings yet. Record a BUY transaction below.</p>}
          </div>
          <div className="portfolio-panel"><div className="section-title"><div><h2>Record transaction</h2><p>BUY updates weighted average cost. SELL reduces quantity at the existing average cost.</p></div></div>
            <form className="transaction-form" onSubmit={record}>
              <label>Action<select value={type} onChange={event => { setType(event.target.value as 'BUY' | 'SELL'); setError('') }}><option value="BUY">Buy</option><option value="SELL">Sell</option></select></label>
              <label>Find BSE stock<input value={query} onChange={event => { setQuery(event.target.value); setStockId(''); setStocks([]) }} placeholder="Search symbol or company" autoComplete="off" /></label>
              <label>Stock<select required value={stockId} onChange={event => setStockId(event.target.value)} disabled={stocksLoading && stocks.length === 0}><option value="">{stocksLoading ? 'Loading stocks…' : 'Choose stock'}</option>{stocks.map(stock => <option key={stock.id} value={stock.id}>{stock.symbol} · {stock.companyName}</option>)}</select>{stocksError && <span className="field-hint" role="alert">{stocksError}</span>}</label>
              <label>Quantity<input required min="0.00000001" step="any" type="number" inputMode="decimal" value={quantity} onChange={event => setQuantity(event.target.value)} max={type === 'SELL' && selectedHolding ? selectedHolding.quantity : undefined} aria-describedby={type === 'SELL' && selectedHolding ? 'sell-available' : undefined} />{type === 'SELL' && <span id="sell-available" className="field-hint">{selectedHolding ? `Available to sell: ${selectedHolding.quantity}` : 'No shares available to sell for this stock.'}</span>}</label>
              <label>Price per share<input required min="0.0001" step="any" type="number" inputMode="decimal" value={price} onChange={event => setPrice(event.target.value)} /></label>
              <label>Trade date and time <span className="field-hint">Leave blank to use now.</span><input type="datetime-local" value={transactionDate} onChange={event => setTransactionDate(event.target.value)} /></label>
              <button className="primary-button" disabled={busy || !stockId || !quantity || !price || sellExceedsHolding}>{busy ? 'Saving…' : `Record ${type.toLowerCase()}`}</button>
              {tradeTotal != null && <p className="trade-estimate">Estimated transaction value: <strong>{money(tradeTotal)}</strong></p>}
            </form>
          </div>
          <div className="portfolio-panel"><div className="section-title"><div><h2>Risk &amp; performance</h2><p>BSE Sensex · daily returns · trailing 1 year · updates after trade events are processed</p></div></div>
            {analyticsError ? <p className="portfolio-empty" role="status">{analyticsError}</p> : analyticsLoading ? <p className="portfolio-empty"><span className="spinner small" /> Calculating portfolio analytics…</p> : !analytics ? <p className="portfolio-empty">Analytics are not available yet.</p> : analytics.status !== 'READY' ? <><p className="portfolio-empty">{analytics.message || 'More price history is needed to calculate these metrics.'}</p>{analytics.riskFreeAnnualRate != null && <p className="field-hint">Annual risk-free rate used: {(analytics.riskFreeAnnualRate * 100).toFixed(2)}%</p>}</> : <><div className="analytics-metrics"><div><small>Covariance (annualized)</small><strong>{analytics.annualizedCovariance?.toFixed(6) ?? '—'}</strong></div><div><small>Beta</small><strong>{analytics.beta?.toFixed(3) ?? '—'}</strong></div><div><small>Alpha (annualized)</small><strong>{analytics.annualizedAlpha == null ? '—' : `${(analytics.annualizedAlpha * 100).toFixed(2)}%`}</strong></div><div><small>Sharpe ratio (annualized)</small><strong>{analytics.annualizedSharpe?.toFixed(3) ?? '—'}</strong></div><div><small>Risk-free rate</small><strong>{analytics.riskFreeAnnualRate == null ? '—' : `${(analytics.riskFreeAnnualRate * 100).toFixed(2)}%`}</strong></div></div><p className="portfolio-empty">Based on {analytics.observations} aligned daily observations from {analytics.windowStart} to {analytics.windowEnd}.</p></>}
          </div>
          <div className="portfolio-panel"><div className="section-title"><div><h2>Transaction history</h2><p>Latest transactions first</p></div></div>{detailsLoading && transactions.length === 0 ? <p className="portfolio-empty">Loading transactions…</p> : transactions.length ? <div className="table-wrap"><table><thead><tr><th>Date</th><th>Type</th><th>Stock</th><th>Quantity</th><th>Price</th></tr></thead><tbody>{transactions.map(transaction => <tr key={transaction.id}><td>{new Date(transaction.transactionDate).toLocaleDateString('en-IN')}</td><td><span className={`transaction-pill ${transaction.transactionType.toLowerCase()}`}>{transaction.transactionType}</span></td><td><strong>{transaction.symbol}</strong><small>{transaction.companyName}</small></td><td>{transaction.quantity}</td><td>{money(transaction.price)}</td></tr>)}</tbody></table></div> : <p className="portfolio-empty">No transactions recorded.</p>}</div>
        </>}
      </section>
    </div>
  </div>
}
