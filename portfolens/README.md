# PortfoLens — Phase 1

PortfoLens is a BSE portfolio analytics application in its first phase: a modular monolith foundation with account registration, login, JWT authentication, and a small authenticated profile/dashboard experience. Portfolio management, market data, calculations, and recommendations are intentionally out of scope.

## Project structure

```text
portfolens/
├── backend/     Spring Boot 3 / Java 21 API
├── frontend/    React / TypeScript / Vite application
└── README.md
```

## Architecture

The backend is one Spring Boot application with controller → service → repository layers. DTOs separate the API contract from persistence. The `security` package owns JWT token creation and request authentication; the `entity` package contains the JPA `User`. Keeping auth logic grouped makes a future service extraction possible without introducing microservices or a gateway in Phase 1.

The frontend is an independent Vite app. Its auth context keeps the current user and access token in memory, Axios adds the bearer token, and React Router protects dashboard/profile routes.

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
| `SERVER_PORT` | Optional API port; default 8080 |

Frontend variable: `VITE_API_BASE_URL`, the API origin (default `http://localhost:8080`). Vite variables are public and must not contain secrets.

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

Open the URL Vite prints (normally `http://localhost:5173`). The API listens on port 8080. Hibernate schema update is enabled for this student development foundation; use reviewed database migrations before production and avoid automatic schema changes against important data.

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
- Logout clears the in-memory token and user.

## Historical market data (Phase 3)

The separate `market-data-service/` Python application downloads daily historical BSE prices from Yahoo Finance, retains CSV files locally, and exposes a FastAPI API. Its checked-in stock master contains candidate `.BO` tickers for the initial sample universe; verify mappings using `python scripts/download_history.py --validate` before relying on availability. The collector keeps unadjusted OHLC/close separate from Yahoo's adjusted close (`auto_adjust=False`). It does not fill missing trading days or fabricate unavailable prices.

Start the Python service from `market-data-service/` with `uvicorn app.main:app --reload --port 8001`. The Spring backend calls it using `MARKET_DATA_SERVICE_URL` (default `http://localhost:8001`). Authenticated market reads are available through Spring at `/api/market/stocks`, `/api/market/stocks/{symbol}/history`, and `/api/market/stocks/{symbol}/latest`. Open the protected **Market explorer** page to search the sample stock master and chart imported daily closes. Prices are historical and may be delayed.

To import prices, configure a private `MARKET_SYNC_KEY` in the backend environment and send it as `X-Market-Sync-Key` when calling `POST /api/market/sync/{symbol}` or `/api/market/sync/bulk`. Sync is disabled when the key is unset. Do not put this operator key in frontend code. Hibernate creates the `historical_prices` table in the configured database schema on startup.

## Token storage and security notes

This first implementation returns a short-lived bearer JWT and stores it only in JavaScript memory. This avoids persistent token exposure through `localStorage`, but refreshing or closing the tab clears the session and requires another login. An XSS vulnerability can still access any token available to page JavaScript, so memory storage is not a complete production solution.

For a production browser session, prefer a Secure, HttpOnly, SameSite cookie and explicitly design CSRF protection, cookie lifetime/refresh, CORS, and logout invalidation. Do not simply move the bearer token into a cookie while leaving CSRF disabled. The current stateless bearer setup disables CSRF because credentials are sent explicitly in the Authorization header.

Additional deployment work before production includes migrations, rate limiting, TLS, secret management/rotation, security review, and an account recovery flow. The frontend currently has no token refresh endpoint.
