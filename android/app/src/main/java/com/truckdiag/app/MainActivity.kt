package com.truckdiag.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.truckdiag.app.ui.navigation.MainScreen
import com.truckdiag.app.ui.theme.TruckDiagTheme

class MainActivity : AppCompatActivity() {
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
