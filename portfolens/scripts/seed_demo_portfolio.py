"""Seed a local demo account through the authenticated Spring API."""

import csv
from datetime import date, datetime, time, timezone
import json
import os
from pathlib import Path
import time as sleep_time
from urllib.error import HTTPError
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
API_URL = os.getenv("PORTFOLENS_API_URL", "http://localhost:8000").rstrip("/")
EMAIL = os.getenv("PORTFOLENS_DEMO_EMAIL", "demo@portfolens.test").strip().lower()
PASSWORD = os.getenv("PORTFOLENS_DEMO_PASSWORD", "")
PORTFOLIO_NAME = "PortfoLens synthetic demo"
PORTFOLIO_DESCRIPTION = "DEMO ONLY: generated synthetic stock prices; metrics are illustrative, not real market performance."


def api(method, path, payload=None, token=None, headers=None):
    request_headers = {"Accept": "application/json"}
    if payload is not None:
        request_headers["Content-Type"] = "application/json"
    if token:
        request_headers["Authorization"] = f"Bearer {token}"
    if headers:
        request_headers.update(headers)
    body = json.dumps(payload).encode() if payload is not None else None
    request = Request(f"{API_URL}{path}", data=body, headers=request_headers, method=method)
    try:
        with urlopen(request, timeout=25) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except HTTPError as error:
        detail = error.read().decode(errors="replace")
        if error.code == 409 and path == "/api/auth/register":
            return None
        raise RuntimeError(f"{method} {path} returned {error.code}: {detail[:300]}") from error


def login():
    if len(PASSWORD) < 8:
        raise SystemExit("Set PORTFOLENS_DEMO_PASSWORD to a local demo password of at least 8 characters.")
    api("POST", "/api/auth/register", {"fullName": "PortfoLens Demo Investor", "email": EMAIL, "password": PASSWORD})
    result = api("POST", "/api/auth/login", {"email": EMAIL, "password": PASSWORD})
    return result["accessToken"]


def stock_closes(symbol):
    path = ROOT / "market-data-service" / "data" / "historical_prices" / f"{symbol}.csv"
    with path.open(newline="", encoding="utf-8") as source:
        return {date.fromisoformat(row["date"]): float(row["close"]) for row in csv.DictReader(source)}


def next_available_date(closes, requested):
    return next((day for day in sorted(closes) if day >= requested), None)


def main():
    for symbol in ("INFY", "TCS", "HDFCBANK"):
        path = ROOT / "market-data-service" / "data" / "historical_prices" / f"{symbol}.csv"
        metadata_path = path.with_suffix(".metadata.json")
        if not path.exists() or not metadata_path.exists():
            raise SystemExit("Run market-data-service/scripts/seed_synthetic_demo.py before this script.")

    token = login()
    stocks = api("GET", "/api/stocks", token=token)
    stock_ids = {stock["symbol"]: stock["id"] for stock in stocks}

    portfolios = api("GET", "/api/portfolios", token=token)
    portfolio = next((item for item in portfolios if item["name"] == PORTFOLIO_NAME), None)
    if portfolio is None:
        portfolio = api("POST", "/api/portfolios", {
            "name": PORTFOLIO_NAME,
            "description": PORTFOLIO_DESCRIPTION,
        }, token=token)
        print(f"Created demo portfolio: {portfolio['id']}")

    requested = [
        ("2025-12-01", "INFY", "BUY", 10),
        ("2026-02-02", "TCS", "BUY", 5),
        ("2026-05-04", "HDFCBANK", "BUY", 8),
        ("2026-08-03", "INFY", "SELL", 3),
        ("2026-10-05", "TCS", "BUY", 2),
        ("2026-11-16", "INFY", "BUY", 2),
    ]
    for index, (requested_day, symbol, action, quantity) in enumerate(requested, start=1):
        closes = stock_closes(symbol)
        trading_day = next_available_date(closes, date.fromisoformat(requested_day))
        if trading_day is None:
            raise SystemExit(f"No demo price exists on or after {requested_day} for {symbol}.")
        timestamp = datetime.combine(trading_day, time(10, 0), timezone.utc).isoformat().replace("+00:00", "Z")
        api("POST", f"/api/portfolios/{portfolio['id']}/transactions", {
            "stockId": stock_ids[symbol],
            "transactionType": action,
            "quantity": quantity,
            "price": round(closes[trading_day], 2),
            "transactionDate": timestamp,
        }, token=token, headers={"Idempotency-Key": f"synthetic-demo-{symbol.lower()}-{action.lower()}-{index}"})
        print(f"Recorded {action} {quantity} {symbol} on {trading_day}")

    metrics = None
    for _ in range(30):
        try:
            metrics = api("GET", f"/api/analytics/portfolios/{portfolio['id']}", token=token)
            if metrics.get("status") == "READY" or metrics.get("status") == "DATA_UNAVAILABLE":
                break
            if metrics.get("status") == "INSUFFICIENT_HISTORY" and metrics.get("observations", 0) >= 30:
                break
        except (OSError, RuntimeError):
            pass
        sleep_time.sleep(1)
    if not metrics:
        print(f"Analytics will appear when the subscriber is available ({portfolio['id']}).")
    else:
        print(f"Analytics status: {metrics.get('status')} ({metrics.get('observations', 'unknown')} observations): {metrics.get('message') or 'metrics calculated'}")
    print(f"Demo login: {EMAIL} (password is the PORTFOLENS_DEMO_PASSWORD value you supplied)")
    print(f"Portfolio ID: {portfolio['id']}")


if __name__ == "__main__":
    main()
