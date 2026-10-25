# Спецификация REST & WebSocket API TruckDiag

Сервер предоставляет полнофункциональный REST и WebSocket интерфейс для диагностики грузовых автомобилей (порт по умолчанию: `8000`).

Интерактивная документация:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`
- OpenAPI JSON: `http://localhost:8000/openapi.json`

---

## 1. Системные endpoints

### `GET /api/v1/health`
Проверка состояния сервера, подключения базы данных и готовности симулятора.
- **Ответ 200 OK**:
  ```json
  {
    "status": "ok",
    "timestamp": 1792818413.86,
    "database": "connected",
    "simulator": "ready",
    "version": "1.0.0"
  }
  ```

### `GET /api/v1/system/info`
Сведения о поддерживаемых протоколах, диагностических адаптерах и платформах тягачей.
- **Ответ 200 OK**:
  ```json
  {
    "project_name": "TruckDiag API",
    "api_version": "1.0.0",
    "protocol_version": "SAE J1939-2023",
    "supported_protocols": ["SAE J1939", "CAN 2.0B (29-bit)", "Scania Diagnostics (Read-Only Simulation)"],
    "available_adapters": ["SIMULATOR_VIRTUAL_BUS (Active)", "SOCKETCAN_INTERFACE (Prepared)", "RP1210_PASSTHRU (Prepared)"],
    "supported_truck_platforms": ["Scania R/S-Series (2019-2023 Euro 6)", "Volvo FH Series (2014+ J1939 Architecture)"],
    "environment": "local_development"
  }
  ```

---

## 2. Грузовики и сеансы диагностики

### `GET /api/v1/trucks`
Список доступных виртуальных грузовых автомобилей с описанием установленных ECU.

### `GET /api/v1/trucks/{truck_id}`
Подробная информация о конкретном грузовике (VIN, код двигателя, тип КПП, блоки управления).

### `POST /api/v1/sessions`
Создание нового диагностического сеанса.
- **Тело запроса**:
  ```json
  {
    "truck_id": "scania-r500-2021",
    "operator_name": "Диагност Иванов",
    "notes": "Плановая проверка CAN шины и чтение кодов ошибок",
    "connection_mode": "SIMULATOR"
  }
  ```
- **Ответ 201 Created**: JSON с уникальным `id` сеанса и статусом `CREATED`.

### `GET /api/v1/sessions`
Список сохранённых сеансов диагностики с сортировкой по времени создания.

### `GET /api/v1/sessions/{session_id}`
Детальная информация о сеансе диагностики.

### `POST /api/v1/sessions/{session_id}/start`
Запуск диагностического сеанса и активация симулятора грузовика. Статус переходит в `RUNNING`.

### `POST /api/v1/sessions/{session_id}/stop`
Остановка сеанса и симулятора. Статус переходит в `STOPPED`.

### `GET /api/v1/sessions/{session_id}/status`
Текущий статус подключения (`CONNECTED`, `CONNECTING`, `DISCONNECTED`), частота кадров в секунду, количество активных виртуальных ECU и время непрерывной работы.

---

## 3. Телеметрия

### `GET /api/v1/telemetry/latest?session_id={session_id}`
Получение последнего среза параметров в реальном времени.
- **Ответ 200 OK**:
  ```json
  {
    "timestamp": 1792818500.12,
    "session_id": "32cbab57-...",
    "source": "SIMULATED_J1939",
    "engine_state": "IDLE",
    "simulated": true,
    "parameters": [
      {"name": "Engine Speed", "value": 602.4, "unit": "RPM", "quality": "GOOD", "category": "ENGINE", "simulated": true},
      {"name": "Vehicle Speed", "value": 0.0, "unit": "km/h", "quality": "GOOD", "category": "ENGINE", "simulated": true},
      {"name": "Engine Coolant Temperature", "value": 84.8, "unit": "°C", "quality": "GOOD", "category": "ENGINE", "simulated": true},
      {"name": "Engine Percent Load", "value": 17.5, "unit": "%", "quality": "GOOD", "category": "ENGINE", "simulated": true},
      {"name": "Engine Oil Pressure", "value": 2.65, "unit": "bar", "quality": "GOOD", "category": "ENGINE", "simulated": true},
      {"name": "Battery System Voltage", "value": 28.32, "unit": "V", "quality": "GOOD", "category": "ELECTRICAL", "simulated": true},
      {"name": "Transmission Current Gear", "value": 0.0, "unit": "gear", "quality": "GOOD", "category": "TRANSMISSION", "simulated": true},
      {"name": "Brake Circuit 1 Air Pressure", "value": 10.35, "unit": "bar", "quality": "GOOD", "category": "BRAKES", "simulated": true},
      {"name": "Fuel Level", "value": 76.2, "unit": "%", "quality": "GOOD", "category": "ELECTRICAL", "simulated": true}
    ]
  }
  ```

