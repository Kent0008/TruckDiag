package com.truckdiag.app.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.truckdiag.app.R
import com.truckdiag.app.data.model.DiagnosticSession
import com.truckdiag.app.ui.components.SeverityBadge
import com.truckdiag.app.ui.components.SimulatedBadge
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    navController: NavController,
    viewModel: SessionsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sessions_title), color = TextPrimary) },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_new_session))
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
            if (state.isLoading && state.sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryAmber)
                }
            } else if (state.sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Сеансов пока нет. Нажмите '+' для создания.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.sessions, key = { it.id }) { session ->
                        val isSelected = session.id == state.activeSessionId
                        SessionCard(
                            session = session,
                            isSelected = isSelected,
                            onSelect = { viewModel.selectSession(session) },
                            onStart = { viewModel.startSession(session.id) },
                            onStop = { viewModel.stopSession(session.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSessionDialog(
            trucks = state.availableTrucks,
            onDismiss = { showCreateDialog = false },
            onConfirm = { truckId, operator, notes ->
                viewModel.createSession(truckId, operator, notes)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun SessionCard(
    session: DiagnosticSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryAmber else CardBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onSelect() },
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
                    text = "Сеанс: ${session.id.take(8)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PrimaryAmber else TextPrimary
                )
                SeverityBadge(severity = session.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Автомобиль: ${session.truck?.brand ?: "Scania"} ${session.truck?.model ?: "R500"}",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentCyan
            )

            Text(
                text = "Специалист: ${session.operatorName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (session.notes.isNotEmpty()) {
                Text(
                    text = "Заметка: ${session.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.status == "RUNNING") {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Остановить", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Запустить", fontSize = 12.sp, color = BgDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateSessionDialog(
    trucks: List<com.truckdiag.app.data.model.Truck>,
    onDismiss: () -> Unit,
    onConfirm: (truckId: String, operator: String, notes: String) -> Unit
) {
    var selectedTruckId by remember { mutableStateOf(trucks.firstOrNull()?.id ?: "scania-r500-2021") }
    var operator by remember { mutableStateOf("Инженер-диагност") }
    var notes by remember { mutableStateOf("Комплексная диагностика CAN/J1939") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый диагностический сеанс", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Выберите грузовик:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                trucks.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTruckId = t.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTruckId == t.id,
                            onClick = { selectedTruckId = t.id }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${t.brand} ${t.model}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }

                OutlinedTextField(
                    value = operator,
                    onValueChange = { operator = it },
                    label = { Text("Специалист") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки к сеансу") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTruckId, operator, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber)
            ) {
                Text("Создать", color = BgDark, fontWeight = FontWeight.Bold)
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
