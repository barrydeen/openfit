package dev.openfit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.openfit.app.ui.navigation.OpenFitApp
import dev.openfit.app.ui.theme.OpenFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenFitTheme {
                OpenFitApp()
            }
        }
    }
}
