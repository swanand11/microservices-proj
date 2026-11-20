from datetime import date, datetime
from pydantic import BaseModel, Field


class StockOut(BaseModel):
    symbol: str
    company_name: str
    yahoo_ticker: str
    exchange: str
    sector: str
    industry: str
    active: bool


class PriceRecord(BaseModel):
    date: date
    open: float
    high: float
    low: float
    close: float
    adjusted_close: float
    volume: int


class HistoryOut(BaseModel):
    symbol: str
    company_name: str
    yahoo_ticker: str
    exchange: str
    data_start_date: date | None
    data_end_date: date | None
    last_updated: datetime | None
    source: str | None
    price_policy: str = "Raw OHLC/Close and Yahoo adjusted close are returned separately."
    total_records: int
    offset: int = 0
    limit: int
    records: list[PriceRecord] = Field(default_factory=list)


class DownloadRequest(BaseModel):
    period: str = "1y"
    refresh: bool = False


class BulkDownloadRequest(DownloadRequest):
    symbols: list[str] = Field(default_factory=list, max_length=50)
