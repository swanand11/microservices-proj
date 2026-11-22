from datetime import datetime, timezone
from pathlib import Path
import json
import pandas as pd
from .config import HISTORY_DIR

PRICE_COLUMNS = ["date", "open", "high", "low", "close", "adjusted_close", "volume"]


def clean_history(frame: pd.DataFrame) -> pd.DataFrame:
    if frame is None or frame.empty:
        return pd.DataFrame(columns=PRICE_COLUMNS)
    result = frame.copy()
    if "Date" in result.columns:
        result = result.rename(columns={"Date": "date"})
    if "Adj Close" in result.columns:
        result = result.rename(columns={"Adj Close": "adjusted_close"})
    if "Adj Close" in result.columns:
        result = result.drop(columns=["Adj Close"])
    if "date" not in result.columns:
        result.index.name = "date"
        result = result.reset_index()
    rename = {"Open": "open", "High": "high", "Low": "low", "Close": "close", "Volume": "volume"}
    result = result.rename(columns=rename)
    if "adjusted_close" not in result.columns:
        result["adjusted_close"] = result.get("close")
    for column in PRICE_COLUMNS[1:]:
        if column not in result.columns:
            result[column] = pd.NA
    result["date"] = pd.to_datetime(result["date"], errors="coerce", utc=True).dt.date
    for column in PRICE_COLUMNS[1:]:
        result[column] = pd.to_numeric(result[column], errors="coerce")
    result = result.dropna(subset=["date", "open", "high", "low", "close", "adjusted_close", "volume"])
    result = result.drop_duplicates(subset=["date"], keep="last").sort_values("date")
    result["volume"] = result["volume"].astype("int64")
    return result[PRICE_COLUMNS].reset_index(drop=True)


def history_path(symbol: str) -> Path:
    # Symbols are checked against stock master by routes; sanitize defensively too.
    safe = "".join(c for c in symbol.upper() if c.isalnum() or c in "-_" )
    return HISTORY_DIR / f"{safe}.csv"


def save_history(symbol: str, frame: pd.DataFrame, source: str = "Yahoo Finance") -> tuple[Path, int]:
    cleaned = clean_history(frame)
    if cleaned.empty:
        raise ValueError("Refusing to replace history with an empty download")
    HISTORY_DIR.mkdir(parents=True, exist_ok=True)
    path = history_path(symbol)
    temp = path.with_suffix(".csv.tmp")
    cleaned.to_csv(temp, index=False, date_format="%Y-%m-%d", float_format="%.8f")
    temp.replace(path)
    metadata = {"source": source, "downloaded_at": datetime.now(timezone.utc).isoformat(), "price_policy": "auto_adjust=False; raw OHLC/Close and Yahoo Adj Close kept separately"}
    path.with_suffix(".metadata.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    try:
        from .pg_store import save_history as save_postgres_history
        save_postgres_history(symbol, cleaned, source)
    except Exception:
        # CSV remains the local cache if the optional shared database is not reachable.
        pass
    return path, len(cleaned)


def load_history(symbol: str) -> tuple[pd.DataFrame, dict]:
    try:
        from .pg_store import load_history as load_postgres_history
        database_result = load_postgres_history(symbol)
        if database_result is not None:
            return database_result
    except Exception:
        pass
    path = history_path(symbol)
    if not path.exists():
        return pd.DataFrame(columns=PRICE_COLUMNS), {}
    frame = pd.read_csv(path, parse_dates=["date"])
    metadata_path = path.with_suffix(".metadata.json")
    metadata = json.loads(metadata_path.read_text(encoding="utf-8")) if metadata_path.exists() else {}
    return frame, metadata
