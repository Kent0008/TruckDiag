import json
import time
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.diagnostic import (
    CanFrameModel,
    FaultModel,
    ReportModel,
    SessionModel,
    TelemetryModel,
    TruckModel,
    utc_now,
)
from app.schemas.diagnostic import (
    FaultResponse,
    ReportCreateRequest,
    SessionCreateRequest,
)
from app.services.simulator_service import simulator_manager


DEFAULT_TRUCKS = [
    {
        "id": "scania-r500-2021",
        "brand": "Scania",
        "model": "R500 V8 Super Highline",
        "year": 2021,
        "vin": "YS2R4X20002984120",
        "engine_code": "DC13 155 / Euro 6d",
        "transmission_type": "GRSO905R Opticruise 12+2",
        "ecu_list": "EMS S8 (двигатель), COO8 (координатор), GMS (коробка передач), EBS7 (тормозная система), APS (пневмосистема), BUP (резервный блок)",
        "simulated": True,
    },
    {
        "id": "scania-s730-2022",
        "brand": "Scania",
        "model": "S730 V8 King of the Road",
        "year": 2022,
        "vin": "YS2S4X20003019842",
        "engine_code": "DC16 108 / 730HP Euro 6d",
        "transmission_type": "GRSO925R Opticruise with Clutch-on-Demand",
        "ecu_list": "EMS S8, COO8, GMS, EBS7, RET (Retarder R4100), EEC (SCR DeNOx)",
        "simulated": True,
    },
    {
        "id": "volvo-fh540-2020",
        "brand": "Volvo",
        "model": "FH 540 Globetrotter XL",
        "year": 2020,
        "vin": "YV2RT40A9LB198234",
        "engine_code": "D13K 540HP Turbo-TC / Euro 6",
        "transmission_type": "AT2612F I-Shift Dual Clutch",
        "ecu_list": "EMS (Engine), VMCU (Vehicle Master ECU), TECU (Transmission), EBS, BBM (Body Builder)",
        "simulated": True,
    },
]


async def seed_trucks(db: AsyncSession) -> None:
    """Pre-populates database with standard truck profiles if empty."""
    result = await db.execute(select(TruckModel))
    trucks = result.scalars().all()
    if not trucks:
        for t_data in DEFAULT_TRUCKS:
            truck = TruckModel(**t_data)
            db.add(truck)
        await db.commit()


async def list_trucks(db: AsyncSession) -> List[TruckModel]:
    result = await db.execute(select(TruckModel))
    return list(result.scalars().all())


async def get_truck_by_id(db: AsyncSession, truck_id: str) -> Optional[TruckModel]:
    result = await db.execute(select(TruckModel).where(TruckModel.id == truck_id))
    return result.scalars().first()


async def create_session(db: AsyncSession, req: SessionCreateRequest) -> SessionModel:
    session = SessionModel(
        truck_id=req.truck_id,
        operator_name=req.operator_name,
        notes=req.notes or "",
        connection_mode=req.connection_mode,
        status="CREATED",
        created_at=utc_now(),
    )
    db.add(session)
    await db.commit()
    await db.refresh(session)
    # Instantiate simulator
    simulator_manager.get_or_create(session_id=session.id, truck_id=session.truck_id)
    return session


async def get_session_by_id(db: AsyncSession, session_id: str) -> Optional[SessionModel]:
    result = await db.execute(select(SessionModel).where(SessionModel.id == session_id))
    return result.scalars().first()


async def list_sessions(db: AsyncSession, limit: int = 50) -> List[SessionModel]:
    result = await db.execute(
        select(SessionModel).order_by(desc(SessionModel.created_at)).limit(limit)
    )
    return list(result.scalars().all())


async def start_session(db: AsyncSession, session_id: str) -> Optional[SessionModel]:
    session = await get_session_by_id(db, session_id)
    if not session:
        return None
    session.status = "RUNNING"
    session.start_time = utc_now()
    await db.commit()
    await db.refresh(session)

    # Start truck simulator
    sim = simulator_manager.get_or_create(session_id=session.id, truck_id=session.truck_id)
    await sim.start()
    return session


async def stop_session(db: AsyncSession, session_id: str) -> Optional[SessionModel]:
    session = await get_session_by_id(db, session_id)
    if not session:
        return None
    session.status = "STOPPED"
    session.stop_time = utc_now()
    await db.commit()
    await db.refresh(session)

    # Stop simulator
    sim = simulator_manager.get(session_id)
    if sim:
        await sim.stop()
    return session


async def save_telemetry_batch(
    db: AsyncSession, session_id: str, timestamp: float, parameters: List[Any]
) -> None:
    for p in parameters:
        rec = TelemetryModel(
            session_id=session_id,
            timestamp=timestamp,
            parameter_name=p.name,
            value=p.value,
            unit=p.unit,
            quality=p.quality,
            category=p.category,
            simulated=True,
        )
        db.add(rec)
    await db.commit()


async def get_telemetry_history(
    db: AsyncSession, session_id: str, limit: int = 100
) -> List[TelemetryModel]:
    result = await db.execute(
        select(TelemetryModel)
        .where(TelemetryModel.session_id == session_id)
        .order_by(desc(TelemetryModel.timestamp))
        .limit(limit)
    )
    return list(result.scalars().all())


