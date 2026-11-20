import argparse
from app.collector import download_bulk, download_stock, validate_ticker
from app.stock_master import load_stocks


def main():
    parser = argparse.ArgumentParser(description="Download historical BSE prices into local CSV files")
    parser.add_argument("symbols", nargs="*", help="Symbols to download; defaults to active stock master")
    parser.add_argument("--period", default="1y")
    parser.add_argument("--refresh", action="store_true")
    parser.add_argument("--validate", action="store_true", help="Check Yahoo ticker availability before downloading")
    args = parser.parse_args()
    stocks = load_stocks()
    selected = [stocks[s.upper()] for s in args.symbols] if args.symbols else [s for s in stocks.values() if s.active]
    if args.validate:
        selected = [stock for stock in selected if validate_ticker(stock)]
    results = download_bulk(selected, period=args.period, refresh=args.refresh) if len(selected) != 1 else [download_stock(selected[0], period=args.period, refresh=args.refresh)]
    for result in results:
        print(result)


if __name__ == "__main__":
    main()
