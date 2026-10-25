package com.truckdiag.app.ui.screens.livedata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.model.TelemetryMessage
import com.truckdiag.app.data.model.TelemetryParameter
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class LiveDataUiState(
    val telemetry: TelemetryMessage? = null,
    val selectedCategory: String = "ALL",
    val isWsActive: Boolean = false,
    val lastUpdatedFormatted: String = "—",
    val filteredParameters: List<TelemetryParameter> = emptyList()
)

class LiveDataViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveDataUiState())
    val uiState: StateFlow<LiveDataUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    init {
        viewModelScope.launch {
            repository.isWsConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isWsActive = connected)
            }
        }

        viewModelScope.launch {
            repository.telemetryState.collect { msg ->
                if (msg != null) {
                    val formattedTime = timeFormat.format(Date((msg.timestamp * 1000).toLong()))
                    val filtered = filterParams(msg.parameters, _uiState.value.selectedCategory)
                    _uiState.value = _uiState.value.copy(
                        telemetry = msg,
                        lastUpdatedFormatted = formattedTime,
                        filteredParameters = filtered
                    )
                }
            }
        }
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        _uiState.value.telemetry?.let { msg ->
            _uiState.value = _uiState.value.copy(
                filteredParameters = filterParams(msg.parameters, category)
            )
        }
    }

    fun setEngineState(stateName: String) {
        repository.webSocketClient.sendCommand("set_state", "state", stateName)
    }

    private fun filterParams(params: List<TelemetryParameter>, category: String): List<TelemetryParameter> {
        return if (category == "ALL") {
            params
        } else {
            params.filter { it.category.equals(category, ignoreCase = true) }
        }
    }
}
