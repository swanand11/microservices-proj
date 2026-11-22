# PortfoLens — BSE Portfolio Analytics

PortfoLens is a BSE portfolio application with JWT-authenticated portfolio management, historical market data, and a separate asynchronous analytics service.

## Project structure

```text
portfolens/
├── backend/     Spring Boot 3 / Java 21 API
├── frontend/    React / TypeScript / Vite application
├── analytics-service/ Java 21 / Spring Boot RabbitMQ subscriber
├── market-data-service/ Python / FastAPI and Yahoo Finance history
└── README.md
```

## Architecture

The Spring backend is the single client-facing API gateway. It owns authentication and portfolio operations, and applies bearer JWT authentication to every route except registration and login. It retains synchronous HTTP communication with the market-data service. Portfolio buys and sells write the transaction, update holdings, and insert a RabbitMQ outbox event in one PostgreSQL transaction. The outbox publisher sends confirmed events to the Analytics service, which deduplicates transactions and owns its analytics tables.

The project uses one Supabase PostgreSQL instance for course-project practicality. The core group owns the account/portfolio tables and its `historical_prices` read cache; Market Data owns `market_data_prices`; Analytics owns `analytics_transaction_events` and `portfolio_analytics`. These are service-owned table groups in one shared database, not isolated per-service database instances.

The frontend is an independent Vite app. Its auth context restores the current user and access token from tab-scoped session storage, Axios adds the bearer token, and React Router protects workspace routes.

## Requirements

- Java 21 and Maven 3.9+
- Node.js 20+ and npm
- Supabase project with PostgreSQL enabled

## Supabase and environment setup

In Supabase, open **Project Settings → Database → Connection string** and use the PostgreSQL URI/connection values. Prefer the session pooler URI when your network or runtime needs IPv4 compatibility. Require SSL (`sslmode=require`). Do not put a real database password or JWT signing key in Git.

Create local environment values from the examples:

```sh
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```

The backend loads `backend/.env` through the `spring-dotenv` dependency when started from the backend project directory. Values supplied by the operating system take precedence. Replace placeholders with values from Supabase. Generate a Base64 JWT key at least 32 bytes long, for example:

```sh
openssl rand -base64 32
```

Backend variables:

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | Complete JDBC URL, e.g. `jdbc:postgresql://<host>:5432/postgres?sslmode=require`. If the supplied value is a `postgres://` URI, set `DATABASE_URL_JDBC` to its equivalent JDBC URL. |
| `host`, `port`, `database` | Existing Supabase connection parts used to construct a JDBC URL when `DATABASE_URL` is unset. |
| `user` | Supabase database username |
| `DB_PASSWORD` | Supabase database password. Add this locally if it is not already in `.env`; never commit the value. |
| `DB_SCHEMA` | Hibernate schema, defaults to `app`; the database role must have access to it. |
| `JWT_SECRET` | Base64 encoded random signing key, minimum 32 decoded bytes |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds; default 900000 (15 minutes) |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins; default `http://localhost:5173` |
| `SERVER_PORT` | Optional API port; default 8000 |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | RabbitMQ connection; defaults to local guest credentials |
| `OUTBOX_POLL_MS` | Outbox publisher interval in milliseconds; default 1000 |
| `ANALYTICS_SERVICE_URL` | Internal analytics API URL; default `http://localhost:8002` |
| `ANALYTICS_SERVICE_KEY` | Shared backend-to-analytics key; configure the same value in both services |
| `MARKET_DATA_SERVICE_URL` | Market-data service URL; default `http://localhost:8001` |
| `MARKET_DATA_SERVICE_KEY` | Shared internal key required by FastAPI for non-health routes; configure in all three services |
| `SENSEX_YAHOO_TICKER` | Yahoo Finance equity benchmark ticker; default `^BSESN` |
| `RBI_WSS_CACHE_HOURS` | How long the market-data service caches RBI weekly G-Sec observations; default `168` |
| `ANALYTICS_RISK_FREE_ANNUAL_RATE`, `MARKET_RISK_FREE_ANNUAL_RATE` | Optional annual-rate fallback (decimal, e.g. `0.07`, or percent, e.g. `7`) |

Frontend variable: `VITE_API_BASE_URL`, the API origin (default `http://localhost:8000`). Vite variables are public and must not contain secrets.

For quick shell setup, export values before `mvn spring-boot:run`, for example:

```sh
export DATABASE_URL='jdbc:postgresql://<host>:5432/postgres?sslmode=require'
export user='<username>' DB_PASSWORD='<password>'
export JWT_SECRET="$(openssl rand -base64 32)"
export JWT_EXPIRATION_MS=900000 APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

## Run locally

Start the API after setting its environment variables:

```sh
cd backend
mvn spring-boot:run
```

In another terminal:

```sh
cd frontend
npm install
npm run dev
```

Open the URL Vite prints (normally `http://localhost:5173`). The API listens on port 8000. Hibernate schema update is enabled for this student development foundation; use reviewed database migrations before production and avoid automatic schema changes against important data.

Start RabbitMQ locally from the project root with `docker compose up -d rabbitmq`. Run the Analytics service in another terminal with the same database, RabbitMQ, and `ANALYTICS_SERVICE_KEY` environment values:

```sh
cd analytics-service
mvn spring-boot:run
```

