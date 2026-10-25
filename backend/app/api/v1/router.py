from fastapi import APIRouter
from app.api.v1.endpoints import can, faults, health, reports, sessions, telemetry, trucks

api_router = APIRouter()

api_router.include_router(health.router, tags=["System & Health"])
api_router.include_router(trucks.router, prefix="/trucks", tags=["Trucks"])
api_router.include_router(sessions.router, prefix="/sessions", tags=["Diagnostic Sessions"])
api_router.include_router(telemetry.router, prefix="/telemetry", tags=["Telemetry"])
api_router.include_router(faults.router, tags=["Diagnostic Trouble Codes (DTC)"])
api_router.include_router(can.router, prefix="/can", tags=["CAN / J1939 Protocol"])

# J1939 specific sub-router from can
j1939_router = APIRouter()
j1939_router.add_api_route("/parameters", can.get_j1939_parameters, methods=["GET"], tags=["CAN / J1939 Protocol"])
j1939_router.add_api_route("/pgns", can.get_j1939_pgns, methods=["GET"], tags=["CAN / J1939 Protocol"])
api_router.include_router(j1939_router, prefix="/j1939")

api_router.include_router(reports.router, prefix="/reports", tags=["Reports"])
