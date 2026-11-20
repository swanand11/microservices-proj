from datetime import date
import pandas as pd
from fastapi.testclient import TestClient
from app.main import app
from app.csv_manager import clean_history

client = TestClient(app)


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