The Analytics service listens on port 8002. Its internal portfolio endpoint requires `X-Internal-Service-Key`; browser requests go through the Spring gateway at `/api/analytics/portfolios/{id}`, which verifies JWT authentication and portfolio ownership.

## API

All JSON endpoints use UTF-8 JSON. Validation failures and application errors follow this format:

```json
{
  "timestamp": "2025-01-01T00:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```

| Method and path | Auth | Purpose |
|---|---|---|
| `POST /api/auth/register` | Public | Create a user; `fullName`, `email`, `password` (min 8 chars). Returns profile only, HTTP 201. |
| `POST /api/auth/login` | Public | Verify credentials. Returns `accessToken`, `tokenType`, `expiresInSeconds`, and safe user profile. |
| `GET /api/users/me` | Bearer JWT | Return authenticated user profile. |
| `PUT /api/users/me` | Bearer JWT | Update `fullName`; returns the safe profile. |

Example login request:

```json
{ "email": "student@example.com", "password": "example-password" }
```

Example protected request header: `Authorization: Bearer <accessToken>`. A missing or invalid token returns HTTP 401 with `UNAUTHORIZED`; incorrect credentials return `INVALID_CREDENTIALS`. Password hashes are never included in API responses.

## Frontend routes

- `/login` and `/register` are public.
- `/dashboard` and `/profile` require authentication; unauthenticated visitors go to login.
- Successful login and registration go to the dashboard. The dashboard contains placeholders only.
- Logout clears the tab-scoped token and user.

## Historical market data

The separate `market-data-service/` Python application downloads daily historical BSE prices from Yahoo Finance, retains CSV files locally, and exposes a FastAPI API. Its checked-in stock master contains candidate `.BO` tickers for the initial sample universe; verify mappings using `python scripts/download_history.py --validate` before relying on availability. The collector keeps unadjusted OHLC/close separate from Yahoo's adjusted close (`auto_adjust=False`). It does not fill missing trading days or fabricate unavailable prices.

Start the Python service from `market-data-service/` with `uvicorn app.main:app --reload --port 8001`. The Spring backend calls it using `MARKET_DATA_SERVICE_URL` (default `http://localhost:8001`). Authenticated market reads are available through Spring at `/api/market/stocks`, `/api/market/stocks/{symbol}/history`, and `/api/market/stocks/{symbol}/latest`. Open the protected **Market explorer** page to search the sample stock master and chart imported daily closes. Prices are historical and may be delayed.

To import prices, configure a private `MARKET_SYNC_KEY` in the backend environment and send it as `X-Market-Sync-Key` when calling `POST /api/market/sync/{symbol}` or `/api/market/sync/bulk`. Sync is disabled when the key is unset. Do not put this operator key in frontend code. Hibernate creates the `historical_prices` table in the configured database schema on startup.

The market-data service also serves Sensex history and the weekly 10-year G-Sec par yield at `/api/market/series/sensex/history` and `/api/market/series/gsec10y/history`. Sensex comes from Yahoo Finance; G-Sec comes from RBI's public Weekly Statistical Supplement (the "10-Year G-Sec Par Yield (FBIL)" row). The service stores observations in the shared `market_data_series` table and its local CSV cache, refreshing the RBI source every seven days by default. The latest available weekly observation is used as the annual risk-free rate. Set `MARKET_RISK_FREE_ANNUAL_RATE` only as a fallback for periods when the RBI source and cache are unavailable.

The Analytics service computes annualized covariance, beta, Jensen alpha, and Sharpe ratio from aligned daily portfolio and Sensex returns. It requires at least 30 aligned observations; otherwise it reports insufficient history. The portfolio page displays the returned metrics.

For a local demonstration without depending on ticker-download availability, run `python market-data-service/scripts/seed_synthetic_demo.py` to generate labelled synthetic INFY, TCS, and HDFCBANK histories, then `python scripts/sync_demo_prices.py` to bulk-import those rows into the backend's `historical_prices` cache using the credentials in `backend/.env`. Finally set `PORTFOLENS_DEMO_PASSWORD` and run `python scripts/seed_demo_portfolio.py`; it creates a dedicated `demo@portfolens.test` account and synthetic portfolio, records dated transactions, and checks analytics status. Synthetic prices are explicitly labelled in the Market Explorer and must not be presented as real market observations or used for investment decisions.

Every buy/sell request must include an `Idempotency-Key`. The UI reuses the key when retrying the same trade, so the backend returns the original transaction rather than applying it twice. Direct holding-mutation routes are not exposed; position changes use buy/sell transactions so Analytics receives the full event stream.

## Token storage and security notes

The API returns a short-lived bearer JWT. The frontend keeps it in `sessionStorage` so refreshing a tab preserves the session; closing the tab clears it. The app reloads the profile on startup and clears the stored session after an unauthorized response. JavaScript can access session storage, so an XSS vulnerability can access the token; use a hardened content security policy and consider an HttpOnly cookie session before production deployment.

For a production browser session, prefer a Secure, HttpOnly, SameSite cookie and explicitly design CSRF protection, cookie lifetime/refresh, CORS, and logout invalidation. Do not simply move the bearer token into a cookie while leaving CSRF disabled. The current stateless bearer setup disables CSRF because credentials are sent explicitly in the Authorization header.

Additional deployment work before production includes migrations, rate limiting, TLS, secret management/rotation, security review, and an account recovery flow. The frontend currently has no token refresh endpoint.
