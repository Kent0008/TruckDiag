package com.truckdiag.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.truckdiag.app.ui.navigation.MainScreen
import com.truckdiag.app.ui.theme.TruckDiagTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TruckDiagTheme {
                MainScreen()
            }
        }
    }
}
