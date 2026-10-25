import asyncio
import json
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, WebSocket, WebSocketDisconnect, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.diagnostic import TelemetryHistoryResponse, TelemetryMessage
from app.services.diagnostic_service import (
    get_session_by_id,
    get_telemetry_history,
    save_telemetry_batch,
)
from app.services.simulator_service import simulator_manager
from app.simulator.truck_simulator import EngineState

router = APIRouter()


@router.get("/latest", response_model=TelemetryMessage)
async def get_latest_telemetry(
    session_id: Optional[str] = Query(None, description="Optional diagnostic session ID"),
    db: AsyncSession = Depends(get_db),
):
    if not session_id:
        # Fallback to any active simulator
        if simulator_manager.simulators:
            session_id = next(iter(simulator_manager.simulators.keys()))
        else:
            # Temporary virtual demo snapshot
            demo_sim = simulator_manager.get_or_create("demo-session", "scania-r500-2021")
            return demo_sim.get_telemetry_snapshot()

    sim = simulator_manager.get(session_id)
    if not sim:
        # Check if session exists in DB
        s = await get_session_by_id(db, session_id)
        if not s:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Diagnostic session '{session_id}' not found",
            )
        sim = simulator_manager.get_or_create(session_id=session_id, truck_id=s.truck_id)

    snapshot = sim.get_telemetry_snapshot()
    # Asynchronously save to DB
    try:
        await save_telemetry_batch(
            db=db,
            session_id=session_id,
            timestamp=snapshot.timestamp,
            parameters=snapshot.parameters,
        )
    except Exception:
        pass

    return snapshot


@router.get("/history", response_model=TelemetryHistoryResponse)
async def get_telemetry_hist(
    session_id: str = Query(..., description="Target session ID"),
    limit: int = Query(100, ge=1, le=500),
    db: AsyncSession = Depends(get_db),
):
    s = await get_session_by_id(db, session_id)
    if not s:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Diagnostic session '{session_id}' not found",
        )
    records = await get_telemetry_history(db, session_id, limit)
    formatted = [
        {
            "id": r.id,
            "timestamp": r.timestamp,
            "parameter_name": r.parameter_name,
            "value": r.value,
            "unit": r.unit,
            "quality": r.quality,
            "category": r.category,
            "simulated": r.simulated,
        }
        for r in records
    ]
    return TelemetryHistoryResponse(
        session_id=session_id,
        count=len(formatted),
        records=formatted,
    )


@router.websocket("/ws/{session_id}")
async def websocket_telemetry_stream(websocket: WebSocket, session_id: str):
    await websocket.accept()

    sim = simulator_manager.get_or_create(session_id=session_id, truck_id="scania-r500-2021")
    if not sim.is_running:
        await sim.start()

    queue: asyncio.Queue = asyncio.Queue()

    def on_telemetry(msg: TelemetryMessage):
        try:
            queue.put_nowait(msg)
        except Exception:
            pass

    sim.register_listener(on_telemetry)

    async def sender():
        while True:
            msg = await queue.get()
            try:
                await websocket.send_text(msg.model_dump_json())
            except Exception:
                break

    sender_task = asyncio.create_task(sender())

    try:
        while True:
            data = await websocket.receive_text()
            try:
                cmd = json.loads(data)
                action = cmd.get("action")
                if action == "set_state":
                    new_state = cmd.get("state")
                    if new_state in EngineState.__members__:
                        sim.set_engine_state(EngineState[new_state])
                elif action == "inject_fault":
                    f_key = cmd.get("fault_key")
                    if f_key:
                        sim.inject_fault(f_key)
                elif action == "clear_fault":
                    f_key = cmd.get("fault_key")
                    if f_key:
                        sim.clear_fault(f_key)
            except Exception:
                pass
    except (WebSocketDisconnect, asyncio.CancelledError):
        pass
    finally:
        sim.unregister_listener(on_telemetry)
        sender_task.cancel()
