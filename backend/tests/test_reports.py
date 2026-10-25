import json
import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_report_creation_and_export(client: AsyncClient):
    # 1. Create session
    create_res = await client.post(
        "/api/v1/sessions",
        json={"truck_id": "scania-r500-2021", "operator_name": "Senior Diagnostician"},
    )
    session_id = create_res.json()["id"]

    # 2. Trigger scan to attach faults
    await client.post(f"/api/v1/sessions/{session_id}/faults/scan")

    # 3. Create report
    report_req = {
        "session_id": session_id,
        "title": "Comprehensive Periodic Scania Inspection",
        "technician_notes": "Tested CAN bus communication and DM1 active DTC detection.",
    }
    report_res = await client.post("/api/v1/reports", json=report_req)
    assert report_res.status_code == 201
    report_data = report_res.json()
    report_id = report_data["id"]
    assert report_data["total_faults"] >= 2
    assert "Scania" in report_data["truck_model"]

    # 4. List reports
    list_res = await client.get("/api/v1/reports")
    assert list_res.status_code == 200
    reports = list_res.json()
    assert any(r["id"] == report_id for r in reports)

    # 5. Get report details
    detail_res = await client.get(f"/api/v1/reports/{report_id}")
    assert detail_res.status_code == 200
    detail_data = detail_res.json()
    assert "report_data" in detail_data
    assert "disclaimer" in detail_data["report_data"]

    # 6. Export report
    export_res = await client.get(f"/api/v1/reports/{report_id}/export")
    assert export_res.status_code == 200
    assert export_res.headers["content-type"].startswith("application/json")
    exported_json = json.loads(export_res.text)
    assert exported_json["session_id"] == session_id
    assert len(exported_json["faults"]) >= 2


@pytest.mark.asyncio
async def test_nonexistent_report_returns_404(client: AsyncClient):
    bad_id = "00000000-0000-0000-0000-000000000000"
    res1 = await client.get(f"/api/v1/reports/{bad_id}")
    assert res1.status_code == 404

    res2 = await client.get(f"/api/v1/reports/{bad_id}/export")
    assert res2.status_code == 404
