from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class DecodedSpn(BaseModel):
    spn: int
    name: str
    raw_value: Any
    scaled_value: Any
    unit: str
    description: str
    fidelity: str = "VERIFIED_SAE_J1939_STANDARD"
    is_valid: bool = True
    error_message: Optional[str] = None


class J1939DecodedFrame(BaseModel):
    can_id_hex: str
    can_id: int
    priority: int
    pgn: int
    pgn_hex: str
    pgn_name: str
    source_address: int
    source_name: str
    destination_address: Optional[int]
    dlc: int
    payload_hex: str
    spns: List[DecodedSpn]
    fidelity: str = "VERIFIED_SAE_J1939_STANDARD"
    simulated: bool = True


# Standard J1939 Source Addresses
SOURCE_ADDRESS_MAP: Dict[int, str] = {
    0x00: "Engine #1 (EMS)",
    0x01: "Engine #2",
    0x03: "Transmission (GMS / Opticruise / I-Shift)",
    0x0B: "Brakes - System Controller (EBS)",
    0x10: "Retarder, Engine",
    0x17: "Instrument Cluster (IC)",
    0x21: "Body Controller / Coordinator (COO)",
    0x27: "Retarder, Driveline",
    0x29: "Air Suspension (SMS)",
    0x31: "Exhaust Emission System / Aftertreatment (SCR/DPF)",
    0x3D: "Tire Pressure Controller (TPMS)",
    0xFD: "Off-board Diagnostic-Service Tool #1",
    0xFE: "Off-board Diagnostic-Service Tool #2",
    0xFF: "Global / Broadcast",
}


# Known PGNs with fidelity descriptions
PGN_REGISTRY: Dict[int, Dict[str, Any]] = {
    61444: {
        "acronym": "EEC1",
        "name": "Electronic Engine Controller 1",
        "rate_ms": 20,
        "priority": 3,
        "source": "Engine #1 (0x00)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Engine speed, actual torque, driver demand torque.",
    },
    61443: {
        "acronym": "EEC2",
        "name": "Electronic Engine Controller 2",
        "rate_ms": 50,
        "priority": 3,
        "source": "Engine #1 (0x00)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Accelerator pedal position, engine percent load at current speed.",
    },
    65262: {
        "acronym": "ET1",
        "name": "Engine Temperature 1",
        "rate_ms": 1000,
        "priority": 6,
        "source": "Engine #1 (0x00)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Coolant temperature, fuel temperature, oil temperature.",
    },
    65265: {
        "acronym": "CCVS1",
        "name": "Cruise Control / Vehicle Speed 1",
        "rate_ms": 100,
        "priority": 6,
        "source": "Coordinator (0x21) / Engine (0x00)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Wheel-based vehicle speed, parking brake switch status.",
    },
    65271: {
        "acronym": "VEP1",
        "name": "Vehicle Electrical Power 1",
        "rate_ms": 1000,
        "priority": 6,
        "source": "Coordinator (0x21) / Alternator (0x00)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Battery potential / System voltage, alternator status.",
    },
    61445: {
        "acronym": "ETC2",
        "name": "Electronic Transmission Controller 2",
        "rate_ms": 100,
        "priority": 3,
        "source": "Transmission (0x03)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Selected gear, current gear, transmission gear ratio.",
    },
    65274: {
        "acronym": "AIR1",
        "name": "Air Supply Pressure",
        "rate_ms": 1000,
        "priority": 6,
        "source": "Brakes / APS (0x0B)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Pneumatic supply pressure, brake circuit 1 and circuit 2 pressure.",
    },
    65263: {
        "acronym": "EFL/P1",
        "name": "Engine Fluid Level/Pressure 1",
        "rate_ms": 500,
        "priority": 6,
        "source": "Engine #1 (0x00)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Engine oil level, engine oil filter differential pressure, engine oil pressure.",
    },
    65276: {
        "acronym": "DD",
        "name": "Dash Display",
        "rate_ms": 1000,
        "priority": 6,
        "source": "Instrument Cluster (0x17)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Fuel level 1, washer fluid level.",
    },
    65226: {
        "acronym": "DM1",
        "name": "Active Diagnostic Trouble Codes",
        "rate_ms": 1000,
        "priority": 6,
        "source": "Any ECU (0x00, 0x03, 0x0B, 0x21)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "SAE J1939 DM1 active diagnostic trouble codes and lamp states.",
    },
    # Proprietary Scania diagnostic PGN note:
    0xEF00: {
        "acronym": "PROP_SCANIA_PDU1",
        "name": "Scania Proprietary PDU1 (SDP3/KWP)",
        "rate_ms": 0,
        "priority": 6,
        "source": "Scania ECU (0x00, 0x21)",
        "fidelity": "OEM_SCANIA_SPECIFIC_REQUIRES_VERIFICATION",
        "description": "Scania SDP3 proprietary engineering diagnostics. Requires verified OEM decryption keys.",
    },
}


