from typing import Optional
from pydantic import BaseModel, Field, field_validator


class CanFrame(BaseModel):
    """
    Representation of a CAN 2.0B / SAE J1939 extended 29-bit frame.
    """
    frame_id: Optional[str] = None
    can_id: int = Field(..., description="Numeric 29-bit extended CAN identifier")
    can_id_hex: str = Field(..., description="Hex representation of CAN ID, e.g. 0x0CF00400")
    priority: int = Field(..., ge=0, le=7, description="J1939 Priority (0-7, 3 bits)")
    data_page: int = Field(default=0, ge=0, le=1, description="Data Page (0 or 1)")
    pdu_format: int = Field(..., ge=0, le=255, description="PDU Format (PF 8 bits)")
    pdu_specific: int = Field(..., ge=0, le=255, description="PDU Specific (PS 8 bits, DA or GE)")
    pgn: int = Field(..., description="Calculated Parameter Group Number (PGN)")
    pgn_hex: str = Field(..., description="Hex representation of PGN, e.g. 0x00F004")
    pgn_name: str = Field(default="UNKNOWN", description="Acronym or name of PGN")
    source_address: int = Field(..., ge=0, le=255, description="Source Address (SA)")
    source_name: str = Field(default="Unknown ECU", description="Human-readable ECU name for SA")
    destination_address: Optional[int] = Field(default=255, description="Destination Address (DA, 255 = Broadcast)")
    dlc: int = Field(..., ge=0, le=8, description="Data Length Code (0-8 bytes)")
    payload_hex: str = Field(..., description="Hex-encoded byte payload, e.g. 'F07D84204EFFFFF0'")
    timestamp: float = Field(..., description="Unix timestamp of frame capture/generation")
    simulated: bool = Field(default=True, description="True if simulated, False if hardware adapter")

    @classmethod
    def from_raw(
        cls,
        can_id: int,
        payload_bytes: bytes,
        timestamp: float,
        simulated: bool = True,
        frame_id: Optional[str] = None,
    ) -> "CanFrame":
        """
        Parses a 29-bit CAN ID into J1939 fields.
        """
        # Validate 29-bit CAN ID
        if not (0 <= can_id <= 0x1FFFFFFF):
            raise ValueError(f"CAN ID 0x{can_id:X} exceeds 29-bit limit (0x0 to 0x1FFFFFFF)")

        # J1939 29-bit ID structure:
        # Priority: bits 28-26 (3 bits)
        # Reserved: bit 25 (1 bit)
        # Data Page: bit 24 (1 bit)
        # PDU Format: bits 23-16 (8 bits)
        # PDU Specific: bits 15-8 (8 bits)
        # Source Address: bits 7-0 (8 bits)
        priority = (can_id >> 26) & 0x07
        dp = (can_id >> 24) & 0x01
        pf = (can_id >> 16) & 0xFF
        ps = (can_id >> 8) & 0xFF
        sa = can_id & 0xFF

        # PGN calculation:
        # If PF < 240 (PDU1 format - destination specific):
        #   PGN = (DP << 16) | (PF << 8)
        #   Destination Address = PS
        # If PF >= 240 (PDU2 format - broadcast):
        #   PGN = (DP << 16) | (PF << 8) | PS
        #   Destination Address = 255 (Global Broadcast)
        if pf < 240:
            pgn = (dp << 16) | (pf << 8)
            da = ps
        else:
            pgn = (dp << 16) | (pf << 8) | ps
            da = 255

        from app.protocols.j1939 import get_pgn_name, get_source_address_name

        payload_hex = payload_bytes.hex().upper()
        dlc = len(payload_bytes)

        return cls(
            frame_id=frame_id,
            can_id=can_id,
            can_id_hex=f"0x{can_id:08X}",
            priority=priority,
            data_page=dp,
            pdu_format=pf,
            pdu_specific=ps,
            pgn=pgn,
            pgn_hex=f"0x{pgn:04X}" if pgn <= 0xFFFF else f"0x{pgn:06X}",
            pgn_name=get_pgn_name(pgn),
            source_address=sa,
            source_name=get_source_address_name(sa),
            destination_address=da,
            dlc=dlc,
            payload_hex=payload_hex,
            timestamp=timestamp,
            simulated=simulated,
        )
