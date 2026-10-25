package com.truckdiag.app.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.model.DiagnosticSession
import com.truckdiag.app.data.model.Truck
import com.truckdiag.app.data.repository.NetworkResult
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionsUiState(
    val isLoading: Boolean = false,
    val sessions: List<DiagnosticSession> = emptyList(),
    val availableTrucks: List<Truck> = emptyList(),
    val activeSessionId: String? = null,
    val isCreatingSession: Boolean = false,
    val errorMessage: String? = null
)

class SessionsViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeSession.collect { active ->
                _uiState.value = _uiState.value.copy(activeSessionId = active?.id)
            }
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val sRes = repository.getSessions()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(sessions = sRes.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = sRes.message)
                }
                else -> {}
            }

            when (val tRes = repository.getTrucks()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(availableTrucks = tRes.data)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun createSession(truckId: String, operatorName: String, notes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = repository.createSession(truckId, operatorName, notes)) {
                is NetworkResult.Success -> {
                    loadData()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun startSession(sessionId: String) {
        viewModelScope.launch {
            repository.startSession(sessionId)
            loadData()
        }
    }

    fun stopSession(sessionId: String) {
        viewModelScope.launch {
            repository.stopSession(sessionId)
            loadData()
        }
    }

    fun selectSession(session: DiagnosticSession) {
        repository.setActiveSession(session)
    }
}
