package com.truckdiag.app.ui.screens.livedata

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import com.truckdiag.app.ui.components.*
import com.truckdiag.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDataScreen(
    navController: NavController,
    viewModel: LiveDataViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.live_data_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.last_updated, state.lastUpdatedFormatted),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
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
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Stream indicator and Engine State switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (state.isWsActive) StatusGreen else StatusAmber, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isWsActive) stringResource(R.string.live_stream_active) else stringResource(R.string.live_stream_polling),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isWsActive) StatusGreen else StatusAmber
                    )
                }

                state.telemetry?.let {
                    SeverityBadge(severity = it.engineState)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Engine state control bar (Simulator interaction)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Режим ДВС:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                FilterChip(
                    selected = state.telemetry?.engineState == "IDLE",
                    onClick = { viewModel.setEngineState("IDLE") },
                    label = { Text("Холостой ход") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardBorder,
                        selectedLabelColor = PrimaryAmberLight
                    )
                )

                FilterChip(
                    selected = state.telemetry?.engineState == "DRIVING",
                    onClick = { viewModel.setEngineState("DRIVING") },
                    label = { Text("Движение") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardBorder,
                        selectedLabelColor = AccentCyan
                    )
                )

                FilterChip(
                    selected = state.telemetry?.engineState == "STOPPING",
                    onClick = { viewModel.setEngineState("STOPPING") },
                    label = { Text("Торможение") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardBorder,
                        selectedLabelColor = StatusAmber
                    )
                )

                FilterChip(
                    selected = state.telemetry?.engineState == "ENGINE_OFF",
                    onClick = { viewModel.setEngineState("ENGINE_OFF") },
                    label = { Text("Заглушен") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CardBorder,
                        selectedLabelColor = StatusRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("ALL", stringResource(R.string.category_all)),
                    Pair("ENGINE", stringResource(R.string.category_engine)),
                    Pair("TRANSMISSION", stringResource(R.string.category_transmission)),
                    Pair("BRAKES", stringResource(R.string.category_brakes)),
                    Pair("ELECTRICAL", stringResource(R.string.category_electrical))
                ).forEach { (catKey, catTitle) ->
                    val isSelected = state.selectedCategory == catKey
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategory(catKey) },
                        label = { Text(catTitle) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = PrimaryAmber,
                            selectedLabelColor = BgDark,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Parameter Cards Grid
            if (state.filteredParameters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAmber)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.filteredParameters, key = { it.name }) { param ->
                        val (minVal, maxVal) = when (param.name) {
                            "Обороты двигателя" -> Pair(0f, 3000f)
                            "Скорость автомобиля" -> Pair(0f, 120f)
                            "Температура охлаждающей жидкости" -> Pair(0f, 130f)
                            "Процент нагрузки на двигатель" -> Pair(0f, 100f)
                            "Давление масла в двигателе" -> Pair(0f, 8f)
                            "Напряжение бортовой сети (АКБ)" -> Pair(20f, 32f)
                            "Текущая передача КПП" -> Pair(0f, 14f)
                            "Давление воздуха в 1-м тормозном контуре" -> Pair(0f, 12f)
                            "Уровень топлива" -> Pair(0f, 100f)
                            else -> Pair(0f, 100f)
                        }

                        MetricGaugeCard(
                            name = param.name,
                            value = param.value,
                            unit = param.unit,
                            quality = param.quality,
                            simulated = param.simulated,
                            minVal = minVal,
                            maxVal = maxVal
                        )
                    }
                }
            }
        }
    }
}
