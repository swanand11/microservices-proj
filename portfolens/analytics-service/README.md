# PortfoLens Analytics Service

Java 21 / Spring Boot service that consumes portfolio buy/sell events from RabbitMQ and computes trailing-year risk/performance metrics.

## Local run

Start RabbitMQ with `docker compose up -d rabbitmq` from the `portfolens/` directory. Export the same Supabase connection variables used by the backend, set `ANALYTICS_SERVICE_KEY` to the same private value in both services, and start:

```sh
mvn spring-boot:run
```

The service listens on port 8002. It connects to the shared PostgreSQL instance but writes only its `analytics_transaction_events` and `portfolio_analytics` tables. Configure `MARKET_DATA_SERVICE_URL` to reach the FastAPI service.

## Event contract

The service consumes `PortfolioTransactionRecorded` messages from the durable `portfolens.analytics.transactions` queue, bound to `portfolens.events` with routing key `portfolio.transaction.recorded`. The core API writes events to its transactional outbox together with the trade and holding update. Analytics deduplicates by transaction ID before recording the event.

## Metrics API

The internal endpoint is `GET /internal/analytics/portfolios/{id}` and requires `X-Internal-Service-Key`. The Spring API exposes the client-facing `GET /api/analytics/portfolios/{id}` route after JWT authentication and portfolio ownership checks.

Metrics use daily adjusted-close prices and portfolio transaction history over the trailing year, aligned with BSE Sensex daily returns. Beta is daily sample covariance divided by daily benchmark sample variance. Annualized covariance is daily sample covariance multiplied by 252. Jensen alpha is annualized using 252 trading days, and Sharpe uses the square root of 252. The latest weekly 10-year G-Sec par yield (FBIL), published through RBI's free Weekly Statistical Supplement, is converted to a daily rate. At least 30 aligned observations are required.

## Tests

The JUnit suite covers metric calculations, insufficient history, and duplicate-event handling.
