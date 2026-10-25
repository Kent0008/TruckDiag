from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.models.diagnostic import FaultModel
from app.schemas.diagnostic import FaultResponse, FaultScanResponse
from app.services.diagnostic_service import (
    get_session_by_id,
    scan_faults_for_session,
)
from app.services.simulator_service import simulator_manager

router = APIRouter()


class FaultInjectRequest(BaseModel):
    fault_key: str  # "COOLANT_OVERHEAT", "LOW_OIL_PRESSURE", "LOW_AIR_PRESSURE", "SCR_NOX_EXCEEDANCE"


@router.get("/faults", response_model=List[FaultResponse])
async def get_all_faults(
    session_id: Optional[str] = Query(None),
    limit: int = 50,
    db: AsyncSession = Depends(get_db),
):
    query = select(FaultModel).limit(limit)
    if session_id:
        query = query.where(FaultModel.session_id == session_id)
    res = await db.execute(query)
    faults = list(res.scalars().all())
    return [
        FaultResponse(
            id=f.id,
            session_id=f.session_id,
            code=f.code,
            source_ecu=f.source_ecu,
            spn=f.spn,
            fmi=f.fmi,
            oc=f.oc,
            severity=f.severity,
            description=f.description,
            possible_causes=f.possible_causes or "",
            status=f.status,
            timestamp=f.timestamp,
            simulated=f.simulated,
        )
        for f in faults
    ]


@router.get("/faults/{fault_id}", response_model=FaultResponse)
async def get_fault_by_id(fault_id: str, db: AsyncSession = Depends(get_db)):
    res = await db.execute(select(FaultModel).where(FaultModel.id == fault_id))
    f = res.scalars().first()
    if not f:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Fault record '{fault_id}' not found",
        )
    return FaultResponse(
        id=f.id,
        session_id=f.session_id,
        code=f.code,
        source_ecu=f.source_ecu,
        spn=f.spn,
        fmi=f.fmi,
        oc=f.oc,
        severity=f.severity,
        description=f.description,
        possible_causes=f.possible_causes or "",
        status=f.status,
        timestamp=f.timestamp,
        simulated=f.simulated,
    )


@router.get("/sessions/{session_id}/faults", response_model=List[FaultResponse])
async def get_session_faults(session_id: str, db: AsyncSession = Depends(get_db)):
    s = await get_session_by_id(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Session '{session_id}' not found",
        )
    res = await db.execute(select(FaultModel).where(FaultModel.session_id == session_id))
    faults = list(res.scalars().all())
    return [
        FaultResponse(
            id=f.id,
            session_id=f.session_id,
            code=f.code,
            source_ecu=f.source_ecu,
            spn=f.spn,
            fmi=f.fmi,
            oc=f.oc,
            severity=f.severity,
            description=f.description,
            possible_causes=f.possible_causes or "",
            status=f.status,
            timestamp=f.timestamp,
            simulated=f.simulated,
        )
        for f in faults
    ]


@router.post("/sessions/{session_id}/faults/scan", response_model=FaultScanResponse)
async def post_scan_session_faults(session_id: str, db: AsyncSession = Depends(get_db)):
    s = await get_session_by_id(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Session '{session_id}' not found",
        )

    faults = await scan_faults_for_session(db, session_id)
    fault_responses = [
        FaultResponse(
            id=f.id,
            session_id=f.session_id,
            code=f.code,
            source_ecu=f.source_ecu,
            spn=f.spn,
            fmi=f.fmi,
            oc=f.oc,
            severity=f.severity,
            description=f.description,
            possible_causes=f.possible_causes or "",
            status=f.status,
            timestamp=f.timestamp,
            simulated=f.simulated,
        )
        for f in faults
    ]

    return FaultScanResponse(
        session_id=session_id,
        scanned_ecus=[
            "EMS S8 (управление двигателем)",
            "COO8 (координатор бортовых систем)",
            "GMS (коробка передач Opticruise)",
            "EBS7 (тормозная система)",
            "APS (подготовка воздуха)",
            "EEC (система нейтрализации выхлопа SCR)",
        ],
        total_found=len(fault_responses),
        faults=fault_responses,
        simulated=True,
    )


@router.post("/sessions/{session_id}/faults/inject")
async def post_inject_fault(
    session_id: str, req: FaultInjectRequest, db: AsyncSession = Depends(get_db)
):
    s = await get_session_by_id(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Session '{session_id}' not found",
        )
    sim = simulator_manager.get_or_create(session_id, s.truck_id)
    ok = sim.inject_fault(req.fault_key)
    if not ok:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unknown fault key '{req.fault_key}'. Valid: COOLANT_OVERHEAT, LOW_OIL_PRESSURE, LOW_AIR_PRESSURE, SCR_NOX_EXCEEDANCE",
        )
    return {"status": "injected", "fault_key": req.fault_key, "simulated": True}
