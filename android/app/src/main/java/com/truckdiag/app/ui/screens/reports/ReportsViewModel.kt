package com.truckdiag.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckdiag.app.data.model.DiagnosticReport
import com.truckdiag.app.data.model.ReportDetail
import com.truckdiag.app.data.repository.NetworkResult
import com.truckdiag.app.data.repository.TruckDiagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReportsUiState(
    val isLoading: Boolean = false,
    val reports: List<DiagnosticReport> = emptyList(),
    val selectedReportDetail: ReportDetail? = null,
    val isCreating: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class ReportsViewModel(
    private val repository: TruckDiagRepository = TruckDiagRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.getReports()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(reports = res.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun createReportForActiveSession(title: String, notes: String) {
        val sessionId = repository.activeSession.value?.id
        if (sessionId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Нет активного сеанса для отчёта")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = null)
            when (val res = repository.createReport(sessionId, title, notes)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Отчёт '${res.data.title}' успешно сформирован!"
                    )
                    loadReports()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isCreating = false)
        }
    }

    fun viewReportDetails(reportId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val res = repository.getReportDetails(reportId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(selectedReportDetail = res.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                else -> {}
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun dismissDetails() {
        _uiState.value = _uiState.value.copy(selectedReportDetail = null)
    }
}