async def scan_faults_for_session(
    db: AsyncSession, session_id: str, target_ecu: str = "ALL"
) -> List[FaultModel]:
    """
    Executes a simulated diagnostic scan across Scania / J1939 ECUs.
    If the simulator has active injected faults, returns them.
    Also injects standard known truck diagnostic trouble codes for demonstration.
    """
    sim = simulator_manager.get(session_id)
    now = time.time()

    # Pre-existing faults in DB for this session
    res = await db.execute(select(FaultModel).where(FaultModel.session_id == session_id))
    existing_faults = list(res.scalars().all())

    if existing_faults:
        return existing_faults

    # Generate initial realistic scan results for demonstration
    scan_results = []
    
    # 1. Scania Engine ECU (EMS) code: SPN 110 FMI 0 (Coolant High Warning)
    f1 = FaultModel(
        session_id=session_id,
        code="SPN 110 FMI 0 (Scania EMS 5104)",
        source_ecu="EMS (управление двигателем)",
        spn=110,
        fmi=0,
        oc=2,
        severity="WARNING",
        description="Температура охлаждающей жидкости превысила рабочий предел при высокой нагрузке.",
        possible_causes="Длительное движение на подъёме с высокой нагрузкой или неисправность муфты вентилятора охлаждения.",
        status="ACTIVE",
        timestamp=now - 320.0,
        simulated=True,
    )
    scan_results.append(f1)

    # 2. Scania Coordinator (COO8) code: SPN 168 FMI 14 (Battery Low Voltage Warning)
    f2 = FaultModel(
        session_id=session_id,
        code="SPN 168 FMI 14 (Scania COO 3218)",
        source_ecu="COO8 (координатор бортовых систем)",
        spn=168,
        fmi=14,
        oc=1,
        severity="INFO",
        description="Напряжение бортовой сети кратковременно опустилось ниже 23,8 В во время предпускового подогрева двигателя.",
        possible_causes="Высокая нагрузка предпускового подогревателя или износ аккумуляторных батарей.",
        status="PREVIOUSLY_ACTIVE",
        timestamp=now - 1450.0,
        simulated=True,
    )
    scan_results.append(f2)

    # Check if simulator has any additional active injected faults
    if sim and sim.active_faults:
        for fault_key, af in sim.active_faults.items():
            f_sim = FaultModel(
                session_id=session_id,
                code=af.code,
                source_ecu=af.source_ecu,
                spn=af.spn,
                fmi=af.fmi,
                oc=af.oc,
                severity=af.severity,
                description=af.description,
                possible_causes=af.possible_causes,
                status="ACTIVE",
                timestamp=af.timestamp,
                simulated=True,
            )
            scan_results.append(f_sim)

    for item in scan_results:
        db.add(item)
    await db.commit()
    return scan_results


async def create_diagnostic_report(
    db: AsyncSession, req: ReportCreateRequest
) -> ReportModel:
    session = await get_session_by_id(db, req.session_id)
    if not session:
        raise ValueError(f"Session '{req.session_id}' not found")

    truck = await get_truck_by_id(db, session.truck_id)
    truck_model_str = f"{truck.brand} {truck.model} ({truck.year})" if truck else "Грузовой автомобиль"
    truck_vin_str = truck.vin if truck else "не указан"

    # Get faults
    faults_res = await db.execute(
        select(FaultModel).where(FaultModel.session_id == req.session_id)
    )
    faults = list(faults_res.scalars().all())

    # Get latest telemetry
    sim = simulator_manager.get(req.session_id)
    telemetry_snap = sim.get_telemetry_snapshot() if sim else None

    # Summary construction
    active_count = sum(1 for f in faults if f.status == "ACTIVE")
    summary = (
        f"Отчёт TruckDiag по диагностике автомобиля {truck_model_str} (VIN: {truck_vin_str}). "
        f"Кодов неисправностей: {len(faults)}, активных: {active_count}. "
        "Режим симуляции. Протокол SAE J1939."
    )

    report_payload = {
        "report_id": None,
        "session_id": session.id,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "truck": {
            "model": truck_model_str,
            "vin": truck_vin_str,
            "engine": truck.engine_code if truck else "N/A",
            "transmission": truck.transmission_type if truck else "N/A",
            "ecus": truck.ecu_list if truck else "N/A",
        },
        "technician": session.operator_name,
        "technician_notes": req.technician_notes or session.notes,
        "faults": [
            {
                "code": f.code,
                "ecu": f.source_ecu,
                "spn": f.spn,
                "fmi": f.fmi,
                "severity": f.severity,
                "description": f.description,
                "status": f.status,
            }
            for f in faults
        ],
        "telemetry_sample": (
            [p.model_dump() for p in telemetry_snap.parameters] if telemetry_snap else []
        ),
        "disclaimer": (
            "Отчёт сформирован по симулированным диагностическим данным. "
            "Прямое соединение с электронными блоками реального автомобиля не выполнялось."
        ),
    }

    report = ReportModel(
        session_id=session.id,
        created_at=utc_now(),
        title=req.title or "Отчёт диагностического сканирования",
        summary=summary,
        truck_model=truck_model_str,
        truck_vin=truck_vin_str,
        operator_name=session.operator_name,
        total_faults=len(faults),
        active_faults=active_count,
        report_data_json=json.dumps(report_payload),
    )
    db.add(report)
    await db.commit()
    await db.refresh(report)

    # Fill report_id in JSON
    report_payload["report_id"] = report.id
    report.report_data_json = json.dumps(report_payload)
    await db.commit()
    await db.refresh(report)

    return report


async def list_reports(db: AsyncSession, limit: int = 50) -> List[ReportModel]:
    res = await db.execute(
        select(ReportModel).order_by(desc(ReportModel.created_at)).limit(limit)
    )
    return list(res.scalars().all())


async def get_report_by_id(db: AsyncSession, report_id: str) -> Optional[ReportModel]:
    res = await db.execute(select(ReportModel).where(ReportModel.id == report_id))
    return res.scalars().first()
