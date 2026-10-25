import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_latest_telemetry(client: AsyncClient):
    # 1. Create a session first
    create_res = await client.post(
        "/api/v1/sessions",
        json={"truck_id": "scania-r500-2021", "operator_name": "Test Tech"},
    )
    assert create_res.status_code == 201
    session_id = create_res.json()["id"]

    # Start session
    await client.post(f"/api/v1/sessions/{session_id}/start")

    # Fetch latest telemetry
    response = await client.get(f"/api/v1/telemetry/latest?session_id={session_id}")
    assert response.status_code == 200
    data = response.json()
    assert data["session_id"] == session_id
    assert data["source"] == "SIMULATED_J1939"
    assert data["simulated"] is True

    # Validate parameters
    params = {p["name"]: p for p in data["parameters"]}
    assert "Engine Speed" in params
    assert "Vehicle Speed" in params
    assert "Engine Coolant Temperature" in params
    assert "Battery System Voltage" in params
    assert "Engine Oil Pressure" in params

    assert params["Engine Speed"]["unit"] == "RPM"
    assert params["Engine Coolant Temperature"]["unit"] == "°C"
    assert params["Battery System Voltage"]["unit"] == "V"


@pytest.mark.asyncio
async def test_telemetry_history(client: AsyncClient):
    create_res = await client.post(
        "/api/v1/sessions",
        json={"truck_id": "scania-r500-2021", "operator_name": "Test Tech"},
    )
    session_id = create_res.json()["id"]

    # Trigger latest to populate history in DB
    await client.get(f"/api/v1/telemetry/latest?session_id={session_id}")

    # Query history
    hist_res = await client.get(f"/api/v1/telemetry/history?session_id={session_id}&limit=50")
    assert hist_res.status_code == 200
    hist_data = hist_res.json()
    assert hist_data["session_id"] == session_id
    assert hist_data["count"] > 0
