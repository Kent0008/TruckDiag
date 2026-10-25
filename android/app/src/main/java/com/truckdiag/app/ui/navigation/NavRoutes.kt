package com.truckdiag.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.truckdiag.app.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object LiveData : Screen("livedata", R.string.nav_livedata, Icons.Default.Speed)
    object Faults : Screen("faults", R.string.nav_faults, Icons.Default.Warning)
    object CanBus : Screen("canbus", R.string.nav_canbus, Icons.Default.Memory)
    object Decoder : Screen("decoder", R.string.nav_decoder, Icons.Default.Code)
    object Sessions : Screen("sessions", R.string.nav_sessions, Icons.Default.History)
    object Reports : Screen("reports", R.string.nav_reports, Icons.Default.Description)
    object Connection : Screen("connection", R.string.nav_connection, Icons.Default.Sensors)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.LiveData,
    Screen.Faults,
    Screen.CanBus,
    Screen.Sessions
)
