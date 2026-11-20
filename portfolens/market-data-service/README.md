# PortfoLens Market Data Service

Independent FastAPI service for daily historical BSE prices. It stores Yahoo Finance downloads as CSV and serves those records to Spring Boot. Data is historical, may be delayed, and is not a guaranteed real-time feed.

## Price policy

The collector calls yfinance with `auto_adjust=False` and `actions=False`. CSV/API `open`, `high`, `low`, and `close` are Yahoo's unadjusted OHLC series; `adjusted_close` is Yahoo's `Adj Close` series. Keep these series distinct in downstream calculations. Dates with no trading data remain absent; missing sessions are never filled with zero prices or returns. Yahoo data can be revised, so metadata records source and fetch time.

The checked-in stock master contains the project's ten sample BSE names with candidate `.BO` ticker strings. Confirm support with `python scripts/download_history.py --validate` before treating a mapping as available; Yahoo may not provide history for every candidate. Add larger CSV rows only after checking the ticker and symbol mapping.

## Run

```sh
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python scripts/download_history.py --validate
uvicorn app.main:app --reload --port 8001
```

Set `MARKET_DATA_DIR` to change the private storage root and `MARKET_REQUEST_DELAY_SECONDS` to tune conservative bulk pacing. History download APIs only accept stock-master symbols and bulk is limited to 50 symbols.

## API

- `GET /api/market/health`
- `GET /api/market/stocks?q=&sector=`
- `GET /api/market/stocks/{symbol}/history?start=&end=&period=&offset=&limit=`
- `GET /api/market/stocks/{symbol}/latest`
- `POST /api/market/download/{symbol}`
- `POST /api/market/download/bulk`
- `GET /api/market/data/status`

## Tests

`pytest` runs offline tests using local stock master and mock frames; tests do not contact Yahoo Finance.
