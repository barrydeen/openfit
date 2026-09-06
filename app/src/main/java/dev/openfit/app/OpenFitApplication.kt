package dev.openfit.app

import android.app.Application
import dev.openfit.app.di.AppContainer

class OpenFitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
