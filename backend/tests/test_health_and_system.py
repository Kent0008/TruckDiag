import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_health_check(client: AsyncClient):
    response = await client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["database"] == "connected"
    assert data["simulator"] == "ready"
    assert "version" in data


@pytest.mark.asyncio
async def test_system_info(client: AsyncClient):
    response = await client.get("/api/v1/system/info")
    assert response.status_code == 200
    data = response.json()
    assert data["protocol_version"] == "SAE J1939-2023"
    assert "SAE J1939" in data["supported_protocols"]
    assert any("Scania" in platform for platform in data["supported_truck_platforms"])
