import { useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'

type Stock = { symbol: string; company_name: string; yahoo_ticker: string; exchange: string; sector: string; industry: string; active: boolean }
type Price = { symbol: string; company_name: string; trading_date: string; open: number; high: number; low: number; close: number; adjusted_close: number; volume: number; source: string; fetched_at: string }
const formatPrice = (value: number) => `₹${value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

function PriceChart({ prices }: { prices: Price[] }) {
  const values = prices.map(p => p.close)
  const min = Math.min(...values), max = Math.max(...values), range = max - min || 1
  const points = values.map((value, index) => `${(index / Math.max(values.length - 1, 1)) * 1000},${210 - ((value - min) / range) * 190}`).join(' ')
  const first = prices[0], last = prices[prices.length - 1]
  return <div className="market-chart">
    <svg viewBox="0 0 1000 240" role="img" aria-label={`Historical closing prices from ${first.trading_date} to ${last.trading_date}`} preserveAspectRatio="none">
      {[20, 70, 120, 170, 220].map(y => <line key={y} x1="0" x2="1000" y1={y} y2={y} stroke="#203129" strokeDasharray="4 6" />)}
      <polyline points={points} fill="none" stroke="#62d9a5" strokeWidth="3" vectorEffect="non-scaling-stroke" />
    </svg>
    <div className="market-chart-labels"><span>{new Date(`${first.trading_date}T00:00:00`).toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'})}</span><span>{formatPrice(max)}</span><span>{new Date(`${last.trading_date}T00:00:00`).toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'})}</span></div>
  </div>
}

export function MarketExplorerPage() {
  const [stocks, setStocks] = useState<Stock[]>([])
  const [query, setQuery] = useState('')
  const [symbol, setSymbol] = useState('')
  const [prices, setPrices] = useState<Price[]>([])
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { api.get<Stock[]>('/api/market/stocks').then(({ data }) => setStocks(data)).catch(() => setError('Market data service is unavailable.')) }, [])
  useEffect(() => {
    if (!symbol) { setPrices([]); return }
    setLoading(true); setError('')
    api.get<Price[]>(`/api/market/stocks/${encodeURIComponent(symbol)}/history`, { params: { start: start || undefined, end: end || undefined } }).then(({ data }) => setPrices(data)).catch(() => { setPrices([]); setError('Could not load historical data for this stock.') }).finally(() => setLoading(false))
  }, [symbol, start, end])
  const filtered = useMemo(() => { const q=query.trim().toLowerCase(); return stocks.filter(s => !q || s.symbol.toLowerCase().includes(q) || s.company_name.toLowerCase().includes(q)) }, [stocks,query])
  const selected = stocks.find(s => s.symbol === symbol)
  const latest = prices[prices.length - 1]

  return <div className="page-content market-page">
    <div className="page-heading"><div><div className="eyebrow">BSE HISTORICAL DATA <span className="live-dot" /></div><h1>Market <span className="accent">explorer.</span></h1><p>Explore daily historical prices from Yahoo Finance.</p></div><div className="date-chip">HISTORICAL · MAY BE DELAYED</div></div>
    <div className="market-layout"><aside className="portfolio-panel market-stock-panel"><label className="market-search-label">Search stocks<input value={query} onChange={e => setQuery(e.target.value)} placeholder="Symbol or company" /></label><div className="market-stock-list">{filtered.map(stock => <button key={stock.symbol} className={`portfolio-choice ${stock.symbol === symbol ? 'selected' : ''}`} onClick={() => setSymbol(stock.symbol)}><strong>{stock.symbol}</strong><span>{stock.company_name}</span></button>)}{!filtered.length && <p className="portfolio-empty">No supported stocks match that search.</p>}</div></aside>
      <section className="portfolio-main">{selected ? <>
        <div className="portfolio-panel"><div className="portfolio-panel-heading market-title"><div><div className="eyebrow">{selected.exchange} · {selected.sector}</div><h2>{selected.company_name}</h2><p>{selected.symbol} · Yahoo Finance ticker {selected.yahoo_ticker}</p></div>{latest && <div className="market-latest"><span>Latest available close</span><strong>{formatPrice(latest.close)}</strong><small>{latest.trading_date}</small></div>}</div></div>
        <div className="portfolio-panel"><div className="market-chart-heading"><div><h2>Historical close</h2><p>Unadjusted daily close · {prices.length} sessions</p></div><div className="market-date-fields"><label>From<input type="date" value={start} onChange={e => setStart(e.target.value)} /></label><label>To<input type="date" value={end} onChange={e => setEnd(e.target.value)} /></label></div></div>
          {loading ? <div className="market-empty"><span className="spinner"/> Loading historical prices…</div> : error ? <div className="market-empty">{error}</div> : prices.length ? <PriceChart prices={prices} /> : <div className="market-empty"><span className="stat-icon green">⌁</span><strong>No historical data available</strong><p>Price history has not been imported for this stock yet. Try again after the service has collected data.</p></div>}
          <div className="market-source-note">Source: Yahoo Finance · Historical data, not a real-time feed{latest ? ` · Last fetched ${new Date(latest.fetched_at).toLocaleString()}` : ''}</div>
        </div>
      </> : <div className="portfolio-empty-card"><div className="stat-icon green">⌁</div><h2>Select a BSE stock</h2><p>Choose a supported symbol to see available historical prices.</p></div>}</section>
    </div>
  </div>
}
