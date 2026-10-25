package com.truckdiag.app.ui.screens.decoder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.truckdiag.app.R
import com.truckdiag.app.ui.components.DiagnosticCard
import com.truckdiag.app.ui.components.SimulatedBadge
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoderScreen(
    navController: NavController,
    viewModel: DecoderViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.decoder_title), color = TextPrimary) },
                actions = {
                    SimulatedBadge(modifier = Modifier.padding(end = 12.dp))
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
            Text(
                text = stringResource(R.string.decoder_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            DiagnosticCard(title = "Входные параметры CAN кадра") {
                OutlinedTextField(
                    value = state.inputCanId,
                    onValueChange = { viewModel.onCanIdChanged(it) },
                    label = { Text("CAN ID (HEX)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAmber,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.inputPayload,
                    onValueChange = { viewModel.onPayloadChanged(it) },
                    label = { Text("Payload (HEX)") },
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
                    text = "Загрузить проверенный образец J1939:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.loadSample("0x0CF00400", "F07D84204EFFFFF0") },
                        label = { Text("EEC1 (Обороты)") }
                    )
                    AssistChip(
                        onClick = { viewModel.loadSample("0x18FEEE00", "7DFFFFFFFFFFFFFF") },
                        label = { Text("ET1 (ОЖ)") }
                    )
                    AssistChip(
                        onClick = { viewModel.loadSample("0x18FEF121", "000050FFFFFFFFFF") },
                        label = { Text("CCVS (Скорость)") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.decode() },
                    enabled = !state.isDecoding,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isDecoding) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BgDark, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Code, contentDescription = null, tint = BgDark)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.btn_decode),
                        color = BgDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error display
            state.errorMessage?.let { errorMsg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = StatusRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = errorMsg, color = StatusRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Decoded Output Card
            state.result?.let { res ->
                DiagnosticCard(title = stringResource(R.string.decoded_result_title)) {
                    Text(
                        text = "CAN ID: ${res.canIdHex} | Приоритет: ${res.priority} | DLC: ${res.dlc}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "PGN: ${res.pgn} (${res.pgnName}) [${res.pgnHex}]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryAmberLight,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Источник SA: 0x${res.sourceAddress.toString(16).uppercase()} (${res.sourceName})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Text(
                        text = "Уровень достоверности: ${res.fidelity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusGreen
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Декодированные параметры SPN (${res.decodedSpns.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    res.decodedSpns.forEach { spn ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${spn.name} (SPN ${spn.spn})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${spn.scaledValue ?: "N/A"} ${spn.unit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = spn.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
