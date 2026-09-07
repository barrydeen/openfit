package dev.openfit.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.openfit.app.di.AppContainer
import dev.openfit.app.notify.DailyCoachScheduler
import dev.openfit.app.notify.DailyCoachWorker
import kotlinx.coroutines.launch

class OpenFitApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch {
            container.dailyCoachScheduler.sync()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(OpenFitWorkerFactory(container))
            .build()
}

/** Builds workers with manual DI from [AppContainer]. */
class OpenFitWorkerFactory(private val container: AppContainer) : WorkerFactory() {
    override fun createWorker(
        appContext: android.content.Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ) = when (workerClassName) {
        DailyCoachWorker::class.java.name ->
            DailyCoachWorker(
                appContext,
                workerParameters,
                container.dailyCoach,
                DailyCoachScheduler(appContext, container.settingsRepository),
            )
        else -> null
    }
}