### `GET /api/v1/telemetry/history?session_id={session_id}&limit=100`
Исторический ряд параметров из базы данных.

### `WS /api/v1/telemetry/ws/{session_id}`
Полнодуплексный WebSocket канал телеметрии.
- **Входящие сообщения клиенту**: регулярные пакеты `TelemetryMessage` каждую секунду.
- **Исходящие команды от клиента**:
  - Смена режима ДВС: `{"action": "set_state", "state": "DRIVING"}` (доступны: `ENGINE_OFF`, `IDLE`, `DRIVING`, `STOPPING`).
  - Включение неисправности: `{"action": "inject_fault", "fault_key": "COOLANT_OVERHEAT"}`.
  - Сброс неисправности: `{"action": "clear_fault", "fault_key": "COOLANT_OVERHEAT"}`.

---

## 4. Диагностические коды неисправностей (DTC)

### `GET /api/v1/faults` & `GET /api/v1/sessions/{session_id}/faults`
Список зарегистрированных неисправностей.

### `POST /api/v1/sessions/{session_id}/faults/scan`
Запуск симулированного диагностического опроса виртуальных блоков (EMS, COO8, GMS, EBS).
- Возвращает обнаруженные коды, уровень серьёзности (`CRITICAL`, `WARNING`, `INFO`), возможные причины и отметку `simulated=true`.

### `POST /api/v1/sessions/{session_id}/faults/inject`
Принудительное включение неисправности для тестирования системы оповещения.

---

## 5. CAN / J1939 протокол

### `GET /api/v1/can/frames?session_id={session_id}&limit=50`
Список последних "сырых" 29-битных кадров шины CAN.

### `POST /api/v1/can/decode`
Интерактивный декодер CAN кадра.
- **Тело запроса**:
  ```json
  {
    "can_id": "0x0CF00400",
    "payload": "F07D84204EFFFFF0"
  }
  ```
- **Ответ 200 OK**:
  ```json
  {
    "can_id": 217056256,
    "can_id_hex": "0x0CF00400",
    "priority": 3,
    "pgn": 61444,
    "pgn_hex": "0x00F004",
    "pgn_name": "EEC1 (Electronic Engine Controller 1)",
    "source_address": 0,
    "source_name": "Engine #1 (EMS)",
    "destination_address": 255,
    "dlc": 8,
    "payload_hex": "F07D84204EFFFFF0",
    "fidelity": "VERIFIED_SAE_J1939_STANDARD",
    "decoded_spns": [
      {
        "spn": 513,
        "name": "Actual Engine - Percent Torque",
        "raw_value": 125,
        "scaled_value": 0.0,
        "unit": "%",
        "description": "Calculated output torque of the engine.",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "is_valid": true
      },
      {
        "spn": 190,
        "name": "Engine Speed",
        "raw_value": 8324,
        "scaled_value": 1040.5,
        "unit": "RPM",
        "description": "Actual rotational speed of the engine crankshaft.",
        "fidelity": "VERIFIED_SAE_J1939_STANDARD",
        "is_valid": true
      }
    ],
    "simulated": true
  }
  ```
- **Обработка ошибок**: возвращает понятные HTTP 400 при выходе за пределы 29 бит, нечётной длине HEX или недопустимых символах.

### `GET /api/v1/j1939/parameters`
Справочник реализованных SPN параметров с формулами масштабирования.

### `GET /api/v1/j1939/pgns`
Справочник PGN групп и уровней достоверности (SAE Standard vs Scania OEM).

---

## 6. Отчёты

### `POST /api/v1/reports`
Формирование сводного отчёта по диагностическому сеансу.

### `GET /api/v1/reports`
Список сохранённых отчётов.

### `GET /api/v1/reports/{report_id}`
Полные сведения по отчёту, включая состояние блоков, параметры телеметрии и DTC коды.

### `GET /api/v1/reports/{report_id}/export`
Экспорт полного отчёта в формате JSON файла с заголовком `Content-Disposition`.
