package dev.openfit.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.openfit.app.coach.DailyCoach

/**
 * Runs once per day: generates the coach message from the last 7 days of meals and workouts and
 * posts it as a local notification, then schedules the next run.
 */
class DailyCoachWorker(
    appContext: Context,
    params: WorkerParameters,
    private val coach: DailyCoach,
    private val scheduler: DailyCoachScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (coach.isEnabled()) {
            val message = coach.generate()
            if (CoachNotifications.canPost(applicationContext)) {
                CoachNotifications.showDailyCoach(applicationContext, message.text)
            }
            scheduler.scheduleNext()
        } else {
            scheduler.cancel()
        }
        return Result.success()
    }
}
