package com.truckdiag.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckdiag.app.R
import com.truckdiag.app.ui.theme.*

@Composable
fun SimulatedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0x33FF9800), RoundedCornerShape(4.dp))
            .border(1.dp, PrimaryAmber, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = stringResource(R.string.status_simulated),
            color = PrimaryAmberLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ConnectionStatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, textRes) = when (status) {
        "CONNECTED" -> Pair(StatusGreen, R.string.status_connected)
        "CONNECTING" -> Pair(StatusAmber, R.string.status_connecting)
        else -> Pair(StatusRed, R.string.status_disconnected)
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(textRes),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SeverityBadge(severity: String) {
    val (bg, textColor) = when (severity.uppercase()) {
        "CRITICAL" -> Pair(StatusRed.copy(alpha = 0.2f), StatusRed)
        "WARNING" -> Pair(StatusAmber.copy(alpha = 0.2f), StatusAmber)
        else -> Pair(AccentCyan.copy(alpha = 0.2f), AccentCyan)
    }
    val displayText = when (severity.uppercase()) {
        "CRITICAL" -> stringResource(R.string.severity_critical)
        "WARNING" -> stringResource(R.string.severity_warning)
        "INFO" -> stringResource(R.string.severity_info)
        "ACTIVE" -> stringResource(R.string.fault_active)
        "PREVIOUSLY_ACTIVE" -> stringResource(R.string.fault_previously_active)
        "PENDING" -> stringResource(R.string.fault_pending)
        "CREATED" -> stringResource(R.string.session_created)
        "RUNNING" -> stringResource(R.string.session_running)
        "STOPPED" -> stringResource(R.string.session_stopped)
        "ENGINE_OFF" -> stringResource(R.string.engine_off)
        "IDLE" -> stringResource(R.string.engine_idle)
        "DRIVING" -> stringResource(R.string.engine_driving)
        "STOPPING" -> stringResource(R.string.engine_stopping)
        else -> severity.uppercase()
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.dp, textColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LocalizedDtcStatus(status: String): String = when (status.uppercase()) {
    "ACTIVE" -> stringResource(R.string.fault_active)
    "PREVIOUSLY_ACTIVE" -> stringResource(R.string.fault_previously_active)
    "PENDING" -> stringResource(R.string.fault_pending)
    else -> status
}

@Composable
fun LocalizedPgnName(pgn: Int, fallback: String): String = when (pgn) {
    61444 -> stringResource(R.string.pgn_eec1)
    61443 -> stringResource(R.string.pgn_eec2)
    65262 -> stringResource(R.string.pgn_et1)
    65265 -> stringResource(R.string.pgn_ccvs1)
    65271 -> stringResource(R.string.pgn_vep1)
    61445 -> stringResource(R.string.pgn_etc2)
    65274 -> stringResource(R.string.pgn_air1)
    65263 -> stringResource(R.string.pgn_eflp1)
    65276 -> stringResource(R.string.pgn_dd)
    65226 -> stringResource(R.string.pgn_dm1)
    else -> fallback
}

fun LocalizedSourceName(sourceAddress: Int): String = when (sourceAddress) {
    0x00 -> "Двигатель №1 (EMS)"
    0x01 -> "Двигатель №2"
    0x03 -> "Коробка передач (GMS)"
    0x0B -> "Тормозная система (EBS)"
    0x17 -> "Панель приборов"
    0x21 -> "Координатор систем (COO)"
    0xFF -> "Общая рассылка"
    else -> "Блок управления 0x${sourceAddress.toString(16).uppercase().padStart(2, '0')}"
}

fun LocalizedSpnName(spn: Int, fallback: String): String = when (spn) {
    513 -> "Фактический крутящий момент двигателя"
    190 -> "Частота вращения двигателя"
    92 -> "Нагрузка двигателя при текущих оборотах"
    110 -> "Температура охлаждающей жидкости"
    84 -> "Скорость автомобиля по колёсам"
    168 -> "Напряжение бортовой сети"
    523 -> "Текущая передача коробки передач"
    100 -> "Давление моторного масла"
    117 -> "Давление в тормозном контуре 1"
    96 -> "Уровень топлива"
    else -> if (fallback.startsWith("Active DTC")) "Активный код неисправности" else fallback
}

fun LocalizedSpnDescription(spn: Int, fallback: String): String = when (spn) {
    513 -> "Расчётный крутящий момент двигателя относительно номинального значения."
    190 -> "Фактическая частота вращения коленчатого вала двигателя."
    92 -> "Нагрузка двигателя относительно доступного крутящего момента при текущих оборотах."
    110 -> "Температура охлаждающей жидкости в системе охлаждения двигателя."
    84 -> "Скорость движения автомобиля по данным датчиков колёс или оси."
    168 -> "Напряжение бортовой сети или аккумуляторных батарей."
    523 -> "Текущая включённая передача коробки передач."
    100 -> "Давление масла в системе смазки двигателя."
    117 -> "Давление воздуха в первом контуре тормозной системы."
    96 -> "Уровень топлива в основном топливном баке."
    else -> if (fallback.startsWith("DTC reported via DM1")) "Код неисправности передан в сообщении DM1." else fallback
}

@Composable
fun DiagnosticCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun MetricGaugeCard(
    name: String,
    value: Double,
    unit: String,
    quality: String,
    simulated: Boolean = true,
    minVal: Float = 0f,
    maxVal: Float = 100f,
    modifier: Modifier = Modifier
) {
    val qualityColor = when (quality) {
        "GOOD" -> StatusGreen
        "DEGRADED" -> StatusAmber
        else -> StatusRed
    }

    val progress = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(qualityColor, RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (value % 1.0 == 0.0) "${value.toInt()}" else String.format("%.1f", value),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryAmberLight,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (quality == "BAD") StatusRed else PrimaryAmber,
                trackColor = CardBorder,
            )
        }
    }
}
