"""Bulk-import labelled synthetic histories into the Spring API's price cache."""

import csv
from datetime import datetime, timezone
from pathlib import Path
import uuid

try:
    import psycopg
    from psycopg import sql
except ImportError:
    import psycopg2 as psycopg
    from psycopg2 import sql


ROOT = Path(__file__).resolve().parents[1]
SYMBOLS = ("INFY", "TCS", "HDFCBANK")
SOURCE = "Synthetic demo data"


def read_env(path):
    values = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "'\"":
            value = value[1:-1]
        values[key.strip()] = value
    return values


def connect():
    values = read_env(ROOT / "backend" / ".env")
    url = values.get("DATABASE_URL", "")
    if url.startswith("jdbc:"):
        url = url[len("jdbc:"):]
    if not url:
        raise SystemExit("DATABASE_URL is missing from backend/.env.")
    return psycopg.connect(
        url,
        user=values.get("DB_USER") or values.get("user"),
        password=values.get("DB_PASSWORD") or values.get("DATABASE_PASSWORD"),
        sslmode="require",
        connect_timeout=15,
    ), values.get("DB_SCHEMA", "public")


def read_rows(symbol):
    path = ROOT / "market-data-service" / "data" / "historical_prices" / f"{symbol}.csv"
    metadata = path.with_suffix(".metadata.json")
    if not path.exists() or not metadata.exists():
        raise SystemExit(f"Generate {symbol} demo history first with seed_synthetic_demo.py.")
    import json
    if json.loads(metadata.read_text(encoding="utf-8")).get("source") != SOURCE:
        raise SystemExit(f"Refusing to import non-synthetic data for {symbol}.")
    with path.open(newline="", encoding="utf-8") as source:
        return [
            (row["date"], row["open"], row["high"], row["low"], row["close"], row["adjusted_close"], int(row["volume"]))
            for row in csv.DictReader(source)
        ]


def main():
    connection, schema = connect()
    inserted_at = datetime.now(timezone.utc)
    try:
        with connection:
            with connection.cursor() as cursor:
                for symbol in SYMBOLS:
                    rows = read_rows(symbol)
                    cursor.execute(
                        sql.SQL("SELECT id FROM {}.stocks WHERE symbol = %s AND active = true").format(sql.Identifier(schema)),
                        (symbol,),
                    )
                    stock = cursor.fetchone()
                    if stock is None:
                        raise SystemExit(f"Stock {symbol} is missing from the backend stock table.")
                    stock_id = stock[0]
                    values = [(str(uuid.uuid4()), str(stock_id), day, open_price, high, low, close, adjusted, volume, SOURCE, inserted_at)
                              for day, open_price, high, low, close, adjusted, volume in rows]
                    row_template = sql.SQL("({})").format(sql.SQL(", ").join(sql.Placeholder() for _ in range(11)))
                    values_sql = sql.SQL(", ").join(row_template for _ in values)
                    statement = sql.SQL("""
                        INSERT INTO {}.historical_prices
                            (id, stock_id, trading_date, open, high, low, close, adjusted_close, volume, source, fetched_at)
                        VALUES {}
                        ON CONFLICT (stock_id, trading_date) DO UPDATE SET
                            open = EXCLUDED.open, high = EXCLUDED.high, low = EXCLUDED.low,
                            close = EXCLUDED.close, adjusted_close = EXCLUDED.adjusted_close,
                            volume = EXCLUDED.volume, source = EXCLUDED.source, fetched_at = EXCLUDED.fetched_at
                    """).format(sql.Identifier(schema), values_sql)
                    cursor.execute(statement, [field for row in values for field in row])
                    print(f"{symbol}: imported {len(values)} synthetic cache rows", flush=True)
    finally:
        connection.close()


if __name__ == "__main__":
    main()
