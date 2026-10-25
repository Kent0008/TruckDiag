package com.truckdiag.app.ui.screens.faults

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.model.Fault
import com.truckdiag.app.data.repository.NetworkResult
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FaultsUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val faults: List<Fault> = emptyList(),
    val scannedEcus: List<String> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class FaultsViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaultsUiState())
    val uiState: StateFlow<FaultsUiState> = _uiState.asStateFlow()

    init {
        loadFaults()
    }

    fun loadFaults() {
        val sessionId = repository.activeSession.value?.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = if (sessionId != null) {
                repository.getSessionFaults(sessionId)
            } else {
                when (val all = repository.getSessions()) {
                    is NetworkResult.Success -> {
                        val firstSession = all.data.firstOrNull()
                        if (firstSession != null) {
                            repository.getSessionFaults(firstSession.id)
                        } else {
                            NetworkResult.Success(emptyList())
                        }
                    }
                    else -> NetworkResult.Success(emptyList())
                }
            }

            when (res) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(faults = res.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun scanEcus() {
        val sessionId = repository.activeSession.value?.id ?: "demo-session"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null, infoMessage = null)
            when (val scanRes = repository.scanFaults(sessionId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        faults = scanRes.data.faults,
                        scannedEcus = scanRes.data.scannedEcus,
                        infoMessage = "Сканирование завершено. Опрошено ${scanRes.data.scannedEcus.size} блоков ECU."
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = scanRes.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    fun injectTestFault(faultKey: String) {
        repository.webSocketClient.sendCommand("inject_fault", "fault_key", faultKey)
        scanEcus()
    }
}
