from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.protocols.can_frame import CanFrame
from app.protocols.j1939 import (
    PGN_REGISTRY,
    SPN_CATALOG,
    decode_j1939_frame,
)
from app.schemas.diagnostic import (
    CanDecodeRequest,
    CanDecodeResponse,
    CanFrameResponse,
    DecodedSpnInfo,
    J1939ParameterInfo,
    J1939PgnInfo,
)
from app.services.simulator_service import simulator_manager

router = APIRouter()


@router.get("/frames", response_model=List[CanFrameResponse])
async def get_can_frames(
    session_id: Optional[str] = Query(None),
    limit: int = Query(50, ge=1, le=100),
):
    sim = None
    if session_id:
        sim = simulator_manager.get(session_id)
    elif simulator_manager.simulators:
        sim = next(iter(simulator_manager.simulators.values()))

    if not sim:
        # Create or fetch demo frames
        sim = simulator_manager.get_or_create("demo-session", "scania-r500-2021")

    frames = sim.get_recent_can_frames(limit)
    return [
        CanFrameResponse(
            id=f.frame_id or f"frame-{i}",
            session_id=session_id,
            can_id=f.can_id,
            can_id_hex=f.can_id_hex,
            priority=f.priority,
            pgn=f.pgn,
            pgn_hex=f.pgn_hex,
            pgn_name=f.pgn_name,
            source_address=f.source_address,
            source_name=f.source_name,
            destination_address=f.destination_address,
            dlc=f.dlc,
            payload_hex=f.payload_hex,
            timestamp=f.timestamp,
            simulated=f.simulated,
        )
        for i, f in enumerate(frames)
    ]


@router.get("/frames/{frame_id}", response_model=CanFrameResponse)
async def get_can_frame_by_id(frame_id: str):
    # Find frame in active simulators
    for sim in simulator_manager.simulators.values():
        for i, f in enumerate(sim.can_frames_buffer):
            fid = f.frame_id or f"frame-{i}"
            if fid == frame_id:
                return CanFrameResponse(
                    id=fid,
                    session_id=sim.session_id,
                    can_id=f.can_id,
                    can_id_hex=f.can_id_hex,
                    priority=f.priority,
                    pgn=f.pgn,
                    pgn_hex=f.pgn_hex,
                    pgn_name=f.pgn_name,
                    source_address=f.source_address,
                    source_name=f.source_name,
                    destination_address=f.destination_address,
                    dlc=f.dlc,
                    payload_hex=f.payload_hex,
                    timestamp=f.timestamp,
                    simulated=f.simulated,
                )
    raise HTTPException(
        status_code=status.HTTP_404_NOT_FOUND,
        detail=f"CAN frame '{frame_id}' not found in active frame buffer",
    )


@router.post("/decode", response_model=CanDecodeResponse)
async def post_decode_can_frame(req: CanDecodeRequest):
    # Parse CAN ID (int or hex string)
    can_id_val: int
    if isinstance(req.can_id, int):
        can_id_val = req.can_id
    elif isinstance(req.can_id, str):
        cleaned = req.can_id.strip()
        try:
            if cleaned.lower().startswith("0x"):
                can_id_val = int(cleaned, 16)
            else:
                can_id_val = int(cleaned)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid CAN ID format '{req.can_id}'. Must be decimal or 0x-hex string.",
            )
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="CAN ID must be integer or string.",
        )

    # Validate 29-bit CAN ID bounds (0 to 0x1FFFFFFF)
    if not (0 <= can_id_val <= 0x1FFFFFFF):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"CAN ID 0x{can_id_val:X} out of 29-bit range (0x0 to 0x1FFFFFFF).",
        )

    # Parse and validate Payload hex
    cleaned_hex = req.payload.strip().replace(" ", "").replace(":", "")
    if len(cleaned_hex) % 2 != 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Payload hex length must be even (got {len(cleaned_hex)} hex characters).",
        )

    try:
        payload_bytes = bytes.fromhex(cleaned_hex)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Payload contains invalid hexadecimal characters.",
        )

    if len(payload_bytes) == 0 or len(payload_bytes) > 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"J1939 standard single frame payload DLC must be between 1 and 8 bytes (got {len(payload_bytes)} bytes).",
        )

    try:
        decoded = decode_j1939_frame(can_id=can_id_val, payload=payload_bytes)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Failed to decode J1939 frame: {str(e)}",
        )

    return CanDecodeResponse(
        can_id=decoded.can_id,
        can_id_hex=decoded.can_id_hex,
        priority=decoded.priority,
        pgn=decoded.pgn,
        pgn_hex=decoded.pgn_hex,
        pgn_name=decoded.pgn_name,
        source_address=decoded.source_address,
        source_name=decoded.source_name,
        destination_address=decoded.destination_address,
        dlc=decoded.dlc,
        payload_hex=decoded.payload_hex,
        fidelity=decoded.fidelity,
        decoded_spns=[
            DecodedSpnInfo(
                spn=s.spn,
                name=s.name,
                raw_value=s.raw_value,
                scaled_value=s.scaled_value,
                unit=s.unit,
                description=s.description,
                fidelity=s.fidelity,
                is_valid=s.is_valid,
            )
            for s in decoded.spns
        ],
        simulated=True,
    )


@router.get("/parameters", response_model=List[J1939ParameterInfo])
async def get_j1939_parameters():
    return [
        J1939ParameterInfo(
            spn=spn,
            name=data["name"],
            pgn=data["pgn"],
            resolution=data["resolution"],
            offset=data["offset"],
            unit=data["unit"],
            range=data["range"],
            fidelity=data["fidelity"],
            description=data["description"],
        )
        for spn, data in SPN_CATALOG.items()
    ]


@router.get("/pgns", response_model=List[J1939PgnInfo])
async def get_j1939_pgns():
    return [
        J1939PgnInfo(
            pgn=pgn,
            acronym=data["acronym"],
            name=data["name"],
            rate_ms=data["rate_ms"],
            priority=data["priority"],
            source=data["source"],
            fidelity=data["fidelity"],
            description=data["description"],
        )
        for pgn, data in PGN_REGISTRY.items()
    ]
