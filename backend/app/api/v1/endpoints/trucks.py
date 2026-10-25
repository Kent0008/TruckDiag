from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.diagnostic import TruckResponse
from app.services.diagnostic_service import get_truck_by_id, list_trucks

router = APIRouter()


@router.get("", response_model=List[TruckResponse])
async def get_trucks(db: AsyncSession = Depends(get_db)):
    trucks = await list_trucks(db)
    return [
        TruckResponse(
            id=t.id,
            brand=t.brand,
            model=t.model,
            year=t.year,
            vin=t.vin,
            engine_code=t.engine_code,
            transmission_type=t.transmission_type,
            ecu_list=[e.strip() for e in t.ecu_list.split(",")],
            simulated=t.simulated,
        )
        for t in trucks
    ]


@router.get("/{truck_id}", response_model=TruckResponse)
async def get_truck(truck_id: str, db: AsyncSession = Depends(get_db)):
    t = await get_truck_by_id(db, truck_id)
    if not t:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Truck with ID '{truck_id}' not found",
        )
    return TruckResponse(
        id=t.id,
        brand=t.brand,
        model=t.model,
        year=t.year,
        vin=t.vin,
        engine_code=t.engine_code,
        transmission_type=t.transmission_type,
        ecu_list=[e.strip() for e in t.ecu_list.split(",")],
        simulated=t.simulated,
    )
