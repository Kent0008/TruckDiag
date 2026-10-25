package com.truckdiag.app.ui.screens.canbus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.model.CanFrameDto
import com.truckdiag.app.data.repository.NetworkResult
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CanBusUiState(
    val frames: List<CanFrameDto> = emptyList(),
    val isStreaming: Boolean = true,
    val selectedFilterPgn: Int? = null,
    val errorMessage: String? = null
)

class CanBusViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(CanBusUiState())
    val uiState: StateFlow<CanBusUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    fun toggleStreaming() {
        val newState = !_uiState.value.isStreaming
        _uiState.value = _uiState.value.copy(isStreaming = newState)
        if (newState) {
            startPolling()
        } else {
            pollJob?.cancel()
        }
    }

    fun clearFrames() {
        _uiState.value = _uiState.value.copy(frames = emptyList())
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isStreaming) {
                    val sId = repository.activeSession.value?.id
                    when (val res = repository.getCanFrames(sId, 50)) {
                        is NetworkResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                frames = res.data.reversed(),
                                errorMessage = null
                            )
                        }
                        is NetworkResult.Error -> {
                            _uiState.value = _uiState.value.copy(errorMessage = res.message)
                        }
                        else -> {}
                    }
                }
                delay(1200)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
