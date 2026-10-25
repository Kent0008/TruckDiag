import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_can_frames_retrieval(client: AsyncClient):
    response = await client.get("/api/v1/can/frames?limit=10")
    assert response.status_code == 200
    frames = response.json()
    assert isinstance(frames, list)
    if frames:
        f = frames[0]
        assert "can_id" in f
        assert "pgn" in f
        assert "payload_hex" in f
        assert f["simulated"] is True


@pytest.mark.asyncio
async def test_j1939_decoding_eec1_engine_speed(client: AsyncClient):
    # CAN ID: 0x0CF00400 (Priority 3, PGN 61444 EEC1, SA 0x00 Engine)
    # Payload: 8 bytes
    # Byte 1: F0
    # Byte 2: 7D (Torque: 125 - 125 = 0.0%)
    # Byte 3-4: 0x2000 (8192 little-endian: 00 20 -> 8192 * 0.125 = 1024.0 RPM)
    payload_hex = "F07D0020FFFFFFFF"
    decode_req = {
        "can_id": "0x0CF00400",
        "payload": payload_hex,
    }
    response = await client.post("/api/v1/can/decode", json=decode_req)
    assert response.status_code == 200
    data = response.json()

    assert data["pgn"] == 61444
    assert data["priority"] == 3
    assert data["source_address"] == 0
    assert data["dlc"] == 8

    spns = {s["spn"]: s for s in data["decoded_spns"]}
    assert 190 in spns
    assert spns[190]["scaled_value"] == 1024.0
    assert spns[190]["unit"] == "RPM"
    assert spns[190]["fidelity"] == "VERIFIED_SAE_J1939_STANDARD"


@pytest.mark.asyncio
async def test_j1939_decoding_coolant_temp_et1(client: AsyncClient):
    # PGN 65262 (ET1) - CAN ID: 0x18FEEE00 (0xFEEE = 65262)
    # Byte 1: 0x7D (125 - 40 = 85 °C)
    payload_hex = "7DFFFFFFFFFFFFFF"
    decode_req = {
        "can_id": 0x18FEEE00,
        "payload": payload_hex,
    }
    response = await client.post("/api/v1/can/decode", json=decode_req)
    assert response.status_code == 200
    data = response.json()
    assert data["pgn"] == 65262
    spns = {s["spn"]: s for s in data["decoded_spns"]}
    assert 110 in spns
    assert spns[110]["scaled_value"] == 85.0
    assert spns[110]["unit"] == "°C"


@pytest.mark.asyncio
async def test_j1939_decoding_vehicle_speed_ccvs(client: AsyncClient):
    # PGN 65265 (CCVS1) - CAN ID: 0x18FEF121
    # Bytes 2-3: 0x5000 (20480 / 256 = 80.0 km/h) -> little endian: 00 50
    payload_hex = "000050FFFFFFFFFF"
    decode_req = {
        "can_id": "0x18FEF121",
        "payload": payload_hex,
    }
    response = await client.post("/api/v1/can/decode", json=decode_req)
    assert response.status_code == 200
    data = response.json()
    assert data["pgn"] == 65265
    spns = {s["spn"]: s for s in data["decoded_spns"]}
    assert 84 in spns
    assert spns[84]["scaled_value"] == 80.0
    assert spns[84]["unit"] == "km/h"


@pytest.mark.asyncio
async def test_can_decode_validation_errors(client: AsyncClient):
    # 1. Non-hex characters
    res1 = await client.post(
        "/api/v1/can/decode",
        json={"can_id": "0x0CF00400", "payload": "GGHHZZ11"},
    )
    assert res1.status_code == 400

    # 2. Odd length
    res2 = await client.post(
        "/api/v1/can/decode",
        json={"can_id": "0x0CF00400", "payload": "123"},
    )
    assert res2.status_code == 400

    # 3. Exceeds 8 bytes DLC
    res3 = await client.post(
        "/api/v1/can/decode",
        json={"can_id": "0x0CF00400", "payload": "010203040506070809"},
    )
    assert res3.status_code == 400

    # 4. Out of 29-bit CAN ID bounds (> 0x1FFFFFFF)
    res4 = await client.post(
        "/api/v1/can/decode",
        json={"can_id": "0x2FFFFFFF", "payload": "0102030405060708"},
    )
    assert res4.status_code == 400


@pytest.mark.asyncio
async def test_j1939_parameters_and_pgns_catalog(client: AsyncClient):
    res_params = await client.get("/api/v1/j1939/parameters")
    assert res_params.status_code == 200
    params = res_params.json()
    assert len(params) >= 5
    spn_ids = [p["spn"] for p in params]
    assert 190 in spn_ids  # Engine speed
    assert 110 in spn_ids  # Coolant temp
    assert 84 in spn_ids   # Vehicle speed

    res_pgns = await client.get("/api/v1/j1939/pgns")
    assert res_pgns.status_code == 200
    pgns = res_pgns.json()
    assert len(pgns) >= 5
    pgn_numbers = [p["pgn"] for p in pgns]
    assert 61444 in pgn_numbers  # EEC1
    assert 65262 in pgn_numbers  # ET1
