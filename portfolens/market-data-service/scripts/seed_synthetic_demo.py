"""Create deterministic, clearly labelled market history for a local demo."""

import csv
from datetime import date, datetime, timezone
import json
import math
from pathlib import Path
import random


BASE_DIR = Path(__file__).resolve().parents[1]
HISTORY_DIR = BASE_DIR / "data" / "historical_prices"
SOURCE = "Synthetic demo data"
STOCKS = {
    "INFY": (1_520.0, 0.92, 0.010, 20261122),
    "TCS": (3_680.0, 0.78, 0.008, 20261123),
    "HDFCBANK": (1_710.0, 1.08, 0.009, 20261124),
}


def read_sensex():
    path = HISTORY_DIR / "SENSEX.csv"
    if not path.exists():
        raise SystemExit("SENSEX.csv is required to generate aligned demo prices.")
    with path.open(newline="", encoding="utf-8") as source:
        rows = [
            (date.fromisoformat(row["date"]), float(row["close"]))
            for row in csv.DictReader(source)
            if row.get("date") and row.get("close")
        ]
    if len(rows) < 35:
        raise SystemExit("At least 35 Sensex observations are required for a useful demo.")
    return sorted(rows)


def write_stock(symbol, base_price, beta, daily_volatility, seed, benchmark):
    path = HISTORY_DIR / f"{symbol}.csv"
    metadata_path = HISTORY_DIR / f"{symbol}.metadata.json"
    if path.exists():
        existing = json.loads(metadata_path.read_text(encoding="utf-8")) if metadata_path.exists() else {}
        if existing.get("source") != SOURCE:
            raise SystemExit(f"Refusing to replace non-demo market history: {path}")

    rng = random.Random(seed)
    previous_benchmark = benchmark[0][1]
    close = base_price
    rows = []
    for index, (trading_date, sensex_close) in enumerate(benchmark):
        benchmark_return = sensex_close / previous_benchmark - 1.0 if index else 0.0
        cyclical = 0.00012 * math.sin(index / 17.0) + 0.00006 * math.cos(index / 41.0)
        daily_return = max(-0.045, min(0.045, cyclical + beta * benchmark_return + rng.gauss(0.0, daily_volatility)))
        open_price = close * (1.0 + rng.gauss(0.0, 0.0022))
        close *= 1.0 + daily_return
        high = max(open_price, close) * (1.0 + abs(rng.gauss(0.0025, 0.002)))
        low = min(open_price, close) * (1.0 - abs(rng.gauss(0.0025, 0.002)))
        volume = rng.randint(250_000, 8_500_000)
        rows.append((trading_date.isoformat(), open_price, high, low, close, close, volume))
        previous_benchmark = sensex_close

    HISTORY_DIR.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as output:
        writer = csv.writer(output)
        writer.writerow(["date", "open", "high", "low", "close", "adjusted_close", "volume"])
        writer.writerows((day, *(f"{value:.8f}" for value in values[:5]), values[5]) for day, *values in rows)
    metadata_path.write_text(json.dumps({
        "source": SOURCE,
        "downloaded_at": datetime.now(timezone.utc).isoformat(),
        "price_policy": "Generated deterministic demo series; not actual market observations. Returns loosely follow historical Sensex changes with seeded daily variation.",
        "synthetic": True,
    }, indent=2) + "\n", encoding="utf-8")
    print(f"{symbol}: wrote {len(rows)} synthetic observations ({rows[0][0]} to {rows[-1][0]})")


def main():
    benchmark = read_sensex()
    for symbol, (base_price, beta, volatility, seed) in STOCKS.items():
        write_stock(symbol, base_price, beta, volatility, seed, benchmark)


if __name__ == "__main__":
    main()
