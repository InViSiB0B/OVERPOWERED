package com.example.overpowered

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.overpowered.navigation.MainNavigation
import com.example.overpowered.ui.theme.OVERPOWEREDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OVERPOWEREDTheme {
                MainNavigation()
            }
        }
    }
}
