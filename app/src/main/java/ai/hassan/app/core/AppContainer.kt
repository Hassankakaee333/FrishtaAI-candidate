package ai.hassan.app.core

import android.content.Context
import ai.hassan.app.cloud.CloudJobOrchestratorImpl
import ai.hassan.app.cloud.CloudTaskOrchestrator
import ai.hassan.app.cloud.HassanCloudApi
import ai.hassan.app.conversation.ConversationSettingsStore
import ai.hassan.app.conversation.HassanConversationProvider
import ai.hassan.app.data.ActiveConversationStore
import ai.hassan.app.data.HassanDatabase
import ai.hassan.app.data.HassanRepository
import ai.hassan.app.identity.DeviceIdentityManager
import ai.hassan.app.radar.CompositeRadarScanner
import ai.hassan.app.radar.GitHubReleaseFeed
import ai.hassan.app.radar.OfficialSourceRadar
import ai.hassan.app.selfupdate.SelfUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = HassanDatabase.create(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS)
        .build()

    val conversationSettingsStore = ConversationSettingsStore(context)
    val hassanCloudApi = HassanCloudApi(httpClient)
    val identityManager = DeviceIdentityManager(context)
    val selfUpdateManager = SelfUpdateManager(context, httpClient)

    private val radarScanner = CompositeRadarScanner(
        feeds = listOf(GitHubReleaseFeed(OfficialSourceRadar(httpClient))),
    )

    val cloudTaskOrchestrator: CloudTaskOrchestrator =
        CloudJobOrchestratorImpl(conversationSettingsStore, hassanCloudApi, database)

    val repository = HassanRepository(
        context = context,
        database = database,
        identityManager = identityManager,
        radarScanner = radarScanner,
        conversationProvider = HassanConversationProvider(
            settingsStore = conversationSettingsStore,
            cloudApi = hassanCloudApi,
        ),
        activeConversationStore = ActiveConversationStore(context),
        selfUpdateManager = selfUpdateManager,
        conversationSettingsStore = conversationSettingsStore,
        cloudTaskOrchestrator = cloudTaskOrchestrator,
        hassanCloudApi = hassanCloudApi,
    )

    init {
        applicationScope.launch {
            identityManager.ensureIdentity()
            repository.initialize()
        }
    }
}
