import asyncio
from typing import Dict, Optional
from app.simulator.truck_simulator import TruckSimulator


class SimulatorManager:
    _instance: Optional["SimulatorManager"] = None

    def __init__(self):
        self.simulators: Dict[str, TruckSimulator] = {}

    @classmethod
    def get_instance(cls) -> "SimulatorManager":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def get_or_create(self, session_id: str, truck_id: str = "scania-r500-2021") -> TruckSimulator:
        if session_id not in self.simulators:
            sim = TruckSimulator(session_id=session_id, truck_id=truck_id)
            self.simulators[session_id] = sim
        return self.simulators[session_id]

    def get(self, session_id: str) -> Optional[TruckSimulator]:
        return self.simulators.get(session_id)

    async def remove(self, session_id: str) -> None:
        if session_id in self.simulators:
            sim = self.simulators.pop(session_id)
            await sim.stop()


simulator_manager = SimulatorManager.get_instance()
