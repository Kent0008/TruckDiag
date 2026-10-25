import time
from fastapi import APIRouter, Depends
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.config import settings
from app.core.database import get_db
from app.schemas.diagnostic import HealthResponse, SystemInfoResponse

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
async def get_health(db: AsyncSession = Depends(get_db)):
    db_status = "connected"
    try:
        await db.execute(text("SELECT 1"))
    except Exception:
        db_status = "error"

    return HealthResponse(
        status="ok" if db_status == "connected" else "degraded",
        timestamp=time.time(),
        database=db_status,
        simulator="ready",
        version=settings.VERSION,
    )


@router.get("/system/info", response_model=SystemInfoResponse)
async def get_system_info():
    return SystemInfoResponse(
        project_name=settings.PROJECT_NAME,
        api_version=settings.VERSION,
        protocol_version="SAE J1939-2023",
        supported_protocols=[
            "SAE J1939",
            "CAN 2.0B (29-bit)",
            "Scania Diagnostics (Read-Only Simulation)",
        ],
        available_adapters=[
            "SIMULATOR_VIRTUAL_BUS (Active)",
            "SOCKETCAN_INTERFACE (Prepared)",
            "RP1210_PASSTHRU (Prepared)",
        ],
        supported_truck_platforms=[
            "Scania R/S-Series (2019-2023 Euro 6)",
            "Volvo FH Series (2014+ J1939 Architecture)",
        ],
        environment="local_development",
    )
