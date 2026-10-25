import time
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.diagnostic import (
    SessionCreateRequest,
    SessionResponse,
    SessionStatusResponse,
    TruckResponse,
)
from app.services.diagnostic_service import (
    create_session,
    get_session_by_id,
    get_truck_by_id,
    list_sessions,
    start_session,
    stop_session,
)
from app.services.simulator_service import simulator_manager

router = APIRouter()


@router.post("", response_model=SessionResponse, status_code=status.HTTP_201_CREATED)
async def post_session(req: SessionCreateRequest, db: AsyncSession = Depends(get_db)):
    truck = await get_truck_by_id(db, req.truck_id)
    if not truck:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Truck '{req.truck_id}' not found",
        )
    session = await create_session(db, req)
    return SessionResponse(
        id=session.id,
        truck_id=session.truck_id,
        operator_name=session.operator_name,
        notes=session.notes,
        connection_mode=session.connection_mode,
        status=session.status,
        start_time=session.start_time,
        stop_time=session.stop_time,
        created_at=session.created_at,
        truck=TruckResponse(
            id=truck.id,
            brand=truck.brand,
            model=truck.model,
            year=truck.year,
            vin=truck.vin,
            engine_code=truck.engine_code,
            transmission_type=truck.transmission_type,
            ecu_list=[e.strip() for e in truck.ecu_list.split(",")],
            simulated=truck.simulated,
        ),
    )


@router.get("", response_model=List[SessionResponse])
async def get_sessions(limit: int = 50, db: AsyncSession = Depends(get_db)):
    sessions = await list_sessions(db, limit)
    res = []
    for s in sessions:
        truck = await get_truck_by_id(db, s.truck_id)
        truck_resp = (
            TruckResponse(
                id=truck.id,
                brand=truck.brand,
                model=truck.model,
                year=truck.year,
                vin=truck.vin,
                engine_code=truck.engine_code,
                transmission_type=truck.transmission_type,
                ecu_list=[e.strip() for e in truck.ecu_list.split(",")],
                simulated=truck.simulated,
            )
            if truck
            else None
        )
        res.append(
            SessionResponse(
                id=s.id,
                truck_id=s.truck_id,
                operator_name=s.operator_name,
                notes=s.notes,
                connection_mode=s.connection_mode,
                status=s.status,
                start_time=s.start_time,
                stop_time=s.stop_time,
                created_at=s.created_at,
                truck=truck_resp,
            )
        )
    return res


@router.get("/{session_id}", response_model=SessionResponse)
async def get_session(session_id: str, db: AsyncSession = Depends(get_db)):
    s = await get_session_by_id(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Diagnostic session '{session_id}' not found",
        )
    truck = await get_truck_by_id(db, s.truck_id)
    truck_resp = (
        TruckResponse(
            id=truck.id,
            brand=truck.brand,
            model=truck.model,
            year=truck.year,
            vin=truck.vin,
            engine_code=truck.engine_code,
            transmission_type=truck.transmission_type,
            ecu_list=[e.strip() for e in truck.ecu_list.split(",")],
            simulated=truck.simulated,
        )
        if truck
        else None
    )
    return SessionResponse(
        id=s.id,
        truck_id=s.truck_id,
        operator_name=s.operator_name,
        notes=s.notes,
        connection_mode=s.connection_mode,
        status=s.status,
        start_time=s.start_time,
        stop_time=s.stop_time,
        created_at=s.created_at,
        truck=truck_resp,
    )


@router.post("/{session_id}/start", response_model=SessionResponse)
async def post_start_session(session_id: str, db: AsyncSession = Depends(get_db)):
    s = await start_session(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Diagnostic session '{session_id}' not found",
        )
    return await get_session(session_id, db)


@router.post("/{session_id}/stop", response_model=SessionResponse)
async def post_stop_session(session_id: str, db: AsyncSession = Depends(get_db)):
    s = await stop_session(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Diagnostic session '{session_id}' not found",
        )
    return await get_session(session_id, db)


@router.get("/{session_id}/status", response_model=SessionStatusResponse)
async def get_session_status(session_id: str, db: AsyncSession = Depends(get_db)):
    s = await get_session_by_id(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Diagnostic session '{session_id}' not found",
        )

    sim = simulator_manager.get(session_id)
    is_running = sim.is_running if sim else False
    uptime = 0.0
    if s.start_time:
        end = s.stop_time or time.time()
        start_ts = s.start_time.timestamp()
        if isinstance(end, float):
            uptime = max(0.0, end - start_ts)
        else:
            uptime = max(0.0, end.timestamp() - start_ts)

    connection_state = "CONNECTED" if is_running else ("CONNECTING" if s.status == "CREATED" else "DISCONNECTED")

    return SessionStatusResponse(
        session_id=session_id,
        status=s.status,
        connection_state=connection_state,
        data_source="SIMULATED_J1939",
        uptime_seconds=round(uptime, 1),
        active_ecus=6,
        frames_per_second=10.0 if is_running else 0.0,
        simulated=True,
    )
