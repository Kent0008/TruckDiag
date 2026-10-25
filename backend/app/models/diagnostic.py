import uuid
from datetime import datetime, timezone
from typing import Optional
from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
)
from sqlalchemy.orm import relationship
from app.core.database import Base


def generate_uuid() -> str:
    return str(uuid.uuid4())


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class TruckModel(Base):
    __tablename__ = "trucks"

    id = Column(String(64), primary_key=True)
    brand = Column(String(64), nullable=False)
    model = Column(String(64), nullable=False)
    year = Column(Integer, nullable=False)
    vin = Column(String(32), nullable=False, unique=True)
    engine_code = Column(String(64), nullable=False)
    transmission_type = Column(String(64), nullable=False)
    ecu_list = Column(Text, nullable=False)  # JSON or comma-separated ECUs
    simulated = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)

    sessions = relationship("SessionModel", back_populates="truck")


class SessionModel(Base):
    __tablename__ = "diagnostic_sessions"

    id = Column(String(64), primary_key=True, default=generate_uuid)
    truck_id = Column(String(64), ForeignKey("trucks.id"), nullable=False)
    operator_name = Column(String(128), default="Technician", nullable=False)
    notes = Column(Text, default="", nullable=False)
    connection_mode = Column(String(32), default="SIMULATOR", nullable=False)
    status = Column(String(32), default="CREATED", nullable=False)  # CREATED, RUNNING, STOPPED
    start_time = Column(DateTime(timezone=True), nullable=True)
    stop_time = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)

    truck = relationship("TruckModel", back_populates="sessions")
    telemetry = relationship("TelemetryModel", back_populates="session", cascade="all, delete-orphan")
    faults = relationship("FaultModel", back_populates="session", cascade="all, delete-orphan")
    can_frames = relationship("CanFrameModel", back_populates="session", cascade="all, delete-orphan")
    reports = relationship("ReportModel", back_populates="session", cascade="all, delete-orphan")


class TelemetryModel(Base):
    __tablename__ = "telemetry_records"

    id = Column(Integer, primary_key=True, autoincrement=True)
    session_id = Column(String(64), ForeignKey("diagnostic_sessions.id"), nullable=False, index=True)
    timestamp = Column(Float, nullable=False, index=True)
    parameter_name = Column(String(64), nullable=False, index=True)
    value = Column(Float, nullable=False)
    unit = Column(String(32), nullable=False)
    quality = Column(String(16), default="GOOD", nullable=False)
    category = Column(String(32), default="ENGINE", nullable=False)
    simulated = Column(Boolean, default=True, nullable=False)

    session = relationship("SessionModel", back_populates="telemetry")


class FaultModel(Base):
    __tablename__ = "fault_records"

    id = Column(String(64), primary_key=True, default=generate_uuid)
    session_id = Column(String(64), ForeignKey("diagnostic_sessions.id"), nullable=False, index=True)
    code = Column(String(64), nullable=False)
    source_ecu = Column(String(64), nullable=False)
    spn = Column(Integer, nullable=True)
    fmi = Column(Integer, nullable=True)
    oc = Column(Integer, default=1, nullable=False)
    severity = Column(String(32), default="WARNING", nullable=False)  # Backward compatible (INFO, WARNING, CRITICAL)
    description = Column(Text, nullable=False)
    possible_causes = Column(Text, default="")
    status = Column(String(32), default="ACTIVE", nullable=False)  # ACTIVE, PREVIOUSLY_ACTIVE, PENDING
    timestamp = Column(Float, nullable=False)
    simulated = Column(Boolean, default=True, nullable=False)

    # Rich human-friendly Russian diagnostic fields
    user_title = Column(String(256), default="", nullable=False)
    explanation = Column(Text, default="", nullable=False)
    symptoms = Column(Text, default="", nullable=False)
    self_check_steps = Column(Text, default="", nullable=False)
    specialist_steps = Column(Text, default="", nullable=False)
    urgency = Column(String(64), default="Средняя", nullable=False)  # "Критическая", "Высокая", "Средняя", "Низкая"
    driving_recommendation = Column(Text, default="", nullable=False)
    fidelity = Column(String(128), default="База знаний TruckDiag", nullable=False)

    session = relationship("SessionModel", back_populates="faults")


class CanFrameModel(Base):
    __tablename__ = "can_frame_records"

    id = Column(String(64), primary_key=True, default=generate_uuid)
    session_id = Column(String(64), ForeignKey("diagnostic_sessions.id"), nullable=False, index=True)
    can_id = Column(Integer, nullable=False)
    can_id_hex = Column(String(16), nullable=False)
    priority = Column(Integer, nullable=False)
    pgn = Column(Integer, nullable=False, index=True)
    pgn_hex = Column(String(16), nullable=False)
    pgn_name = Column(String(128), nullable=False)
    source_address = Column(Integer, nullable=False)
    source_name = Column(String(128), nullable=False)
    dlc = Column(Integer, nullable=False)
    payload_hex = Column(String(64), nullable=False)
    timestamp = Column(Float, nullable=False)
    simulated = Column(Boolean, default=True, nullable=False)

    session = relationship("SessionModel", back_populates="can_frames")


class ReportModel(Base):
    __tablename__ = "diagnostic_reports"

    id = Column(String(64), primary_key=True, default=generate_uuid)
    session_id = Column(String(64), ForeignKey("diagnostic_sessions.id"), nullable=False, index=True)
    created_at = Column(DateTime(timezone=True), default=utc_now, nullable=False)
    title = Column(String(256), nullable=False)
    summary = Column(Text, nullable=False)
    truck_model = Column(String(128), nullable=False)
    truck_vin = Column(String(32), nullable=False)
    operator_name = Column(String(128), nullable=False)
    total_faults = Column(Integer, default=0, nullable=False)
    active_faults = Column(Integer, default=0, nullable=False)
    report_data_json = Column(Text, nullable=False)  # Full JSON snapshot

    session = relationship("SessionModel", back_populates="reports")
