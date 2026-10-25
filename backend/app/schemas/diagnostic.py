from datetime import datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


# System & Health
class HealthResponse(BaseModel):
    status: str = "ok"
    timestamp: float
    database: str = "connected"
    simulator: str = "ready"
    version: str


class SystemInfoResponse(BaseModel):
    project_name: str
    api_version: str
    protocol_version: str = "SAE J1939-2023"
    supported_protocols: List[str] = ["SAE J1939", "CAN 2.0B (29-bit)", "Scania Diagnostics (Read-Only Simulation)"]
    available_adapters: List[str] = [
        "SIMULATOR_VIRTUAL_BUS (Active)",
        "SOCKETCAN_INTERFACE (Prepared)",
        "RP1210_PASSTHRU (Prepared)",
    ]
    supported_truck_platforms: List[str] = [
        "Scania серии R/S (2019–2023, Euro 6)",
        "Volvo FH (с 2014 года, архитектура J1939)",
    ]
    environment: str = "local_development"


# Trucks
class TruckResponse(BaseModel):
    id: str
    brand: str
    model: str
    year: int
    vin: str
    engine_code: str
    transmission_type: str
    ecu_list: List[str]
    simulated: bool = True


# Sessions
class SessionCreateRequest(BaseModel):
    truck_id: str = Field(..., description="Target truck ID, e.g. 'scania-r500-2021'")
    operator_name: str = Field(default="Diagnostic Specialist", description="Technician name")
    notes: Optional[str] = Field(default="", description="Session purpose or symptoms notes")
    connection_mode: str = Field(default="SIMULATOR", description="SIMULATOR or HARDWARE")


class SessionResponse(BaseModel):
    id: str
    truck_id: str
    operator_name: str
    notes: str
    connection_mode: str
    status: str  # CREATED, RUNNING, STOPPED
    start_time: Optional[datetime] = None
    stop_time: Optional[datetime] = None
    created_at: datetime
    truck: Optional[TruckResponse] = None


class SessionStatusResponse(BaseModel):
    session_id: str
    status: str  # CREATED, RUNNING, STOPPED
    connection_state: str  # CONNECTED, CONNECTING, DISCONNECTED
    data_source: str  # SIMULATED_J1939, VIRTUAL_CAN0
    uptime_seconds: float
    active_ecus: int
    frames_per_second: float
    simulated: bool = True


# Telemetry
class TelemetryParameter(BaseModel):
    name: str
    name_ru: str = ""
    value: float
    unit: str
    quality: str = "GOOD"  # GOOD, DEGRADED, BAD
    category: str = "ENGINE"  # ENGINE, TRANSMISSION, BRAKES, ELECTRICAL, AFTERTREATMENT
    description_ru: str = ""
    normal_range: str = ""
    simulated: bool = True


class TelemetryMessage(BaseModel):
    timestamp: float
    session_id: str
    source: str = "SIMULATED_J1939"
    parameters: List[TelemetryParameter]
    engine_state: str = "IDLE"  # ENGINE_OFF, IDLE, DRIVING, STOPPING
    simulated: bool = True


class TelemetryHistoryResponse(BaseModel):
    session_id: str
    count: int
    records: List[Dict[str, Any]]


# Faults
class FaultResponse(BaseModel):
    id: str
    session_id: str
    code: str
    source_ecu: str
    spn: Optional[int] = None
    fmi: Optional[int] = None
    oc: int
    severity: str  # INFO, WARNING, CRITICAL
    description: str
    possible_causes: str
    status: str  # ACTIVE, PREVIOUSLY_ACTIVE, PENDING
    timestamp: float
    simulated: bool = True

    # Human-friendly fields for drivers and mechanics
    user_title: str = ""
    explanation: str = ""
    symptoms: str = ""
    self_check_steps: str = ""
    specialist_steps: str = ""
    urgency: str = "Средняя"  # Критическая, Высокая, Средняя, Низкая
    driving_recommendation: str = ""
    fidelity: str = "База знаний TruckDiag J1939 (Проверено)"


class FaultScanRequest(BaseModel):
    target_ecu: Optional[str] = Field(default="ALL", description="Target ECU or ALL")


class FaultScanResponse(BaseModel):
    session_id: str
    scanned_ecus: List[str]
    total_found: int
    faults: List[FaultResponse]
    simulated: bool = True
    disclaimer: str = (
        "Симуляционное сканирование. Физическое подключение к реальному автомобилю отсутствует. "
        "Очистка реальных кодов неисправностей отключена в целях безопасности."
    )


class SimulatorScenarioRequest(BaseModel):
    scenario: str = Field(..., description="'NORMAL', 'WARNING', 'CRITICAL', 'UNKNOWN_CODE'")


# CAN / J1939
class CanFrameResponse(BaseModel):
    id: Optional[str] = None
    session_id: Optional[str] = None
    can_id: int
    can_id_hex: str
    priority: int
    pgn: int
    pgn_hex: str
    pgn_name: str
    source_address: int
    source_name: str
    destination_address: Optional[int] = 255
    dlc: int
    payload_hex: str
    timestamp: float
    simulated: bool = True


class CanDecodeRequest(BaseModel):
    can_id: Any = Field(..., description="29-bit CAN ID as integer (e.g. 217056256) or hex string ('0x0CF00400')")
    payload: str = Field(..., description="Hex string of CAN data payload (1-8 bytes, e.g. 'F07D84204EFFFFF0')")


class DecodedSpnInfo(BaseModel):
    spn: int
    name: str
    raw_value: Any
    scaled_value: Any
    unit: str
    description: str
    fidelity: str = "VERIFIED_SAE_J1939_STANDARD"
    is_valid: bool = True


class CanDecodeResponse(BaseModel):
    can_id: int
    can_id_hex: str
    priority: int
    pgn: int
    pgn_hex: str
    pgn_name: str
    source_address: int
    source_name: str
    destination_address: Optional[int]
    dlc: int
    payload_hex: str
    fidelity: str
    decoded_spns: List[DecodedSpnInfo]
    simulated: bool = True


class J1939ParameterInfo(BaseModel):
    spn: int
    name: str
    pgn: int
    resolution: float
    offset: float
    unit: str
    range: str
    fidelity: str
    description: str


class J1939PgnInfo(BaseModel):
    pgn: int
    acronym: str
    name: str
    rate_ms: int
    priority: int
    source: str
    fidelity: str
    description: str


# Reports
class ReportCreateRequest(BaseModel):
    session_id: str
    title: Optional[str] = "Отчёт диагностического сканирования"
    technician_notes: Optional[str] = ""


class ReportResponse(BaseModel):
    id: str
    session_id: str
    created_at: datetime
    title: str
    summary: str
    truck_model: str
    truck_vin: str
    operator_name: str
    total_faults: int
    active_faults: int


class ReportDetailResponse(ReportResponse):
    report_data: Dict[str, Any]
