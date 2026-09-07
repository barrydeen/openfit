package dev.openfit.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.domain.CoachTiming
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime

/**
 * Schedules the daily coach run without any Google services, using WorkManager (AndroidX, backed
 * by AlarmManager/JobScheduler). Each run enqueues the next one, so the schedule survives reboots
 * and re-aligns to the user's preferred delivery time.
 */
class DailyCoachScheduler(
    private val context: Context,
    private val settings: SettingsRepository,
) {

    /** (Re)schedules the next run at the user's preferred time, if the feature is enabled. */
    suspend fun scheduleNext() {
        val minutes = settings.coachTimeMinutes.first()
        enqueue(CoachTiming.nextDelay(minutes, LocalDateTime.now()))
    }

    /** Schedules a run right now (used for "test notification"); the worker reschedules afterwards. */
    suspend fun runNow() {
        enqueue(Duration.ZERO)
    }

    /** Cancels any pending run. */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Runs on app start: makes sure a run is pending without replacing one that is already queued
     * (replacing near the scheduled minute could otherwise skip today's message).
     */
    suspend fun sync() {
        if (!settings.coachEnabled.first()) return
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get()
        if (infos.none { !it.state.isFinished }) scheduleNext()
    }

    private suspend fun enqueue(delay: Duration) {
        val request = OneTimeWorkRequestBuilder<DailyCoachWorker>()
            .setInitialDelay(delay)
            .setConstraints(Constraints(requiresBatteryNotLow = true))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "daily_coach"
    }
}
