import asyncio
import pytest
from app.simulator.truck_simulator import EngineState, TruckSimulator


@pytest.mark.asyncio
async def test_truck_simulator_state_transitions():
    sim = TruckSimulator(session_id="test-sim", truck_id="scania-r500-2021")

    # Initial state
    assert sim.state == EngineState.IDLE
    snap = sim.get_telemetry_snapshot()
    assert snap.simulated is True
    assert snap.source == "SIMULATED_J1939"

    # Transition to DRIVING
    sim.set_engine_state(EngineState.DRIVING)
    assert sim.state == EngineState.DRIVING
    # Step simulation physics
    for _ in range(5):
        sim._update_physics()

    assert sim.speed > 0.0
    assert sim.gear >= 1

    # Transition to STOPPING
    sim.set_engine_state(EngineState.STOPPING)
    for _ in range(10):
        sim._update_physics()

    # Transition to ENGINE_OFF
    sim.set_engine_state(EngineState.ENGINE_OFF)
    for _ in range(5):
        sim._update_physics()

    assert sim.rpm < 500.0


@pytest.mark.asyncio
async def test_truck_simulator_fault_injection():
    sim = TruckSimulator(session_id="test-sim-faults", truck_id="scania-r500-2021")

    # Inject COOLANT_OVERHEAT
    injected = sim.inject_fault("COOLANT_OVERHEAT")
    assert injected is True
    assert "COOLANT_OVERHEAT" in sim.active_faults

    # Simulate physics with active fault
    for _ in range(5):
        sim._update_physics()

    assert sim.coolant_temp > 85.0

    # CAN frames must include DM1
    sim._generate_can_frames()
    recent_frames = sim.get_recent_can_frames()
    dm1_frames = [f for f in recent_frames if f.pgn == 65226]
    assert len(dm1_frames) > 0

    # Clear fault
    cleared = sim.clear_fault("COOLANT_OVERHEAT")
    assert cleared is True
    assert "COOLANT_OVERHEAT" not in sim.active_faults
