import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_fault_scanning_and_injection(client: AsyncClient):
    create_res = await client.post(
        "/api/v1/sessions",
        json={"truck_id": "scania-r500-2021", "operator_name": "Test Tech"},
    )
    session_id = create_res.json()["id"]

    # 1. Trigger scan
    scan_res = await client.post(f"/api/v1/sessions/{session_id}/faults/scan")
    assert scan_res.status_code == 200
    scan_data = scan_res.json()
    assert scan_data["session_id"] == session_id
    assert scan_data["simulated"] is True
    assert scan_data["total_found"] >= 2
    assert len(scan_data["scanned_ecus"]) >= 4

    first_fault = scan_data["faults"][0]
    assert "spn" in first_fault
    assert "fmi" in first_fault
    assert first_fault["simulated"] is True

    # 2. Query session faults
    session_faults_res = await client.get(f"/api/v1/sessions/{session_id}/faults")
    assert session_faults_res.status_code == 200
    faults_list = session_faults_res.json()
    assert len(faults_list) == scan_data["total_found"]

    # 3. Inject predefined fault
    inject_res = await client.post(
        f"/api/v1/sessions/{session_id}/faults/inject",
        json={"fault_key": "COOLANT_OVERHEAT"},
    )
    assert inject_res.status_code == 200
    assert inject_res.json()["status"] == "injected"

    # Inject invalid fault key
    bad_inject = await client.post(
        f"/api/v1/sessions/{session_id}/faults/inject",
        json={"fault_key": "NONEXISTENT_FAULT_999"},
    )
    assert bad_inject.status_code == 400
