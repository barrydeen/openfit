package dev.openfit.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.openfit.app.OpenFitApplication
import dev.openfit.app.di.AppContainer

@Composable
fun appContainer(): AppContainer {
    val context = LocalContext.current
    return (context.applicationContext as OpenFitApplication).container
}
