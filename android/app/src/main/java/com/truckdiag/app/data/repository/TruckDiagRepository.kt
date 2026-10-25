package com.truckdiag.app.data.repository

import com.truckdiag.app.data.model.*
import com.truckdiag.app.data.remote.ApiClient
import com.truckdiag.app.data.remote.WebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

class TruckDiagRepository private constructor() {

    private val api get() = ApiClient.getService()
    val webSocketClient = WebSocketClient()

    private val _activeSession = MutableStateFlow<DiagnosticSession?>(null)
    val activeSession: StateFlow<DiagnosticSession?> = _activeSession.asStateFlow()

    private val _telemetryState = MutableStateFlow<TelemetryMessage?>(null)
    val telemetryState: StateFlow<TelemetryMessage?> = _telemetryState.asStateFlow()

    private val _isWsConnected = MutableStateFlow(false)
    val isWsConnected: StateFlow<Boolean> = _isWsConnected.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    init {
        scope.launch {
            webSocketClient.telemetryFlow.collect { msg ->
                _telemetryState.value = msg
            }
        }
        scope.launch {
            webSocketClient.connectionState.collect { connected ->
                _isWsConnected.value = connected
                if (!connected && _activeSession.value?.status == "RUNNING") {
                    startFallbackPolling()
                } else if (connected) {
                    stopFallbackPolling()
                }
            }
        }
    }

    suspend fun checkHealth(): NetworkResult<HealthStatus> {
        return try {
            val res = api.getHealth()
            NetworkResult.Success(res)
        } catch (e: Exception) {
            NetworkResult.Error("API недоступен: ${e.localizedMessage ?: e.message}", e)
        }
    }

    suspend fun getSystemInfo(): NetworkResult<SystemInfo> {
        return try {
            val res = api.getSystemInfo()
            NetworkResult.Success(res)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка получения системной информации: ${e.message}", e)
        }
    }

    suspend fun getTrucks(): NetworkResult<List<Truck>> {
        return try {
            val res = api.getTrucks()
            NetworkResult.Success(res)
        } catch (e: Exception) {
            NetworkResult.Error("Не удалось загрузить список грузовиков: ${e.message}", e)
        }
    }

    suspend fun createSession(truckId: String, operatorName: String, notes: String): NetworkResult<DiagnosticSession> {
        return try {
            val req = SessionCreateDto(truckId = truckId, operatorName = operatorName, notes = notes)
            val session = api.createSession(req)
            _activeSession.value = session
            NetworkResult.Success(session)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка создания сеанса: ${e.message}", e)
        }
    }

    suspend fun startSession(sessionId: String): NetworkResult<DiagnosticSession> {
        return try {
            val session = api.startSession(sessionId)
            _activeSession.value = session
            webSocketClient.connect(sessionId)
            NetworkResult.Success(session)
        } catch (e: Exception) {
            NetworkResult.Error("Не удалось запустить сеанс: ${e.message}", e)
        }
    }

    suspend fun stopSession(sessionId: String): NetworkResult<DiagnosticSession> {
        return try {
            val session = api.stopSession(sessionId)
            _activeSession.value = session
            webSocketClient.disconnect()
            stopFallbackPolling()
            NetworkResult.Success(session)
        } catch (e: Exception) {
            NetworkResult.Error("Не удалось остановить сеанс: ${e.message}", e)
        }
    }

    suspend fun getSessions(): NetworkResult<List<DiagnosticSession>> {
        return try {
            val list = api.getSessions(50)
            NetworkResult.Success(list)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка загрузки истории сеансов: ${e.message}", e)
        }
    }

    suspend fun getSessionStatus(sessionId: String): NetworkResult<SessionStatus> {
        return try {
            val status = api.getSessionStatus(sessionId)
            NetworkResult.Success(status)
        } catch (e: Exception) {
            NetworkResult.Error("Не удалось получить статус: ${e.message}", e)
        }
    }

    suspend fun scanFaults(sessionId: String): NetworkResult<FaultScanResult> {
        return try {
            val scan = api.scanFaults(sessionId)
            NetworkResult.Success(scan)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка сканирования кодов DTC: ${e.message}", e)
        }
    }

    suspend fun getSessionFaults(sessionId: String): NetworkResult<List<Fault>> {
        return try {
            val faults = api.getSessionFaults(sessionId)
            NetworkResult.Success(faults)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка загрузки DTC: ${e.message}", e)
        }
    }

    suspend fun getCanFrames(sessionId: String? = null, limit: Int = 50): NetworkResult<List<CanFrameDto>> {
        return try {
            val frames = api.getCanFrames(sessionId, limit)
            NetworkResult.Success(frames)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка получения CAN кадров: ${e.message}", e)
        }
    }

    suspend fun decodeCanFrame(canId: String, payload: String): NetworkResult<CanDecodeResponseDto> {
        return try {
            val req = CanDecodeRequestDto(canId = canId, payload = payload)
            val res = api.decodeCanFrame(req)
            NetworkResult.Success(res)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка декодирования: ${e.message}", e)
        }
    }

    suspend fun createReport(sessionId: String, title: String, notes: String): NetworkResult<DiagnosticReport> {
        return try {
            val req = ReportCreateDto(sessionId = sessionId, title = title, technicianNotes = notes)
            val res = api.createReport(req)
            NetworkResult.Success(res)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка формирования отчёта: ${e.message}", e)
        }
    }

    suspend fun getReports(): NetworkResult<List<DiagnosticReport>> {
        return try {
            val res = api.getReports(50)
            NetworkResult.Success(res)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка загрузки отчётов: ${e.message}", e)
        }
    }

    suspend fun getReportDetails(reportId: String): NetworkResult<ReportDetail> {
        return try {
            val res = api.getReportDetails(reportId)
            NetworkResult.Success(res)
        } catch (e: Exception) {
            NetworkResult.Error("Ошибка загрузки деталей отчёта: ${e.message}", e)
        }
    }

    fun setActiveSession(session: DiagnosticSession?) {
        _activeSession.value = session
        if (session != null && session.status == "RUNNING") {
            webSocketClient.connect(session.id)
        } else {
            webSocketClient.disconnect()
            stopFallbackPolling()
        }
    }

    private fun startFallbackPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val sId = _activeSession.value?.id
                    val snap = api.getLatestTelemetry(sId)
                    _telemetryState.value = snap
                } catch (_: Exception) {}
                delay(1000)
            }
        }
    }

    private fun stopFallbackPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    companion object {
        val instance: TruckDiagRepository by lazy { TruckDiagRepository() }
    }
}
