package ai.hassan.app

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.hassan.app.data.MessageRoles
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end exploration: simulates user chat and logs Hassan responses.
 * Output tag: HassanExplore
 */
@RunWith(AndroidJUnit4::class)
class FullAppExplorationTest {
    private val app: HassanApplication
        get() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as HassanApplication

    @Test
    fun exploreChatResponsesAndRouting() = runBlocking {
        val repository = app.container.repository
        repository.initialize()

        val scenarios = linkedMapOf(
            "مرحبا" to "CHAT",
            "ما إمكانياتك؟" to "CHAT",
            "من أنت؟" to "CHAT",
            "ما فائدة تطبيق حسن؟" to "CHAT",
            "اشرح لي Kotlin" to "CHAT",
            "حدّث حسن" to "SELF_UPDATE",
            "ابحث عن آخر إصدار Android" to "RESEARCH",
            "ابن تطبيق بسيط للمولدات" to "EXECUTION",
            "اصنع لي صورة" to "MEDIA",
        )

        scenarios.forEach { (userMessage, expectedRoute) ->
            repository.sendChat(userMessage)
            val all = repository.messages.first()
            val hassanReplies = all.filter { it.role == MessageRoles.HASSAN }
            val lastReply = hassanReplies.lastOrNull()?.content.orEmpty()
            val plans = repository.plans.first()
            val activePlans = plans.filter { it.status !in setOf("REJECTED", "COMPLETED", "DISCUSSING") }

            Log.i(TAG, "=== SCENARIO: $expectedRoute ===")
            Log.i(TAG, "USER: $userMessage")
            Log.i(TAG, "HASSAN: $lastReply")
            Log.i(TAG, "ACTIVE_PLANS: ${activePlans.size}")
            Log.i(TAG, "CONVERSATION_PROVIDER_CONFIGURED: ${app.container.repository.conversationUi.value.providerConfigured}")

            when (expectedRoute) {
                "CHAT", "MEDIA" -> assertFalse(
                    "Chat must not create plan for: $userMessage",
                    activePlans.any { it.goal.contains(userMessage.take(8)) },
                )
                "RESEARCH", "EXECUTION" -> assertTrue(
                    "Execution/research should create plan for: $userMessage",
                    lastReply.contains("الخطة جاهزة") || activePlans.isNotEmpty(),
                )
                "SELF_UPDATE" -> assertTrue(
                    "Self-update should mention backup",
                    lastReply.contains("احتياط") || lastReply.contains("تحديث"),
                )
            }
        }
    }

    @Test
    fun exploreProviderCatalogState() = runBlocking {
        val resources = app.container.repository.resources.first()
        Log.i(TAG, "=== PROVIDER CATALOG ===")
        resources.forEach { provider ->
            Log.i(
                TAG,
                "PROVIDER id=${provider.providerId} name=${provider.displayName} " +
                    "humanGated=${provider.requiresHumanBridge} cost=${provider.costClass}",
            )
        }
        val chatGpt = resources.firstOrNull { it.providerId == "chatgpt" }
        val gemini = resources.firstOrNull { it.providerId == "gemini" }
        val deepseek = resources.firstOrNull { it.providerId == "deepseek" }
        assertTrue(chatGpt?.requiresHumanBridge == true)
        assertTrue(gemini?.requiresHumanBridge == true)
        assertTrue(deepseek?.requiresHumanBridge == true)
    }

    @Test
    fun exploreRadarScan() = runBlocking {
        repository().runRadarNow()
        val findings = repository().radarFindings.first()
        Log.i(TAG, "=== RADAR FINDINGS: ${findings.size} ===")
        findings.take(5).forEach { f ->
            Log.i(TAG, "RADAR: ${f.title} | ${f.status} | ${f.version}")
        }
    }

    private fun repository() = app.container.repository

    companion object {
        private const val TAG = "HassanExplore"
    }
}
