from pathlib import Path
import os

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = Path(os.getenv("MARKET_DATA_DIR", BASE_DIR / "data"))
STOCKS_FILE = DATA_DIR / "stocks.csv"
HISTORY_DIR = DATA_DIR / "historical_prices"
DEFAULT_PERIOD = "1y"
DEFAULT_INTERVAL = "1d"
MAX_HISTORY_RECORDS = 1500
REQUEST_DELAY_SECONDS = float(os.getenv("MARKET_REQUEST_DELAY_SECONDS", "1.0"))
