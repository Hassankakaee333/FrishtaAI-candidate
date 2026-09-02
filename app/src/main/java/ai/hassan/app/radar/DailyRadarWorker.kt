package ai.hassan.app.radar

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ai.hassan.app.HassanApplication
import ai.hassan.app.data.RadarStatuses
import java.util.concurrent.TimeUnit

class DailyRadarWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as HassanApplication
        val repository = app.container.repository
        return runCatching {
            repository.runRadarNow(silent = true)
            val findings = repository.latestRadarFindings()
            val verified = findings.count { it.status == RadarStatuses.VERIFIED }
            RadarNotifier.notifyDailySummary(applicationContext, verified, findings.size)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "hassan_daily_radar"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<DailyRadarWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
