from pathlib import Path
import os

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = Path(os.getenv("MARKET_DATA_DIR", BASE_DIR / "data"))
STOCKS_FILE = DATA_DIR / "stocks.csv"
HISTORY_DIR = DATA_DIR / "historical_prices"
SERIES_DIR = DATA_DIR / "market_series"
DEFAULT_PERIOD = "1y"
DEFAULT_INTERVAL = "1d"
MAX_HISTORY_RECORDS = 1500
REQUEST_DELAY_SECONDS = float(os.getenv("MARKET_REQUEST_DELAY_SECONDS", "1.0"))
SENSEX_YAHOO_TICKER = os.getenv("SENSEX_YAHOO_TICKER", "^BSESN")
RISK_FREE_ANNUAL_RATE = os.getenv("MARKET_RISK_FREE_ANNUAL_RATE", "")
RBI_WSS_CACHE_HOURS = int(os.getenv("RBI_WSS_CACHE_HOURS", "168"))
MARKET_DATA_SERVICE_KEY = os.getenv("MARKET_DATA_SERVICE_KEY", "")
