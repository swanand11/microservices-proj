from datetime import date, datetime, timezone
from fastapi import APIRouter, HTTPException, Query
import pandas as pd
from app.collector import download_bulk, download_stock
from app.config import MAX_HISTORY_RECORDS
from app.csv_manager import load_history
from app.schemas import BulkDownloadRequest, DownloadRequest, HistoryOut, PriceRecord, StockOut
from app.stock_master import load_stocks, resolve_stock

router = APIRouter(prefix="/api/market")
VALID_PERIODS = {"1mo", "3mo", "6mo", "1y", "2y", "5y", "max"}


def _stock_or_404(symbol: str):
    stock = resolve_stock(symbol)
    if stock is None or not stock.active:
        raise HTTPException(status_code=404, detail="Supported active stock not found")
    return stock


@router.get("/health")
def health():
    return {"status": "ok", "service": "market-data", "timestamp": datetime.now(timezone.utc)}


@router.get("/stocks", response_model=list[StockOut])
def stocks(q: str | None = None, sector: str | None = None):
    values = [s for s in load_stocks().values() if s.active]
    if q:
        term = q.strip().casefold()
        values = [s for s in values if term in s.symbol.casefold() or term in s.company_name.casefold()]
    if sector:
        values = [s for s in values if (s.sector or "").casefold() == sector.strip().casefold()]
    return [StockOut(symbol=s.symbol, company_name=s.company_name, yahoo_ticker=s.yahoo_ticker, exchange=s.exchange, sector=s.sector, industry=s.industry, active=s.active) for s in sorted(values, key=lambda item: item.symbol)]


@router.get("/stocks/{symbol}/history", response_model=HistoryOut)
def history(symbol: str, start: date | None = None, end: date | None = None, period: str | None = None, offset: int = Query(0, ge=0), limit: int = Query(500, ge=1, le=MAX_HISTORY_RECORDS)):
    stock = _stock_or_404(symbol)
    if start and end and start > end:
        raise HTTPException(status_code=422, detail="start must be on or before end")
    if period and period not in VALID_PERIODS:
        raise HTTPException(status_code=422, detail=f"period must be one of {sorted(VALID_PERIODS)}")
    frame, metadata = load_history(stock.symbol)
    if frame.empty:
        return HistoryOut(symbol=stock.symbol, company_name=stock.company_name, yahoo_ticker=stock.yahoo_ticker, exchange=stock.exchange, data_start_date=None, data_end_date=None, last_updated=None, source=None, total_records=0, offset=offset, limit=limit, records=[])
    frame["date"] = pd.to_datetime(frame["date"]).dt.date
    if start: frame = frame[frame["date"] >= start]
    if end: frame = frame[frame["date"] <= end]
    if period and not start and not end:
        days = {"1mo": 31, "3mo": 93, "6mo": 186, "1y": 366, "2y": 732, "5y": 1830}.get(period)
        if days:
            frame = frame[frame["date"] >= (date.today() - pd.Timedelta(days=days)).date()]
    frame = frame.sort_values("date")
    total = len(frame)
    sliced = frame.iloc[offset:offset + limit]
    records = [PriceRecord(**row) for row in sliced.to_dict(orient="records")]
    all_frame, _ = load_history(stock.symbol)
    all_dates = pd.to_datetime(all_frame["date"]).dt.date if not all_frame.empty else []
    updated = metadata.get("downloaded_at")
    return HistoryOut(symbol=stock.symbol, company_name=stock.company_name, yahoo_ticker=stock.yahoo_ticker, exchange=stock.exchange, data_start_date=min(all_dates) if len(all_dates) else None, data_end_date=max(all_dates) if len(all_dates) else None, last_updated=datetime.fromisoformat(updated) if updated else None, source=metadata.get("source"), price_policy=metadata.get("price_policy", "Raw OHLC/Close and adjusted close are separate."), total_records=total, offset=offset, limit=limit, records=records)


@router.get("/stocks/{symbol}/latest")
def latest(symbol: str):
    result = history(symbol, limit=1, offset=0)
    if not result.records:
        raise HTTPException(status_code=404, detail="No historical price data available")
    record = result.records[-1]
    return {"symbol": result.symbol, "company_name": result.company_name, "yahoo_ticker": result.yahoo_ticker, "exchange": result.exchange, "last_updated": result.last_updated, "price": record}


@router.post("/download/{symbol}")
def download(symbol: str, request: DownloadRequest):
    stock = _stock_or_404(symbol)
    if request.period not in VALID_PERIODS:
        raise HTTPException(status_code=422, detail="Unsupported period")
    result = download_stock(stock, period=request.period, refresh=request.refresh)
    return result


@router.post("/download/bulk")
def download_many(request: BulkDownloadRequest):
    symbols = [s.strip().upper() for s in request.symbols] or [s.symbol for s in load_stocks().values() if s.active]
    if request.period not in VALID_PERIODS:
        raise HTTPException(status_code=422, detail="Unsupported period")
    stock_map = load_stocks()
    unknown = sorted(set(symbols) - set(stock_map))
    if unknown:
        raise HTTPException(status_code=422, detail=f"Unknown supported symbols: {', '.join(unknown)}")
    selected = [stock_map[symbol] for symbol in dict.fromkeys(symbols) if stock_map[symbol].active]
    return {"results": download_bulk(selected, period=request.period, refresh=request.refresh)}


@router.get("/data/status")
def status():
    values = []
    for stock in load_stocks().values():
        frame, metadata = load_history(stock.symbol)
        values.append({"symbol": stock.symbol, "company_name": stock.company_name, "available": not frame.empty, "records": len(frame), "start_date": str(frame["date"].min().date()) if not frame.empty else None, "end_date": str(frame["date"].max().date()) if not frame.empty else None, "last_updated": metadata.get("downloaded_at")})
    return {"stocks": values, "available_count": sum(1 for value in values if value["available"])}
