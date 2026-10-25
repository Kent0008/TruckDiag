package com.truckdiag.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.truckdiag.app.R
import com.truckdiag.app.ui.components.*
import com.truckdiag.app.ui.navigation.Screen
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Connection.route) }) {
                        Icon(
                            imageVector = Icons.Default.SettingsEthernet,
                            contentDescription = stringResource(R.string.nav_connection),
                            tint = PrimaryAmber
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Simulated / Demo Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimulatedBadge()
                ConnectionStatusPill(status = if (state.isConnected) "CONNECTED" else "DISCONNECTED")
            }

            // Disclaimer Notice Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryAmberLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.simulated_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Active Diagnostic Session Card
            DiagnosticCard(
                title = stringResource(R.string.active_session_card_title),
                trailing = {
                    state.activeSession?.let {
                        SeverityBadge(severity = it.status)
                    }
                }
            ) {
                val session = state.activeSession
                if (session != null) {
                    Text(
                        text = "Сеанс: ${session.id.take(12)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${stringResource(R.string.session_operator_label, session.operatorName)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Автомобиль: ${session.truck?.brand ?: "Scania"} ${session.truck?.model ?: "R500"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate(Screen.LiveData.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.nav_livedata), color = BgDark, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { navController.navigate(Screen.Faults.route) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.nav_faults))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_active_session),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startDemoSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_start_demo),
                            color = BgDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Diagnostic Shortcuts Grid
            Text(
                text = "Диагностические функции",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DiagnosticFeatureCard(
                    title = stringResource(R.string.nav_canbus),
                    subtitle = "CAN 2.0B / J1939",
                    icon = Icons.Default.Memory,
                    accentColor = AccentCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate(Screen.CanBus.route)
                }

                DiagnosticFeatureCard(
                    title = stringResource(R.string.nav_decoder),
                    subtitle = "Разбор SPN/PGN",
                    icon = Icons.Default.Code,
                    accentColor = PrimaryAmberLight,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate(Screen.Decoder.route)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DiagnosticFeatureCard(
                    title = stringResource(R.string.nav_sessions),
                    subtitle = "История и статус",
                    icon = Icons.Default.History,
                    accentColor = StatusGreen,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate(Screen.Sessions.route)
                }

                DiagnosticFeatureCard(
                    title = stringResource(R.string.nav_reports),
                    subtitle = "Экспорт в JSON",
                    icon = Icons.Default.Description,
                    accentColor = PrimaryAmber,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate(Screen.Reports.route)
                }
            }

            // System Capabilities Card
            state.systemInfo?.let { info ->
                DiagnosticCard(title = "Платформа и протоколы") {
                    Text(
                        text = "Версия API: ${info.apiVersion} | ${info.protocolVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Поддерживаемые платформы: ${info.supportedTruckPlatforms.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
