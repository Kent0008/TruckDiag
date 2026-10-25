import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_list_and_get_trucks(client: AsyncClient):
    response = await client.get("/api/v1/trucks")
    assert response.status_code == 200
    trucks = response.json()
    assert len(trucks) >= 3
    assert any(t["id"] == "scania-r500-2021" for t in trucks)

    # Get specific truck
    single_res = await client.get("/api/v1/trucks/scania-r500-2021")
    assert single_res.status_code == 200
    truck_data = single_res.json()
    assert truck_data["brand"] == "Scania"
    assert truck_data["simulated"] is True
    assert len(truck_data["ecu_list"]) > 0

    # Get nonexistent truck
    not_found_res = await client.get("/api/v1/trucks/nonexistent-truck-999")
    assert not_found_res.status_code == 404


@pytest.mark.asyncio
async def test_session_lifecycle(client: AsyncClient):
    # 1. Create Session
    create_payload = {
        "truck_id": "scania-r500-2021",
        "operator_name": "Senior Diagnostic Specialist",
        "notes": "Testing J1939 telemetry and fault scan",
        "connection_mode": "SIMULATOR",
    }
    create_res = await client.post("/api/v1/sessions", json=create_payload)
    assert create_res.status_code == 201
    session_data = create_res.json()
    session_id = session_data["id"]
    assert session_data["status"] == "CREATED"
    assert session_data["truck_id"] == "scania-r500-2021"

    # 2. Get Session Details
    get_res = await client.get(f"/api/v1/sessions/{session_id}")
    assert get_res.status_code == 200
    assert get_res.json()["id"] == session_id

    # 3. Check Initial Status
    status_res = await client.get(f"/api/v1/sessions/{session_id}/status")
    assert status_res.status_code == 200
    status_data = status_res.json()
    assert status_data["session_id"] == session_id
    assert status_data["simulated"] is True

    # 4. Start Session
    start_res = await client.post(f"/api/v1/sessions/{session_id}/start")
    assert start_res.status_code == 200
    assert start_res.json()["status"] == "RUNNING"

    # 5. Check Running Status
    status_running = await client.get(f"/api/v1/sessions/{session_id}/status")
    assert status_running.status_code == 200
    assert status_running.json()["connection_state"] == "CONNECTED"

    # 6. Stop Session
    stop_res = await client.post(f"/api/v1/sessions/{session_id}/stop")
    assert stop_res.status_code == 200
    assert stop_res.json()["status"] == "STOPPED"


@pytest.mark.asyncio
async def test_nonexistent_session_returns_404(client: AsyncClient):
    bad_id = "00000000-0000-0000-0000-000000000000"
    res1 = await client.get(f"/api/v1/sessions/{bad_id}")
    assert res1.status_code == 404

    res2 = await client.post(f"/api/v1/sessions/{bad_id}/start")
    assert res2.status_code == 404

    res3 = await client.post(f"/api/v1/sessions/{bad_id}/stop")
    assert res3.status_code == 404

    res4 = await client.get(f"/api/v1/sessions/{bad_id}/status")
    assert res4.status_code == 404
