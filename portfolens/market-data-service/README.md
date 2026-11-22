# PortfoLens Market Data Service

Independent FastAPI service for daily historical BSE prices. It stores Yahoo Finance downloads as CSV and serves those records to Spring Boot. Data is historical, may be delayed, and is not a guaranteed real-time feed.

## Price policy

The collector calls yfinance with `auto_adjust=False` and `actions=False`. CSV/API `open`, `high`, `low`, and `close` are Yahoo's unadjusted OHLC series; `adjusted_close` is Yahoo's `Adj Close` series. Keep these series distinct in downstream calculations. Dates with no trading data remain absent; missing sessions are never filled with zero prices or returns. Yahoo data can be revised, so metadata records source and fetch time.

The checked-in stock master contains the project's ten sample BSE names with candidate `.BO` ticker strings. Confirm support with `python scripts/download_history.py --validate` before treating a mapping as available; Yahoo may not provide history for every candidate. Add larger CSV rows only after checking the ticker and symbol mapping.

## Synthetic demo data

For a repeatable UI demo without depending on Yahoo downloads, run `python scripts/seed_synthetic_demo.py`. It creates deterministic INFY, TCS, and HDFCBANK histories aligned to the checked-in Sensex dates. Each file is marked `Synthetic demo data` and includes metadata stating that its prices are generated, not observed market prices. The script refuses to replace existing history unless that file is already marked synthetic. Keep this dataset confined to demos; its prices and resulting analytics are illustrative only.

## Run

```sh
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python scripts/download_history.py --validate
uvicorn app.main:app --reload --port 8001
```

Set `MARKET_DATA_DIR` to change the private storage root and `MARKET_REQUEST_DELAY_SECONDS` to tune conservative bulk pacing. History download APIs only accept stock-master symbols and bulk is limited to 50 symbols.

Set `MARKET_DATA_SERVICE_KEY` in the market-data service, Spring backend, and Analytics service to the same private value. The key is required: protected market-data routes fail closed when it is unset and require `X-Internal-Service-Key`; only `/api/market/health` remains public. Clients should reach market data through the authenticated Spring gateway.

## API

- `GET /api/market/health`
- `GET /api/market/stocks?q=&sector=`
- `GET /api/market/stocks/{symbol}/history?start=&end=&period=&offset=&limit=`
- `GET /api/market/stocks/{symbol}/latest`
- `POST /api/market/download/{symbol}`
- `POST /api/market/download/bulk`
- `GET /api/market/data/status`
- `GET /api/market/series/sensex/history?period=1y`
- `GET /api/market/series/gsec10y/history?period=1y`
- `GET /api/market/series/gsec10y/latest`

The Sensex series is fetched from Yahoo Finance using `^BSESN` by default. The 10-year G-Sec series uses the Reserve Bank of India's public Weekly Statistical Supplement (WSS), specifically its "10-Year G-Sec Par Yield (FBIL)" row. The adapter discovers the latest WSS issue, saves its dated weekly observations, and refreshes the cache every seven days (`RBI_WSS_CACHE_HOURS` can change that interval). The source is weekly, so the latest available observation can be several days old. If RBI is temporarily unavailable, the service serves its last stored observations. If no observations have ever been stored, `MARKET_RISK_FREE_ANNUAL_RATE` can provide an annual rate in decimal or percent form. Analytics uses the latest available G-Sec observation as an annualized risk-free rate.

When `DATABASE_URL` (or `MARKET_DATABASE_URL`) is configured, downloaded equity and Sensex rows are upserted into the market-data-owned `market_data_prices` table, while macro series such as G-Sec yields are stored in `market_data_series`. Both tables use the shared PostgreSQL database. Local CSV caches remain available when PostgreSQL is unavailable, and database values are loaded preferentially when present.

## Tests

`pytest` runs offline tests using local stock master and mock frames; tests do not contact Yahoo Finance.
