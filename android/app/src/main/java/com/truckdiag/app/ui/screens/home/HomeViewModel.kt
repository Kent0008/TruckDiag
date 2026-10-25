package com.truckdiag.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.model.DiagnosticSession
import com.truckdiag.app.data.model.HealthStatus
import com.truckdiag.app.data.model.SystemInfo
import com.truckdiag.app.data.model.Truck
import com.truckdiag.app.data.repository.NetworkResult
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val health: HealthStatus? = null,
    val systemInfo: SystemInfo? = null,
    val trucks: List<Truck> = emptyList(),
    val activeSession: DiagnosticSession? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeSession.collect { session ->
                _uiState.value = _uiState.value.copy(activeSession = session)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Health check
            when (val healthRes = repository.checkHealth()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isConnected = true,
                        health = healthRes.data
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isConnected = false,
                        errorMessage = healthRes.message
                    )
                }
                else -> {}
            }

            // System info
            when (val infoRes = repository.getSystemInfo()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(systemInfo = infoRes.data)
                }
                else -> {}
            }

            // Trucks list
            when (val trucksRes = repository.getTrucks()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(trucks = trucksRes.data)
                }
                else -> {}
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun startDemoSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val truckId = _uiState.value.trucks.firstOrNull()?.id ?: "scania-r500-2021"
            when (val createRes = repository.createSession(
                truckId = truckId,
                operatorName = "Демонстрационный режим",
                notes = "Автоматический запуск симуляции Scania R500 J1939"
            )) {
                is NetworkResult.Success -> {
                    repository.startSession(createRes.data.id)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = createRes.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
