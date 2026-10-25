package com.truckdiag.app.ui.screens.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.remote.ApiClient
import com.truckdiag.app.data.repository.NetworkResult
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectionUiState(
    val apiUrl: String = ApiClient.DEFAULT_BASE_URL,
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val statusMessage: String? = null,
    val apiVersion: String? = null
)

class ConnectionViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConnectionUiState(apiUrl = ApiClient.getBaseUrl())
    )
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _uiState.value = _uiState.value.copy(
            apiUrl = newUrl,
            testSuccess = null,
            statusMessage = null
        )
    }

    fun applyPreset(presetUrl: String) {
        onUrlChanged(presetUrl)
    }

    fun saveAndTest() {
        val url = _uiState.value.apiUrl.trim()
        ApiClient.setBaseUrl(url)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testSuccess = null, statusMessage = null)
            when (val res = repository.checkHealth()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        testSuccess = true,
                        statusMessage = "Связь с сервером TruckDiag установлена. Версия API: ${res.data.version}",
                        apiVersion = res.data.version
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        testSuccess = false,
                        statusMessage = res.message
                    )
                }
                else -> {}
            }
        }
    }
}
