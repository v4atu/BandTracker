package com.v4atu.bandtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.v4atu.bandtracker.ui.navigation.AppNavHost
import com.v4atu.bandtracker.ui.theme.BandTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BandTrackerApp).container
        setContent {
            BandTrackerTheme {
                AppNavHost(container)
            }
        }
    }
}
