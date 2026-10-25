import asyncio
import math
import random
import time
from enum import Enum
from typing import Any, Callable, Dict, List, Optional, Set
from app.protocols.can_frame import CanFrame
from app.protocols.knowledge_base import PARAMETER_METADATA_RU, lookup_dtc_info
from app.protocols.j1939 import (
    encode_air1,
    encode_ccvs1,
    encode_dd,
    encode_dm1,
    encode_eec1,
    encode_eec2,
    encode_eflp1,
    encode_et1,
    encode_etc2,
    encode_vep1,
)
from app.schemas.diagnostic import TelemetryMessage, TelemetryParameter


class EngineState(str, Enum):
    ENGINE_OFF = "ENGINE_OFF"
    IDLE = "IDLE"
    DRIVING = "DRIVING"
    STOPPING = "STOPPING"


class ActiveFault:
    def __init__(
        self,
        key: str,
        code: str,
        source_ecu: str,
        spn: Optional[int],
        fmi: Optional[int],
        oc: int,
        severity: str,
        description: str,
        possible_causes: str,
        user_title: str = "",
        explanation: str = "",
        symptoms: str = "",
        self_check_steps: str = "",
        specialist_steps: str = "",
        urgency: str = "Средняя",
        driving_recommendation: str = "",
        fidelity: str = "База знаний TruckDiag J1939 (Проверено)",
    ):
        self.key = key
        self.code = code
        self.source_ecu = source_ecu
        self.spn = spn
        self.fmi = fmi
        self.oc = oc
        self.severity = severity
        self.description = description
        self.possible_causes = possible_causes
        self.user_title = user_title
        self.explanation = explanation
        self.symptoms = symptoms
        self.self_check_steps = self_check_steps
        self.specialist_steps = specialist_steps
        self.urgency = urgency
        self.driving_recommendation = driving_recommendation
        self.fidelity = fidelity
        self.timestamp = time.time()


