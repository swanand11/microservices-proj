from datetime import date
import time
import yfinance as yf
from app.config import DEFAULT_INTERVAL, DEFAULT_PERIOD, REQUEST_DELAY_SECONDS
from app.csv_manager import load_history, save_history
from app.stock_master import Stock


def fetch_ticker_history(yahoo_ticker: str, period: str = DEFAULT_PERIOD):
    ticker = yf.Ticker(yahoo_ticker)
    return ticker.history(period=period, interval=DEFAULT_INTERVAL, auto_adjust=False, actions=False, raise_errors=True)


def validate_ticker(stock: Stock) -> bool:
    try:
        data = fetch_ticker_history(stock.yahoo_ticker, period="5d")
        return data is not None and not data.empty
    except Exception:
        return False


def download_stock(stock: Stock, period: str = DEFAULT_PERIOD, refresh: bool = False) -> dict:
    existing, _ = load_history(stock.symbol)
    if not refresh and not existing.empty:
        return {"symbol": stock.symbol, "status": "already_available", "records": len(existing)}
    try:
        frame = fetch_ticker_history(stock.yahoo_ticker, period=period)
        path, count = save_history(stock.symbol, frame)
        return {"symbol": stock.symbol, "status": "downloaded", "records": count, "file": path.name}
    except ValueError as exc:
        return {"symbol": stock.symbol, "status": "unavailable", "records": 0, "message": str(exc)}
    except Exception as exc:
        return {"symbol": stock.symbol, "status": "failed", "records": 0, "message": type(exc).__name__}


def download_bulk(stocks: list[Stock], period: str = DEFAULT_PERIOD, refresh: bool = False) -> list[dict]:
    results = []
    for index, stock in enumerate(stocks):
        results.append(download_stock(stock, period=period, refresh=refresh))
        if index < len(stocks) - 1:
            time.sleep(max(0, REQUEST_DELAY_SECONDS))
    return results