# SPN Parameter Catalog
SPN_CATALOG: Dict[int, Dict[str, Any]] = {
    190: {
        "name": "Engine Speed",
        "pgn": 61444,
        "resolution": 0.125,
        "offset": 0.0,
        "unit": "RPM",
        "range": "0 to 8031.875 RPM",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Actual rotational speed of the engine crankshaft.",
    },
    513: {
        "name": "Actual Engine - Percent Torque",
        "pgn": 61444,
        "resolution": 1.0,
        "offset": -125.0,
        "unit": "%",
        "range": "-125 to 125 %",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Calculated output torque of the engine indicated as a percent of reference engine torque.",
    },
    92: {
        "name": "Engine Percent Load At Current Speed",
        "pgn": 61443,
        "resolution": 1.0,
        "offset": 0.0,
        "unit": "%",
        "range": "0 to 250 %",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Ratio of actual engine percent torque to maximum indicated torque available at current engine speed.",
    },
    110: {
        "name": "Engine Coolant Temperature",
        "pgn": 65262,
        "resolution": 1.0,
        "offset": -40.0,
        "unit": "°C",
        "range": "-40 to 210 °C",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Temperature of liquid coolant circulating in the engine block.",
    },
    84: {
        "name": "Wheel-Based Vehicle Speed",
        "pgn": 65265,
        "resolution": 0.00390625,  # 1/256 km/h
        "offset": 0.0,
        "unit": "km/h",
        "range": "0 to 250.996 km/h",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Speed of the vehicle as calculated from wheel or axle speed sensors.",
    },
    168: {
        "name": "Battery Potential / System Voltage",
        "pgn": 65271,
        "resolution": 0.05,
        "offset": 0.0,
        "unit": "V",
        "range": "0 to 3212.75 V",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Electrical potential measured at the electrical system or battery terminals.",
    },
    523: {
        "name": "Transmission Current Gear",
        "pgn": 61445,
        "resolution": 1.0,
        "offset": -125.0,
        "unit": "gear",
        "range": "-125 to 125 (0 = Neutral)",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Current transmission gear engaged in the gearbox.",
    },
    100: {
        "name": "Engine Oil Pressure",
        "pgn": 65263,
        "resolution": 0.04,  # 4 kPa = 0.04 bar
        "offset": 0.0,
        "unit": "bar",
        "range": "0 to 10.0 bar",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Gage pressure of oil in the engine lubrication system.",
    },
    117: {
        "name": "Brake Application Pressure / Circuit 1",
        "pgn": 65274,
        "resolution": 0.08,  # 8 kPa = 0.08 bar
        "offset": 0.0,
        "unit": "bar",
        "range": "0 to 20.0 bar",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Air pressure delivered to the service brake pneumatic circuit 1.",
    },
    96: {
        "name": "Fuel Level 1",
        "pgn": 65276,
        "resolution": 0.4,
        "offset": 0.0,
        "unit": "%",
        "range": "0 to 100 %",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "description": "Ratio of the volume of fuel to the total volume of the primary fuel tank.",
    },
}


def get_pgn_name(pgn: int) -> str:
    if pgn in PGN_REGISTRY:
        return f"{PGN_REGISTRY[pgn]['acronym']} ({PGN_REGISTRY[pgn]['name']})"
    return f"PGN {pgn}"


def get_source_address_name(sa: int) -> str:
    return SOURCE_ADDRESS_MAP.get(sa, f"Unknown ECU (0x{sa:02X})")


