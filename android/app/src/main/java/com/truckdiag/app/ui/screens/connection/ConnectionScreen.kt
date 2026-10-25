package com.truckdiag.app.ui.screens.connection

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
import com.truckdiag.app.ui.components.DiagnosticCard
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    navController: NavController,
    viewModel: ConnectionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.connection_title), color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
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
            DiagnosticCard(title = stringResource(R.string.api_url_label)) {
                OutlinedTextField(
                    value = state.apiUrl,
                    onValueChange = { viewModel.onUrlChanged(it) },
                    label = { Text("URL") },
                    placeholder = { Text(stringResource(R.string.api_url_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAmber,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Быстрые шаблоны:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.applyPreset("http://10.0.2.2:8000/") },
                        label = { Text("10.0.2.2 (Эмулятор)") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceDark)
                    )

                    AssistChip(
                        onClick = { viewModel.applyPreset("http://192.168.1.100:8000/") },
                        label = { Text("Wi-Fi LAN") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceDark)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveAndTest() },
                    enabled = !state.isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BgDark,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = BgDark)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.btn_check_connection),
                        color = BgDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Result status banner
            state.statusMessage?.let { msg ->
                val isSuccess = state.testSuccess == true
                val cardColor = if (isSuccess) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f)
                val borderColor = if (isSuccess) StatusGreen else StatusRed

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = borderColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = borderColor
                        )
                    }
                }
            }

            // Connection tips card
            DiagnosticCard(title = "Инструкция по подключению") {
                Text(
                    text = "• Эмулятор Android Studio: адрес 'http://10.0.2.2:8000'\n" +
                           "• Физический смартфон: запустите backend на ПК, подключите телефон и ПК к одной Wi-Fi сети и укажите IP ПК, например: 'http://192.168.0.105:8000'\n" +
                           "• Порт по умолчанию: 8000",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
