FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends build-essential libgomp1 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 appuser \
    && useradd --system --uid 10001 --gid 10001 --create-home appuser
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY --chown=10001:10001 app ./app
COPY --chown=10001:10001 data/stocks.csv ./data/stocks.csv
ENV PORTFOLENS_STOCKS_FILE=/app/data/stocks.csv
RUN mkdir -p /data/historical_prices /data/market_series && chown -R 10001:10001 /data
ENV MARKET_DATA_DIR=/data
USER 10001:10001
EXPOSE 8001
STOPSIGNAL SIGTERM
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001", "--proxy-headers"]
