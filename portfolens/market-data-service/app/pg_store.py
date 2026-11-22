"""PostgreSQL-owned market-history table for the shared Supabase deployment."""
import os
from datetime import date, datetime, timezone
import logging
import pandas as pd

logger = logging.getLogger(__name__)


def _connect():
    url = os.getenv("MARKET_DATABASE_URL") or os.getenv("DATABASE_URL", "")
    if url.startswith("jdbc:"):
        url = url[len("jdbc:"):]
    if url.startswith("postgres://"):
        url = "postgresql://" + url[len("postgres://"):]
    try:
        import psycopg
    except ImportError:
        logger.warning("psycopg is not installed; using the CSV market-data cache only")
        return None
    if url:
        return psycopg.connect(
            url,
            user=os.getenv("DB_USER") or os.getenv("DB_USERNAME"),
            password=os.getenv("DB_PASSWORD") or os.getenv("DATABASE_PASSWORD", ""),
            sslmode="require" if "sslmode=" not in url else None,
        )
    host = os.getenv("host", "")
    if not host:
        return None
    return psycopg.connect(
        host=host,
        port=os.getenv("port", "5432"),
        dbname=os.getenv("database", "postgres"),
        user=os.getenv("DB_USER") or os.getenv("user") or os.getenv("DB_USERNAME"),
        password=os.getenv("DATABASE_PASSWORD") or os.getenv("DB_PASSWORD", ""),
        sslmode="require",
    )


def _ensure_table(connection):
    connection.execute("""
        CREATE TABLE IF NOT EXISTS market_data_prices (
            series_key varchar(32) NOT NULL,
            trading_date date NOT NULL,
            open numeric(19, 8), high numeric(19, 8), low numeric(19, 8),
            close numeric(19, 8) NOT NULL,
            adjusted_close numeric(19, 8) NOT NULL,
            volume bigint NOT NULL DEFAULT 0,
            source varchar(80) NOT NULL,
            fetched_at timestamptz NOT NULL,
            PRIMARY KEY (series_key, trading_date)
        )
    """)


def save_history(series_key: str, frame: pd.DataFrame, source: str = "Yahoo Finance") -> bool:
    connection = _connect()
    if connection is None:
        return False
    now = datetime.now(timezone.utc)
    rows = [
        (series_key.upper(), row.date, row.open, row.high, row.low, row.close, row.adjusted_close,
         int(row.volume), source, now)
        for row in frame.itertuples(index=False)
    ]
    try:
        with connection:
            _ensure_table(connection)
            with connection.cursor() as cursor:
                cursor.executemany("""
                    INSERT INTO market_data_prices
                        (series_key, trading_date, open, high, low, close, adjusted_close, volume, source, fetched_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON CONFLICT (series_key, trading_date) DO UPDATE SET
                        open = EXCLUDED.open, high = EXCLUDED.high, low = EXCLUDED.low,
                        close = EXCLUDED.close, adjusted_close = EXCLUDED.adjusted_close,
                        volume = EXCLUDED.volume, source = EXCLUDED.source, fetched_at = EXCLUDED.fetched_at
                """, rows)
    finally:
        connection.close()
    return True


def load_history(series_key: str):
    connection = _connect()
    if connection is None:
        return None
    try:
        with connection:
            _ensure_table(connection)
            cursor = connection.execute("""
                SELECT trading_date, open, high, low, close, adjusted_close, volume, source, fetched_at
                FROM market_data_prices WHERE series_key = %s ORDER BY trading_date
            """, (series_key.upper(),))
            rows = cursor.fetchall()
    finally:
        connection.close()
    if not rows:
        return None
    frame = pd.DataFrame(rows, columns=["date", "open", "high", "low", "close", "adjusted_close", "volume", "source", "fetched_at"])
    metadata = {"source": str(frame.iloc[-1]["source"]), "downloaded_at": frame["fetched_at"].max().isoformat(),
                "price_policy": "Raw OHLC/Close and adjusted close are separate; persisted in market_data_prices."}
    return frame.drop(columns=["source", "fetched_at"]), metadata


def _ensure_series_table(connection):
    connection.execute("""
        CREATE TABLE IF NOT EXISTS market_data_series (
            series_key varchar(32) NOT NULL,
            observation_date date NOT NULL,
            value numeric(19, 8) NOT NULL,
            source varchar(120) NOT NULL,
            published_at date NOT NULL,
            fetched_at timestamptz NOT NULL,
            PRIMARY KEY (series_key, observation_date)
        )
    """)


def save_series(series_key: str, records, source: str, published_at: date) -> bool:
    connection = _connect()
    if connection is None:
        return False
    fetched_at = datetime.now(timezone.utc)
    rows = [(series_key.upper(), observation_date, value, source, published_at, fetched_at)
            for observation_date, value in records]
    try:
        with connection:
            _ensure_series_table(connection)
            with connection.cursor() as cursor:
                cursor.executemany("""
                    INSERT INTO market_data_series
                        (series_key, observation_date, value, source, published_at, fetched_at)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    ON CONFLICT (series_key, observation_date) DO UPDATE SET
                        value = EXCLUDED.value, source = EXCLUDED.source,
                        published_at = EXCLUDED.published_at, fetched_at = EXCLUDED.fetched_at
                """, rows)
    finally:
        connection.close()
    return True


def load_series(series_key: str):
    connection = _connect()
    if connection is None:
        return None
    try:
        with connection:
            cursor = connection.execute("""
                SELECT observation_date, value, source, published_at, fetched_at
                FROM market_data_series WHERE series_key = %s ORDER BY observation_date
            """, (series_key.upper(),))
            rows = cursor.fetchall()
    finally:
        connection.close()
    if not rows:
        return None
    records = [(row[0], float(row[1])) for row in rows]
    metadata = {
        "source": str(rows[-1][2]),
        "published_at": rows[-1][3].isoformat(),
        "fetched_at": max(row[4] for row in rows).isoformat(),
    }
    return records, metadata
