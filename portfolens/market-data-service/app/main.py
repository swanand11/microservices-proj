from pathlib import Path
from dotenv import load_dotenv

# Match the Spring services' local .env behavior for direct development runs.
load_dotenv(Path(__file__).resolve().parents[1] / ".env", override=False)

from fastapi import FastAPI

if __package__:
    from .routes import protected_router, router
else:
    import sys
    from pathlib import Path

    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    from app.routes import protected_router, router

app = FastAPI(title="PortfoLens Market Data", version="1.0.0", description="Historical BSE market data sourced from Yahoo Finance. Not a real-time feed.")
app.include_router(router)
app.include_router(protected_router)
