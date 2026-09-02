package ai.hassan.app

import android.app.Application
import ai.hassan.app.core.AppContainer
import ai.hassan.app.cloud.CloudJobSyncWorker
import ai.hassan.app.radar.DailyRadarWorker

class HassanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DailyRadarWorker.schedule(this)
        CloudJobSyncWorker.schedule(this)
    }
}