class TruckSimulator:
    """
    Realistic Scania/J1939 Truck Diagnostic Simulator.
    Simulates physical dynamics (inertia, thermal delay, gear shifting, air consumption)
    and encodes standard SAE J1939 CAN 2.0B 29-bit frames.
    """

    def __init__(self, session_id: str, truck_id: str = "scania-r500-2021"):
        self.session_id = session_id
        self.truck_id = truck_id
        self.state: EngineState = EngineState.IDLE
        self.is_running: bool = False

        # Physical parameters
        self.rpm: float = 600.0
        self.target_rpm: float = 600.0
        self.speed: float = 0.0
        self.target_speed: float = 0.0
        self.gear: int = 0
        self.engine_load: float = 18.0
        self.coolant_temp: float = 82.0
        self.oil_pressure: float = 2.8
        self.battery_voltage: float = 28.3
        self.air_pressure_circuit1: float = 10.2
        self.fuel_level: float = 76.4
        self.engine_hours: float = 4521.8

        # Faults management
        self.active_faults: Dict[str, ActiveFault] = {}
        self._init_default_faults()

        # CAN frames buffer (ring buffer of latest 100 frames)
        self.can_frames_buffer: List[CanFrame] = []
        self.max_buffer_size: int = 100

        # Subscribed WebSocket callbacks
        self.listeners: Set[Callable[[TelemetryMessage], Any]] = set()

        self._task: Optional[asyncio.Task] = None
        self._step_counter: int = 0

    def _init_default_faults(self) -> None:
        # Predefined test fault available for injection
        pass

    def inject_fault(self, fault_key: str) -> bool:
        """Injects a diagnostic trouble code into the simulated ECUs with rich knowledge base lookup."""
        if fault_key == "COOLANT_OVERHEAT":
            info = lookup_dtc_info("SPN 110 FMI 0 (Scania EMS 5104)", spn=110, fmi=0, source_ecu="EMS (Блок управления двигателем)")
            self.active_faults["COOLANT_OVERHEAT"] = ActiveFault(
                key="COOLANT_OVERHEAT",
                code=info.code,
                source_ecu=info.source_ecu,
                spn=info.spn,
                fmi=info.fmi,
                oc=2,
                severity="CRITICAL",
                description=info.explanation,
                possible_causes=info.symptoms,
                user_title=info.user_title,
                explanation=info.explanation,
                symptoms=info.symptoms,
                self_check_steps=info.self_check_steps,
                specialist_steps=info.specialist_steps,
                urgency=info.urgency,
                driving_recommendation=info.driving_recommendation,
                fidelity=info.fidelity,
            )
            return True
        elif fault_key == "LOW_OIL_PRESSURE":
            info = lookup_dtc_info("SPN 100 FMI 1 (Scania EMS 5218)", spn=100, fmi=1, source_ecu="EMS (Блок управления двигателем)")
            self.active_faults["LOW_OIL_PRESSURE"] = ActiveFault(
                key="LOW_OIL_PRESSURE",
                code=info.code,
                source_ecu=info.source_ecu,
                spn=info.spn,
                fmi=info.fmi,
                oc=1,
                severity="CRITICAL",
                description=info.explanation,
                possible_causes=info.symptoms,
                user_title=info.user_title,
                explanation=info.explanation,
                symptoms=info.symptoms,
                self_check_steps=info.self_check_steps,
                specialist_steps=info.specialist_steps,
                urgency=info.urgency,
                driving_recommendation=info.driving_recommendation,
                fidelity=info.fidelity,
            )
            return True
        elif fault_key == "LOW_AIR_PRESSURE":
            info = lookup_dtc_info("SPN 117 FMI 1 (Scania APS 4310)", spn=117, fmi=1, source_ecu="EBS / APS (Тормозная система и пневматика)")
            self.active_faults["LOW_AIR_PRESSURE"] = ActiveFault(
                key="LOW_AIR_PRESSURE",
                code=info.code,
                source_ecu=info.source_ecu,
                spn=info.spn,
                fmi=info.fmi,
                oc=3,
                severity="CRITICAL",
                description=info.explanation,
                possible_causes=info.symptoms,
                user_title=info.user_title,
                explanation=info.explanation,
                symptoms=info.symptoms,
                self_check_steps=info.self_check_steps,
                specialist_steps=info.specialist_steps,
                urgency=info.urgency,
                driving_recommendation=info.driving_recommendation,
                fidelity=info.fidelity,
            )
            return True
        elif fault_key == "SCR_NOX_EXCEEDANCE":
            info = lookup_dtc_info("SPN 4094 FMI 18 (Scania EEC 6108)", spn=4094, fmi=18, source_ecu="EEC (Система нейтрализации выхлопа SCR)")
            self.active_faults["SCR_NOX_EXCEEDANCE"] = ActiveFault(
                key="SCR_NOX_EXCEEDANCE",
                code=info.code,
                source_ecu=info.source_ecu,
                spn=info.spn,
                fmi=info.fmi,
                oc=5,
                severity="WARNING",
                description=info.explanation,
                possible_causes=info.symptoms,
                user_title=info.user_title,
                explanation=info.explanation,
                symptoms=info.symptoms,
                self_check_steps=info.self_check_steps,
                specialist_steps=info.specialist_steps,
                urgency=info.urgency,
                driving_recommendation=info.driving_recommendation,
                fidelity=info.fidelity,
            )
            return True
        elif fault_key == "UNKNOWN_CODE":
            # Uncataloged test code to test fallback graceful handling
            info = lookup_dtc_info("SPN 520201 FMI 31 (Scania OEM BUP)", spn=520201, fmi=31, source_ecu="BUP (Резервный контроллер)")
            self.active_faults["UNKNOWN_CODE"] = ActiveFault(
                key="UNKNOWN_CODE",
                code=info.code,
                source_ecu=info.source_ecu,
                spn=info.spn,
                fmi=info.fmi,
                oc=1,
                severity="INFO",
                description=info.explanation,
                possible_causes=info.symptoms,
                user_title=info.user_title,
                explanation=info.explanation,
                symptoms=info.symptoms,
                self_check_steps=info.self_check_steps,
                specialist_steps=info.specialist_steps,
                urgency=info.urgency,
                driving_recommendation=info.driving_recommendation,
                fidelity=info.fidelity,
            )
            return True
        return False

    def apply_scenario(self, scenario: str) -> List[str]:
        """
        Applies a predefined demonstration scenario:
        - NORMAL: All systems optimal, no DTCs
        - WARNING: Minor battery voltage dip or AdBlue reagent alert
        - CRITICAL: Severe engine overheat + low oil pressure
        - UNKNOWN_CODE: Unconfirmed factory code to verify UI fallback
        """
        scenario_upper = scenario.upper()
        self.active_faults.clear()

        if scenario_upper == "NORMAL":
            self.coolant_temp = 85.0
            self.oil_pressure = 2.8
            self.air_pressure_circuit1 = 10.2
            self.battery_voltage = 28.3
            return []
        elif scenario_upper == "WARNING":
            self.inject_fault("SCR_NOX_EXCEEDANCE")
            self.battery_voltage = 23.4
            return ["SCR_NOX_EXCEEDANCE"]
        elif scenario_upper == "CRITICAL":
            self.inject_fault("COOLANT_OVERHEAT")
            self.inject_fault("LOW_OIL_PRESSURE")
            self.inject_fault("LOW_AIR_PRESSURE")
            return ["COOLANT_OVERHEAT", "LOW_OIL_PRESSURE", "LOW_AIR_PRESSURE"]
        elif scenario_upper == "UNKNOWN_CODE":
            self.inject_fault("UNKNOWN_CODE")
            return ["UNKNOWN_CODE"]
        return []

    def clear_fault(self, fault_key: str) -> bool:
        if fault_key in self.active_faults:
            del self.active_faults[fault_key]
            return True
        return False

    def set_engine_state(self, new_state: EngineState) -> None:
        self.state = new_state
        if new_state == EngineState.ENGINE_OFF:
            self.target_rpm = 0.0
            self.target_speed = 0.0
            self.gear = 0
        elif new_state == EngineState.IDLE:
            self.target_rpm = 600.0
            self.target_speed = 0.0
            self.gear = 0
        elif new_state == EngineState.DRIVING:
            self.target_speed = 78.0
            self.target_rpm = 1250.0
        elif new_state == EngineState.STOPPING:
            self.target_speed = 0.0
            self.target_rpm = 600.0

    def register_listener(self, callback: Callable[[TelemetryMessage], Any]) -> None:
        self.listeners.add(callback)

    def unregister_listener(self, callback: Callable[[TelemetryMessage], Any]) -> None:
        self.listeners.discard(callback)

    async def start(self) -> None:
        if self.is_running:
            return
        self.is_running = True
        self._task = asyncio.create_task(self._simulation_loop())

    async def stop(self) -> None:
        self.is_running = False
        if self._task and not self._task.done():
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass

    async def _simulation_loop(self) -> None:
        while self.is_running:
            try:
                self._update_physics()
                self._generate_can_frames()
                msg = self.get_telemetry_snapshot()

                # Dispatch to listeners
                for listener in list(self.listeners):
                    try:
                        res = listener(msg)
                        if asyncio.iscoroutine(res):
                            await res
                    except Exception:
                        pass

                await asyncio.sleep(1.0)
            except asyncio.CancelledError:
                break
            except Exception:
                await asyncio.sleep(1.0)

    def _update_physics(self) -> None:
        self._step_counter += 1

        # Smooth physical simulation
        if self.state == EngineState.ENGINE_OFF:
            self.rpm = max(0.0, self.rpm - 150.0)
            self.speed = max(0.0, self.speed - 5.0)
            self.gear = 0
            self.engine_load = 0.0
            self.coolant_temp = max(22.0, self.coolant_temp - 0.2)
            self.oil_pressure = max(0.0, self.oil_pressure - 0.5)
            self.battery_voltage = round(24.2 + 0.1 * math.sin(self._step_counter * 0.1), 2)

        elif self.state == EngineState.IDLE:
            # Smoothly reach 580-620 RPM idle with tiny realistic vibration
            jitter = random.uniform(-10.0, 10.0)
            self.rpm += (600.0 - self.rpm) * 0.3 + jitter
            self.speed = max(0.0, self.speed - 3.0)
            self.gear = 0
            self.engine_load = round(17.0 + random.uniform(-1.5, 1.5), 1)
            # Warm up towards 85°C
            self.coolant_temp += (86.0 - self.coolant_temp) * 0.05
            self.oil_pressure = round(2.6 + random.uniform(-0.1, 0.1), 2)
            self.battery_voltage = round(28.3 + random.uniform(-0.1, 0.1), 2)
            # Air pressure recharges slowly to 10.5 bar
            self.air_pressure_circuit1 = min(10.5, round(self.air_pressure_circuit1 + 0.05, 2))

        elif self.state == EngineState.DRIVING:
            # Smooth acceleration towards target speed (e.g. 78 km/h)
            speed_diff = self.target_speed - self.speed
            self.speed += speed_diff * 0.15 + random.uniform(-0.2, 0.2)
            self.speed = max(0.0, min(90.0, self.speed))

            # Opticruise 12-speed transmission emulation based on speed
            if self.speed < 5.0:
                self.gear = 2
                gear_ratio = 12.0
            elif self.speed < 18.0:
                self.gear = 4
                gear_ratio = 6.5
            elif self.speed < 35.0:
                self.gear = 6
                gear_ratio = 3.8
            elif self.speed < 55.0:
                self.gear = 9
                gear_ratio = 2.1
            elif self.speed < 72.0:
                self.gear = 11
                gear_ratio = 1.3
            else:
                self.gear = 12
                gear_ratio = 1.0

            # Realistic RPM calculation from speed & gear
            calculated_rpm = 950.0 + (self.speed % 15.0) * 35.0 + random.uniform(-15.0, 15.0)
            self.rpm += (calculated_rpm - self.rpm) * 0.4
            self.engine_load = round(52.0 + (self.speed / 90.0) * 25.0 + random.uniform(-3.0, 3.0), 1)
            self.coolant_temp += (89.0 - self.coolant_temp) * 0.08
            self.oil_pressure = round(4.4 + (self.rpm / 2000.0) * 0.8 + random.uniform(-0.1, 0.1), 2)
            self.battery_voltage = round(28.4 + random.uniform(-0.05, 0.05), 2)
            self.fuel_level = max(5.0, round(self.fuel_level - 0.005, 3))
            self.engine_hours = round(self.engine_hours + 1.0 / 3600.0, 4)

        elif self.state == EngineState.STOPPING:
            self.speed = max(0.0, self.speed - 6.5)
            if self.speed < 2.0:
                self.state = EngineState.IDLE
                self.gear = 0
            else:
                self.gear = max(1, int(self.speed / 8.0))
            self.rpm += (750.0 - self.rpm) * 0.3
            self.engine_load = round(5.0 + random.uniform(0.0, 2.0), 1)
            # Service brakes consume air during stopping
            self.air_pressure_circuit1 = max(7.5, round(self.air_pressure_circuit1 - 0.15, 2))
            self.oil_pressure = round(2.8 + random.uniform(-0.1, 0.1), 2)

        # Apply active faults overrides if injected
        if "COOLANT_OVERHEAT" in self.active_faults:
            self.coolant_temp = min(114.0, self.coolant_temp + 1.2)
        if "LOW_OIL_PRESSURE" in self.active_faults:
            self.oil_pressure = max(0.6, self.oil_pressure - 0.3)
        if "LOW_AIR_PRESSURE" in self.active_faults:
            self.air_pressure_circuit1 = max(5.1, self.air_pressure_circuit1 - 0.25)

    def _generate_can_frames(self) -> None:
        """Emits bit-accurate J1939 CAN frames for current state."""
        now = time.time()
        frames_to_add: List[CanFrame] = []

        # 1. PGN 61444 (EEC1) - Priority 3, Engine SA = 0x00 -> CAN ID: 0x0CF00400
        p_eec1 = encode_eec1(rpm=self.rpm, actual_torque_pct=self.engine_load * 0.6)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x0CF00400,
            payload_bytes=p_eec1,
            timestamp=now,
            simulated=True,
        ))

        # 2. PGN 61443 (EEC2) - Priority 3, Engine SA = 0x00 -> CAN ID: 0x0CF00300
        p_eec2 = encode_eec2(load_pct=self.engine_load)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x0CF00300,
            payload_bytes=p_eec2,
            timestamp=now,
            simulated=True,
        ))

        # 3. PGN 65262 (ET1) - Priority 6, Engine SA = 0x00 -> CAN ID: 0x18FEEE00 (0xFEEE = 65262)
        p_et1 = encode_et1(coolant_c=self.coolant_temp)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x18FEEE00,
            payload_bytes=p_et1,
            timestamp=now,
            simulated=True,
        ))

        # 4. PGN 65265 (CCVS1) - Priority 6, Coordinator SA = 0x21 -> CAN ID: 0x18FEF121
        p_ccvs = encode_ccvs1(speed_kmh=self.speed)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x18FEF121,
            payload_bytes=p_ccvs,
            timestamp=now,
            simulated=True,
        ))

        # 5. PGN 65271 (VEP1) - Priority 6, Coordinator SA = 0x21 -> CAN ID: 0x18FEF721
        p_vep = encode_vep1(voltage=self.battery_voltage)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x18FEF721,
            payload_bytes=p_vep,
            timestamp=now,
            simulated=True,
        ))

        # 6. PGN 61445 (ETC2) - Priority 3, Transmission SA = 0x03 -> CAN ID: 0x0CF00503
        p_etc2 = encode_etc2(gear=self.gear)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x0CF00503,
            payload_bytes=p_etc2,
            timestamp=now,
            simulated=True,
        ))

        # 7. PGN 65274 (AIR1) - Priority 6, Brakes/APS SA = 0x0B -> CAN ID: 0x18FEFA0B
        p_air = encode_air1(brake_circuit1_bar=self.air_pressure_circuit1)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x18FEFA0B,
            payload_bytes=p_air,
            timestamp=now,
            simulated=True,
        ))

        # 8. PGN 65263 (EFL/P1) - Priority 6, Engine SA = 0x00 -> CAN ID: 0x18FEE700
        p_efl = encode_eflp1(oil_press_bar=self.oil_pressure)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x18FEE700,
            payload_bytes=p_efl,
            timestamp=now,
            simulated=True,
        ))

        # 9. PGN 65276 (DD) - Priority 6, Instrument Cluster SA = 0x17 -> CAN ID: 0x18FEFC17
        p_dd = encode_dd(fuel_pct=self.fuel_level)
        frames_to_add.append(CanFrame.from_raw(
            can_id=0x18FEFC17,
            payload_bytes=p_dd,
            timestamp=now,
            simulated=True,
        ))

        # 10. PGN 65226 (DM1 Active DTCs) if any faults active
        for fault in self.active_faults.values():
            p_dm1 = encode_dm1(spn=fault.spn, fmi=fault.fmi, oc=fault.oc)
            # CAN ID: 0x18FECA00 (From Engine) or appropriate ECU
            frames_to_add.append(CanFrame.from_raw(
                can_id=0x18FECA00,
                payload_bytes=p_dm1,
                timestamp=now,
                simulated=True,
            ))

        # Append to buffer
        self.can_frames_buffer.extend(frames_to_add)
        if len(self.can_frames_buffer) > self.max_buffer_size:
            self.can_frames_buffer = self.can_frames_buffer[-self.max_buffer_size:]

    def get_telemetry_snapshot(self) -> TelemetryMessage:
        now = time.time()
        cur_state = self.state.value

        raw_param_defs = [
            ("Engine Speed", round(self.rpm, 1), "об/мин", "GOOD" if self.rpm < 2400 else "DEGRADED", "ENGINE"),
            ("Vehicle Speed", round(self.speed, 1), "км/ч", "GOOD", "ENGINE"),
            ("Engine Coolant Temperature", round(self.coolant_temp, 1), "°C", "GOOD" if self.coolant_temp < 102 else "BAD", "ENGINE"),
            ("Engine Percent Load", round(self.engine_load, 1), "%", "GOOD", "ENGINE"),
            ("Engine Oil Pressure", round(self.oil_pressure, 2), "бар", "GOOD" if self.oil_pressure > 1.2 else "BAD", "ENGINE"),
            ("Battery System Voltage", round(self.battery_voltage, 2), "В", "GOOD" if 23.5 <= self.battery_voltage <= 29.5 else "DEGRADED", "ELECTRICAL"),
            ("Transmission Current Gear", float(self.gear), "передача", "GOOD", "TRANSMISSION"),
            ("Brake Circuit 1 Air Pressure", round(self.air_pressure_circuit1, 2), "бар", "GOOD" if self.air_pressure_circuit1 >= 6.5 else "BAD", "BRAKES"),
            ("Fuel Level", round(self.fuel_level, 1), "%", "GOOD" if self.fuel_level > 15 else "DEGRADED", "ELECTRICAL"),
        ]

        params = []
        for eng_name, val, fallback_unit, quality, cat in raw_param_defs:
            meta = PARAMETER_METADATA_RU.get(eng_name, {})
            name_ru = meta.get("name_ru", eng_name)
            unit_ru = meta.get("unit", fallback_unit)
            desc_ru = meta.get("description_ru", "")
            ranges = meta.get("normal_ranges", {})
            range_str = ranges.get(cur_state, ranges.get("IDLE", "Норма не установлена"))

            params.append(
                TelemetryParameter(
                    name=eng_name,
                    name_ru=name_ru,
                    value=val,
                    unit=unit_ru,
                    quality=quality,
                    category=cat,
                    description_ru=desc_ru,
                    normal_range=range_str,
                    simulated=True,
                )
            )

        return TelemetryMessage(
            timestamp=now,
            session_id=self.session_id,
            source="SIMULATED_J1939",
            parameters=params,
            engine_state=self.state.value,
            simulated=True,
        )

    def get_recent_can_frames(self, limit: int = 50) -> List[CanFrame]:
        return self.can_frames_buffer[-limit:]
