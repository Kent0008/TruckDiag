from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.api.v1.router import api_router
from app.core.config import settings
from app.core.database import AsyncSessionLocal, init_db
from app.services.diagnostic_service import seed_trucks
from app.services.simulator_service import simulator_manager


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: initialize DB schema and seed initial trucks
    await init_db()
    async with AsyncSessionLocal() as session:
        await seed_trucks(session)

    yield

    # Shutdown: stop all active simulators
    for session_id in list(simulator_manager.simulators.keys()):
        await simulator_manager.remove(session_id)


app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description=(
        "TruckDiag Diagnostic API — SAE J1939 and Scania Heavy Vehicle Telemetry, "
        "DTC fault scanning, CAN frame parsing and diagnostic reporting."
    ),
    openapi_url="/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS Middleware (allows Android Emulator and local LAN devices)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS if isinstance(settings.CORS_ORIGINS, list) else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router, prefix=settings.API_V1_STR)


@app.get("/", tags=["Root"])
async def root():
    return JSONResponse(
        {
            "name": settings.PROJECT_NAME,
            "version": settings.VERSION,
            "docs_url": "/docs",
            "api_v1": settings.API_V1_STR,
            "description": "TruckDiag Heavy Duty Diagnostic Platform API",
            "status": "online",
        }
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
