"""Fetch RBI's published weekly FBIL 10-year G-Sec par-yield observations."""
from calendar import month_abbr
from datetime import date, datetime, timezone
from html.parser import HTMLParser
import logging
import re

import requests

from .config import RBI_WSS_CACHE_HOURS
from .series_store import load_series, save_series

logger = logging.getLogger(__name__)
ARCHIVE_URL = "https://www.rbi.org.in/Scripts/WSSViewDetail.aspx?PARAM1=4&TYPE=Section"
WSS_URL = "https://www.rbi.org.in/Scripts/WSSView.aspx?Id={}"
SOURCE = "RBI Weekly Statistical Supplement (FBIL 10-Year G-Sec Par Yield)"
USER_AGENT = "PortfoLens academic market-data client/1.0"


class _Cell:
    def __init__(self, text: str, colspan: int):
        self.text = " ".join(text.split())
        self.colspan = colspan


class _TableParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.rows = []
        self.text = []
        self._in_row = False
        self._in_cell = False
        self._row = []
        self._cell_text = []
        self._colspan = 1

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "tr":
            self._in_row = True
            self._row = []
        elif tag in ("td", "th"):
            self._in_cell = True
            self._cell_text = []
            try:
                self._colspan = max(1, int(attrs.get("colspan", "1")))
            except ValueError:
                self._colspan = 1

    def handle_endtag(self, tag):
        if tag in ("td", "th") and self._in_cell:
            self._row.append(_Cell(" ".join(self._cell_text), self._colspan))
            self._in_cell = False
        elif tag == "tr" and self._in_row:
            self.rows.append(self._row)
            self._in_row = False

    def handle_data(self, data):
        self.text.append(data)
        if self._in_cell:
            self._cell_text.append(data)


def _request(url: str) -> str:
    response = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=(5, 20))
    response.raise_for_status()
    return response.text


def _latest_wss_url() -> str:
    archive = _request(ARCHIVE_URL)
    match = re.search(r"WSSView\.aspx\?Id=(\d+)", archive, re.IGNORECASE)
    if match is None:
        raise ValueError("RBI WSS archive did not contain a latest issue link")
    return WSS_URL.format(match.group(1))


def _week_date(label: str, year: int) -> date:
    match = re.fullmatch(r"\s*([A-Za-z]+)\.?\s+(\d{1,2})\s*", label)
    if match is None:
        raise ValueError(f"Unrecognized RBI WSS observation date: {label}")
    month_text, day_text = match.groups()
    month = next((index for index, name in enumerate(month_abbr) if name and name.lower() == month_text[:3].lower()), None)
    if month is None:
        raise ValueError(f"Unrecognized RBI WSS month: {month_text}")
    return date(year, month, int(day_text))


def _extract_wss(html: str) -> tuple[list[tuple[date, float]], date]:
    parser = _TableParser()
    parser.feed(html)
    visible_text = " ".join(" ".join(parser.text).split())
    published_match = re.search(r"Date\s*:\s*([A-Za-z]+\s+\d{1,2},?\s+\d{4})", visible_text, re.IGNORECASE)
    if published_match is None:
        raise ValueError("RBI WSS issue date was not present")
    published_text = published_match.group(1).replace(",", "")
    try:
        published_at = datetime.strptime(published_text, "%B %d %Y").date()
    except ValueError:
        published_at = datetime.strptime(published_text, "%b %d %Y").date()

    header_index = next((index for index, row in enumerate(parser.rows)
                         if row and row[0].text.casefold() == "item/week ended"), None)
    if header_index is None or header_index + 2 >= len(parser.rows):
        raise ValueError("RBI WSS weekly rate headers were not present")
    year_cells = parser.rows[header_index][1:]
    years = [int(cell.text) for cell in year_cells for _ in range(cell.colspan)]
    date_cells = parser.rows[header_index + 1]
    labels = [cell.text for cell in date_cells]
    if len(years) != len(labels):
        raise ValueError("RBI WSS week dates did not align with their years")

    rate_row = next((row for row in parser.rows[header_index + 3:]
                     if row and row[0].text.casefold() == "10-year g-sec par yield (fbil)"), None)
    if rate_row is None:
        raise ValueError("RBI WSS did not contain the 10-year G-Sec par-yield row")
    values = [cell.text for cell in rate_row[1:]]
    if len(values) != len(labels):
        raise ValueError("RBI WSS yield values did not align with their dates")
    records = []
    for label, year, raw_value in zip(labels, years, values):
        try:
            value = float(raw_value.replace(",", ""))
        except ValueError:
            continue
        observation_date = _week_date(label, year)
        if not 0 < value < 100:
            raise ValueError("RBI WSS contained an implausible 10-year yield")
        records.append((observation_date, value))
    if not records:
        raise ValueError("RBI WSS contained no numeric 10-year yield observations")
    return sorted(set(records)), published_at


def get_gsec10y_history(force_refresh: bool = False):
    records, metadata = load_series("gsec10y")
    fetched_at_raw = metadata.get("fetched_at")
    if records and not force_refresh and fetched_at_raw:
        try:
            fetched_at = datetime.fromisoformat(fetched_at_raw)
            if fetched_at.tzinfo is None:
                fetched_at = fetched_at.replace(tzinfo=timezone.utc)
            if (datetime.now(timezone.utc) - fetched_at).total_seconds() < RBI_WSS_CACHE_HOURS * 3600:
                return records, metadata
        except (TypeError, ValueError):
            pass
    try:
        records, published_at = _extract_wss(_request(_latest_wss_url()))
        metadata = save_series("gsec10y", records, SOURCE, published_at)
        return records, metadata
    except Exception:
        if records:
            logger.warning("RBI WSS refresh failed; serving the last stored G-Sec series", exc_info=True)
            return records, metadata
        raise
