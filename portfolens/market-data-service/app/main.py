from fastapi import FastAPI
from app.routes import router

app = FastAPI(title="PortfoLens Market Data", version="1.0.0", description="Historical BSE market data sourced from Yahoo Finance. Not a real-time feed.")
app.include_router(router)
