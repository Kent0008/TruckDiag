package com.truckdiag.app.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.truckdiag.app.R
import com.truckdiag.app.data.model.DiagnosticReport
import com.truckdiag.app.data.model.ReportDetail
import com.truckdiag.app.ui.components.DiagnosticCard
import com.truckdiag.app.ui.components.SimulatedBadge
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reports_title), color = TextPrimary) },
                actions = {
                    IconButton(onClick = { viewModel.loadReports() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryAmber,
                contentColor = BgDark
            ) {
                Icon(Icons.Default.PostAdd, contentDescription = stringResource(R.string.btn_create_report))
            }
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            state.successMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusGreen.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = msg,
                        color = StatusGreen,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            state.errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = msg,
                        color = StatusRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (state.isLoading && state.reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAmber)
                }
            } else if (state.reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.reports_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.reports, key = { it.id }) { report ->
                        ReportCardItem(
                            report = report,
                            onDetailsClick = { viewModel.viewReportDetails(report.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateReportDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, notes ->
                viewModel.createReportForActiveSession(title, notes)
                showCreateDialog = false
            }
        )
    }

    state.selectedReportDetail?.let { detail ->
        ReportDetailModal(
            detail = detail,
            onDismiss = { viewModel.dismissDetails() }
        )
    }
}

@Composable
fun ReportCardItem(
    report: DiagnosticReport,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .clickable { onDetailsClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                SimulatedBadge()
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${report.truckModel} | Идентификационный номер: ${report.truckVin}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.report_faults_count, report.totalFaults),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (report.activeFaults > 0) StatusAmber else StatusGreen,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Открыть отчёт →",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryAmber
                )
            }
        }
    }
}

@Composable
fun CreateReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("Отчёт диагностики Scania J1939") }
    var notes by remember { mutableStateOf("Плановый технический осмотр и считывание кодов неисправностей.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сформировать отчёт", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок отчёта") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки инженера") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                Text("Сохранить", color = BgDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun ReportDetailModal(
    detail: ReportDetail,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(detail.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Автомобиль: ${detail.truckModel}\nИдентификационный номер (VIN): ${detail.truckVin}\nСпециалист: ${detail.operatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )

                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Итоги сканирования:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryAmberLight
                )

                Text(
                    text = "Всего кодов неисправностей: ${detail.totalFaults} | Активных: ${detail.activeFaults}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                detail.reportData["disclaimer"]?.let { disc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = disc.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                Text("Закрыть", color = BgDark, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceDark
    )
}
