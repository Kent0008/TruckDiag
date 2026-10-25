package com.truckdiag.app.ui.screens.decoder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.model.CanDecodeResponseDto
import com.truckdiag.app.data.repository.NetworkResult
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DecoderUiState(
    val inputCanId: String = "0x0CF00400",
    val inputPayload: String = "F07D84204EFFFFF0",
    val isDecoding: Boolean = false,
    val result: CanDecodeResponseDto? = null,
    val errorMessage: String? = null
)

class DecoderViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecoderUiState())
    val uiState: StateFlow<DecoderUiState> = _uiState.asStateFlow()

    fun onCanIdChanged(id: String) {
        _uiState.value = _uiState.value.copy(inputCanId = id, errorMessage = null)
    }

    fun onPayloadChanged(payload: String) {
        _uiState.value = _uiState.value.copy(inputPayload = payload, errorMessage = null)
    }

    fun loadSample(canId: String, payload: String) {
        _uiState.value = _uiState.value.copy(
            inputCanId = canId,
            inputPayload = payload,
            errorMessage = null
        )
        decode()
    }

    fun decode() {
        val canId = _uiState.value.inputCanId.trim()
        val payload = _uiState.value.inputPayload.trim()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDecoding = true, errorMessage = null, result = null)
            when (val res = repository.decodeCanFrame(canId, payload)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(result = res.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isDecoding = false)
        }
    }
}
