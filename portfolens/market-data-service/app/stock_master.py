import csv
from dataclasses import dataclass
from .config import STOCKS_FILE


@dataclass(frozen=True)
class Stock:
    symbol: str
    company_name: str
    bse_scrip_code: str
    yahoo_ticker: str
    exchange: str
    sector: str
    industry: str
    active: bool


def load_stocks() -> dict[str, Stock]:
    if not STOCKS_FILE.exists():
        return {}
    with STOCKS_FILE.open(newline="", encoding="utf-8") as source:
        rows = csv.DictReader(source)
        stocks = {}
        for row in rows:
            stock = Stock(**{**row, "active": row["active"].strip().lower() in {"true", "1", "yes"}})
            stocks[stock.symbol.upper()] = stock
        return stocks


def resolve_stock(symbol: str) -> Stock | None:
    return load_stocks().get(symbol.strip().upper())
