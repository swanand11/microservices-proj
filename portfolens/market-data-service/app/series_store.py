"""Durable storage for non-equity market series such as government yields."""
import csv
from datetime import datetime, timezone
import json
import logging
from pathlib import Path

from .config import SERIES_DIR

logger = logging.getLogger(__name__)


def _paths(series_key: str) -> tuple[Path, Path]:
    safe = "".join(char for char in series_key.lower() if char.isalnum() or char in "-_")
    return SERIES_DIR / f"{safe}.csv", SERIES_DIR / f"{safe}.metadata.json"


def save_series(series_key: str, records, source: str, published_at) -> dict:
    fetched_at = datetime.now(timezone.utc)
    SERIES_DIR.mkdir(parents=True, exist_ok=True)
    csv_path, metadata_path = _paths(series_key)
    temp_path = csv_path.with_suffix(".csv.tmp")
    with temp_path.open("w", newline="", encoding="utf-8") as output:
        writer = csv.writer(output)
        writer.writerow(["date", "value"])
        writer.writerows((observation_date.isoformat(), value) for observation_date, value in records)
    temp_path.replace(csv_path)
    metadata = {
        "source": source,
        "published_at": published_at.isoformat(),
        "fetched_at": fetched_at.isoformat(),
    }
    metadata_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    try:
        from .pg_store import save_series as save_postgres_series
        save_postgres_series(series_key, records, source, published_at)
    except Exception:
        # The local cache keeps the rate available if PostgreSQL is temporarily unreachable.
        logger.warning("Could not persist %s to PostgreSQL; using its local cache", series_key, exc_info=True)
    return metadata


def load_series(series_key: str):
    try:
        from .pg_store import load_series as load_postgres_series
        result = load_postgres_series(series_key)
        if result is not None:
            return result
    except Exception as exc:
        if getattr(exc, "sqlstate", None) == "42P01":
            logger.info("PostgreSQL series table is not created yet; checking the local cache")
        else:
            logger.warning("Could not load %s from PostgreSQL; checking its local cache", series_key, exc_info=True)
    csv_path, metadata_path = _paths(series_key)
    if not csv_path.exists():
        return [], {}
    with csv_path.open(newline="", encoding="utf-8") as source:
        rows = list(csv.DictReader(source))
    records = [(datetime.fromisoformat(row["date"]).date(), float(row["value"])) for row in rows]
    metadata = json.loads(metadata_path.read_text(encoding="utf-8")) if metadata_path.exists() else {}
    return records, metadata
