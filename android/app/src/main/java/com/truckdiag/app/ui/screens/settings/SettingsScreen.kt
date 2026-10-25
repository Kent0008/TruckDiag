package com.truckdiag.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.truckdiag.app.R
import com.truckdiag.app.data.remote.ApiClient
import com.truckdiag.app.ui.components.DiagnosticCard
import com.truckdiag.app.ui.components.SimulatedBadge
import com.truckdiag.app.ui.navigation.Screen
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), color = TextPrimary) },
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
            DiagnosticCard(title = stringResource(R.string.pref_network_title)) {
                Text(
                    text = "Текущий адрес API: ${ApiClient.getBaseUrl()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { navController.navigate(Screen.Connection.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SettingsEthernet, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_open_connection))
                }
            }

            DiagnosticCard(title = stringResource(R.string.pref_about_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TruckDiag Android Client",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    SimulatedBadge()
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.app_version_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.protocol_support_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Архитектура: Kotlin, Jetpack Compose, Material 3, MVVM, Coroutines & Flow, Retrofit 2, Moshi, OkHttp WebSocket.\n\n" +
                           "Диагностический стек: SAE J1939 29-bit CAN 2.0B, PGN/SPN декодер, эмуляция ECU Scania DC13/DC16 Euro 6.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
