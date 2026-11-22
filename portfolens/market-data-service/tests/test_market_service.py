from datetime import date
import pandas as pd
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.csv_manager import clean_history
import app.routes as routes

client = TestClient(app, headers={"X-Internal-Service-Key": "test-internal-key"})


@pytest.fixture(autouse=True)
def configure_test_internal_key(monkeypatch):
    monkeypatch.setattr(routes, "MARKET_DATA_SERVICE_KEY", "test-internal-key")


def test_stock_master_lists_only_bse_symbols():
    response = client.get("/api/market/stocks")
    assert response.status_code == 200
    assert response.json()
    assert all(stock["exchange"] == "BSE" for stock in response.json())


def test_unknown_symbol_is_rejected():
    assert client.get("/api/market/stocks/NOTREAL/history").status_code == 404


def test_history_cleaning_drops_missing_rows_and_duplicate_dates():
    dates = pd.to_datetime(["2025-01-02", "2025-01-01", "2025-01-02"])
    frame = pd.DataFrame({"Open": [2, 1, 3], "High": [2, 1, 3], "Low": [2, 1, 3], "Close": [2, 1, 3], "Adj Close": [2, 1, 3], "Volume": [20, 10, 30]}, index=dates)
    cleaned = clean_history(frame)
    assert list(cleaned["date"]) == [date(2025, 1, 1), date(2025, 1, 2)]
    assert cleaned.iloc[-1]["close"] == 3


def test_history_cleaning_returns_empty_for_empty_download():
    assert clean_history(pd.DataFrame()).empty


def test_sensex_series_returns_cached_daily_values(monkeypatch):
    today = date.today()
    frame = pd.DataFrame({"date": [today], "close": [72000.0]})
    monkeypatch.setattr(routes, "load_history", lambda _: (frame.copy(), {"source": "Yahoo Finance"}))
    response = client.get("/api/market/series/sensex/history?period=1y")
    assert response.status_code == 200
    assert response.json()["records"] == [{"date": today.isoformat(), "value": 72000.0}]


def test_gsec_series_uses_configured_fallback_when_yahoo_is_unavailable(monkeypatch):
    monkeypatch.setattr(routes, "load_history", lambda _: (pd.DataFrame(columns=["date", "close"]), {}))
    monkeypatch.setattr(routes, "fetch_ticker_history", lambda *args, **kwargs: (_ for _ in ()).throw(RuntimeError("no ticker")))
    monkeypatch.setattr(routes, "RISK_FREE_ANNUAL_RATE", "7")
    response = client.get("/api/market/series/gsec10y/latest")
    assert response.status_code == 200
    assert response.json()["value"] == 0.07
    assert response.json()["source"] == "configured fallback"


def test_market_data_internal_key_blocks_direct_client_access(monkeypatch):
    monkeypatch.setattr(routes, "MARKET_DATA_SERVICE_KEY", "internal-secret")
    assert client.get("/api/market/stocks", headers={"X-Internal-Service-Key": "wrong-key"}).status_code == 401
    assert client.get("/api/market/stocks", headers={"X-Internal-Service-Key": "internal-secret"}).status_code == 200
    assert client.get("/api/market/health").status_code == 200