def decode_eec1(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 61444 (EEC1)"""
    spns = []
    if len(payload) >= 4:
        # SPN 513: Actual Engine - Percent Torque (Byte 2, 1-indexed) -> payload[1]
        raw_torque = payload[1]
        if raw_torque <= 250:
            actual_torque = raw_torque - 125.0
            spns.append(DecodedSpn(
                spn=513,
                name="Actual Engine - Percent Torque",
                raw_value=raw_torque,
                scaled_value=round(actual_torque, 1),
                unit="%",
                description="Calculated output torque of the engine.",
            ))

        # SPN 190: Engine Speed (Bytes 3-4, Little Endian) -> payload[2:4]
        raw_rpm = payload[2] | (payload[3] << 8)
        if raw_rpm <= 64255:  # 0xFAFF max valid in J1939 (0xFB00+ are error/not available)
            scaled_rpm = raw_rpm * 0.125
            spns.append(DecodedSpn(
                spn=190,
                name="Engine Speed",
                raw_value=raw_rpm,
                scaled_value=round(scaled_rpm, 1),
                unit="RPM",
                description="Actual rotational speed of the engine crankshaft.",
            ))
        else:
            spns.append(DecodedSpn(
                spn=190,
                name="Engine Speed",
                raw_value=raw_rpm,
                scaled_value=None,
                unit="RPM",
                description="Value out of range or not available in frame.",
                is_valid=False,
            ))
    return spns


def decode_eec2(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 61443 (EEC2)"""
    spns = []
    if len(payload) >= 3:
        # SPN 92: Engine Percent Load At Current Speed (Byte 3, 1-indexed -> payload[2])
        raw_load = payload[2]
        if raw_load <= 250:
            spns.append(DecodedSpn(
                spn=92,
                name="Engine Percent Load At Current Speed",
                raw_value=raw_load,
                scaled_value=float(raw_load),
                unit="%",
                description="Ratio of actual engine torque to maximum available.",
            ))
    return spns


def decode_et1(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 65262 (ET1)"""
    spns = []
    if len(payload) >= 1:
        # SPN 110: Engine Coolant Temperature (Byte 1 -> payload[0])
        raw_temp = payload[0]
        if raw_temp <= 250:
            temp_c = raw_temp - 40.0
            spns.append(DecodedSpn(
                spn=110,
                name="Engine Coolant Temperature",
                raw_value=raw_temp,
                scaled_value=round(temp_c, 1),
                unit="°C",
                description="Temperature of liquid coolant circulating in the engine block.",
            ))
    return spns


def decode_ccvs1(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 65265 (CCVS1)"""
    spns = []
    if len(payload) >= 3:
        # SPN 84: Wheel-Based Vehicle Speed (Bytes 2-3, Little Endian -> payload[1:3])
        raw_speed = payload[1] | (payload[2] << 8)
        if raw_speed <= 64255:
            scaled_speed = raw_speed / 256.0
            spns.append(DecodedSpn(
                spn=84,
                name="Wheel-Based Vehicle Speed",
                raw_value=raw_speed,
                scaled_value=round(scaled_speed, 1),
                unit="km/h",
                description="Vehicle travel speed from wheel/axle sensors.",
            ))
    return spns


def decode_vep1(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 65271 (VEP1)"""
    spns = []
    if len(payload) >= 8:
        # SPN 168: Battery Potential / Power Input 1 (Bytes 7-8, Little Endian -> payload[6:8])
        raw_v = payload[6] | (payload[7] << 8)
        if raw_v <= 64255:
            voltage = raw_v * 0.05
            spns.append(DecodedSpn(
                spn=168,
                name="Battery Potential / System Voltage",
                raw_value=raw_v,
                scaled_value=round(voltage, 2),
                unit="V",
                description="Electrical potential measured at battery or main power bus.",
            ))
    return spns


def decode_etc2(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 61445 (ETC2)"""
    spns = []
    if len(payload) >= 4:
        # SPN 523: Current Gear (Byte 4 -> payload[3])
        raw_gear = payload[3]
        if raw_gear <= 250:
            gear = raw_gear - 125
            spns.append(DecodedSpn(
                spn=523,
                name="Transmission Current Gear",
                raw_value=raw_gear,
                scaled_value=gear,
                unit="gear",
                description="Current gear engaged (0 = Neutral, negative = Reverse, positive = Forward).",
            ))
    return spns


def decode_air1(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 65274 (AIR1)"""
    spns = []
    if len(payload) >= 2:
        # SPN 117: Brake Circuit 1 Pressure (Byte 2 -> payload[1])
        raw_p = payload[1]
        if raw_p <= 250:
            # 8 kPa per bit -> 8 / 100 bar = 0.08 bar
            p_bar = raw_p * 0.08
            spns.append(DecodedSpn(
                spn=117,
                name="Brake Circuit 1 Air Pressure",
                raw_value=raw_p,
                scaled_value=round(p_bar, 2),
                unit="bar",
                description="Pneumatic brake circuit 1 reservoir pressure.",
            ))
    return spns


def decode_eflp1(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 65263 (EFL/P1)"""
    spns = []
    if len(payload) >= 4:
        # SPN 100: Engine Oil Pressure (Bytes 3-4 -> payload[2:4] or Byte 4 depending on standard; standard SAE J1939 EFL/P1 uses Byte 4 as 4 kPa/bit)
        raw_p = payload[3]
        if raw_p <= 250:
            p_bar = (raw_p * 4.0) / 100.0  # 4 kPa / bit = 0.04 bar
            spns.append(DecodedSpn(
                spn=100,
                name="Engine Oil Pressure",
                raw_value=raw_p,
                scaled_value=round(p_bar, 2),
                unit="bar",
                description="Gage pressure of oil circulating through engine gallery.",
            ))
    return spns


def decode_dd(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 65276 (DD)"""
    spns = []
    if len(payload) >= 2:
        # SPN 96: Fuel Level (Byte 2 -> payload[1])
        raw_fuel = payload[1]
        if raw_fuel <= 250:
            fuel_pct = raw_fuel * 0.4
            spns.append(DecodedSpn(
                spn=96,
                name="Fuel Level 1",
                raw_value=raw_fuel,
                scaled_value=round(fuel_pct, 1),
                unit="%",
                description="Remaining fuel level percentage in primary fuel tank.",
            ))
    return spns


def decode_dm1(payload: bytes) -> List[DecodedSpn]:
    """Decodes PGN 65226 (DM1 Active DTCs)"""
    spns = []
    # Byte 1-2: Lamps
    # Byte 3-6: DTC 1 (SPN: 19 bits, FMI: 5 bits, CM: 1 bit, OC: 7 bits)
    if len(payload) >= 6:
        b3 = payload[2]
        b4 = payload[3]
        b5 = payload[4]
        b6 = payload[5]

        spn = b3 | (b4 << 8) | ((b5 & 0xE0) << 11)
        fmi = b5 & 0x1F
        cm = (b6 >> 7) & 0x01
        oc = b6 & 0x7F

        if spn > 0 and spn != 0x7FFFF:
            spns.append(DecodedSpn(
                spn=spn,
                name=f"Active DTC (SPN {spn} FMI {fmi})",
                raw_value=f"SPN={spn}, FMI={fmi}, OC={oc}",
                scaled_value=f"SPN {spn} FMI {fmi}",
                unit="DTC",
                description=f"DTC reported via DM1. Occurrence Count: {oc}. Conversion Method: {cm}.",
            ))
    return spns


# Encoder helpers for Simulator to generate accurate frames
def encode_eec1(rpm: float, actual_torque_pct: float = 25.0) -> bytes:
    # Byte 1: 0xF0 (Not specified)
    # Byte 2: Torque (-125 to +125, raw = val + 125)
    torque_raw = max(0, min(250, int(round(actual_torque_pct + 125))))
    # Bytes 3-4: RPM / 0.125
    rpm_raw = max(0, min(64255, int(round(rpm / 0.125))))
    b3 = rpm_raw & 0xFF
    b4 = (rpm_raw >> 8) & 0xFF
    return bytes([0xF0, torque_raw, b3, b4, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_eec2(load_pct: float) -> bytes:
    load_raw = max(0, min(250, int(round(load_pct))))
    return bytes([0xFF, 0xFF, load_raw, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_et1(coolant_c: float) -> bytes:
    # Coolant: raw = val + 40
    raw_temp = max(0, min(250, int(round(coolant_c + 40))))
    return bytes([raw_temp, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_ccvs1(speed_kmh: float) -> bytes:
    # Speed: raw = speed * 256
    raw_speed = max(0, min(64255, int(round(speed_kmh * 256.0))))
    b2 = raw_speed & 0xFF
    b3 = (raw_speed >> 8) & 0xFF
    return bytes([0x00, b2, b3, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_vep1(voltage: float) -> bytes:
    # Voltage: raw = voltage / 0.05
    raw_v = max(0, min(64255, int(round(voltage / 0.05))))
    b7 = raw_v & 0xFF
    b8 = (raw_v >> 8) & 0xFF
    return bytes([0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, b7, b8])


def encode_etc2(gear: int) -> bytes:
    # Gear: offset -125 -> raw = gear + 125
    raw_gear = max(0, min(250, int(gear + 125)))
    return bytes([raw_gear, 0xFF, 0xFF, raw_gear, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_air1(brake_circuit1_bar: float) -> bytes:
    # 0.08 bar per bit -> raw = bar / 0.08
    raw_p = max(0, min(250, int(round(brake_circuit1_bar / 0.08))))
    return bytes([0xFF, raw_p, raw_p, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_eflp1(oil_press_bar: float) -> bytes:
    # 0.04 bar per bit -> raw = bar / 0.04
    raw_p = max(0, min(250, int(round(oil_press_bar / 0.04))))
    return bytes([0xFF, 0xFF, 0xFF, raw_p, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_dd(fuel_pct: float) -> bytes:
    # 0.4% per bit -> raw = pct / 0.4
    raw_f = max(0, min(250, int(round(fuel_pct / 0.4))))
    return bytes([0xFF, raw_f, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF])


def encode_dm1(spn: int, fmi: int, oc: int = 1) -> bytes:
    # Lamps: MIL, Stop, Amber (0x00 = off, 0x05 = amber on)
    b1 = 0x05
    b2 = 0xFF
    # SPN 19 bits, FMI 5 bits
    b3 = spn & 0xFF
    b4 = (spn >> 8) & 0xFF
    b5 = ((spn >> 11) & 0xE0) | (fmi & 0x1F)
    b6 = (oc & 0x7F)  # CM=0
    return bytes([b1, b2, b3, b4, b5, b6, 0xFF, 0xFF])


def decode_j1939_frame(can_id: int, payload: bytes) -> J1939DecodedFrame:
    """
    Decodes an arbitrary CAN frame into J1939 structure and extracts all known SPNs.
    """
    from app.protocols.can_frame import CanFrame

    frame = CanFrame.from_raw(can_id=can_id, payload_bytes=payload, timestamp=0.0)

    decoded_spns: List[DecodedSpn] = []
    if frame.pgn == 61444:
        decoded_spns.extend(decode_eec1(payload))
    elif frame.pgn == 61443:
        decoded_spns.extend(decode_eec2(payload))
    elif frame.pgn == 65262:
        decoded_spns.extend(decode_et1(payload))
    elif frame.pgn == 65265:
        decoded_spns.extend(decode_ccvs1(payload))
    elif frame.pgn == 65271:
        decoded_spns.extend(decode_vep1(payload))
    elif frame.pgn == 61445:
        decoded_spns.extend(decode_etc2(payload))
    elif frame.pgn == 65274:
        decoded_spns.extend(decode_air1(payload))
    elif frame.pgn == 65263:
        decoded_spns.extend(decode_eflp1(payload))
    elif frame.pgn == 65276:
        decoded_spns.extend(decode_dd(payload))
    elif frame.pgn == 65226:
        decoded_spns.extend(decode_dm1(payload))

    fidelity = PGN_REGISTRY.get(frame.pgn, {}).get("fidelity", "VERIFIED_SAE_J1939_STANDARD")

    return J1939DecodedFrame(
        can_id_hex=frame.can_id_hex,
        can_id=frame.can_id,
        priority=frame.priority,
        pgn=frame.pgn,
        pgn_hex=frame.pgn_hex,
        pgn_name=frame.pgn_name,
        source_address=frame.source_address,
        source_name=frame.source_name,
        destination_address=frame.destination_address,
        dlc=frame.dlc,
        payload_hex=frame.payload_hex,
        spns=decoded_spns,
        fidelity=fidelity,
        simulated=True,
    )
