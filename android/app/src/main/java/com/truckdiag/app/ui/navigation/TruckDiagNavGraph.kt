package com.truckdiag.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.truckdiag.app.ui.screens.canbus.CanBusScreen
import com.truckdiag.app.ui.screens.connection.ConnectionScreen
import com.truckdiag.app.ui.screens.decoder.DecoderScreen
import com.truckdiag.app.ui.screens.faults.FaultsScreen
import com.truckdiag.app.ui.screens.home.HomeScreen
import com.truckdiag.app.ui.screens.livedata.LiveDataScreen
import com.truckdiag.app.ui.screens.reports.ReportsScreen
import com.truckdiag.app.ui.screens.sessions.SessionsScreen
import com.truckdiag.app.ui.screens.settings.SettingsScreen

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.LiveData.route) {
                LiveDataScreen(navController = navController)
            }
            composable(Screen.Faults.route) {
                FaultsScreen(navController = navController)
            }
            composable(Screen.CanBus.route) {
                CanBusScreen(navController = navController)
            }
            composable(Screen.Decoder.route) {
                DecoderScreen(navController = navController)
            }
            composable(Screen.Sessions.route) {
                SessionsScreen(navController = navController)
            }
            composable(Screen.Reports.route) {
                ReportsScreen(navController = navController)
            }
            composable(Screen.Connection.route) {
                ConnectionScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
