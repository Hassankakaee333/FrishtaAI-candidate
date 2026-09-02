package ai.hassan.app.providers

import ai.hassan.app.conversation.ConversationIntent
import ai.hassan.app.execution.ExecutionState
import ai.hassan.app.policy.CostClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class Capability {
    CHAT, CODING, RESEARCH, REVIEW, WEB, IMAGE, VIDEO, AUDIO, TTS, STT,
    ANDROID_TEST, BUILD, BROWSER, COMPUTE, DIAGNOSTIC,
}

enum class ProviderAvailability { AVAILABLE, HUMAN_GATED, UNAVAILABLE, BLOCKED }
enum class ProviderHealth { HEALTHY, DEGRADED, UNKNOWN, OFFLINE }

enum class CodexReasoningEffort(
    val apiValue: String,
    val arabicLabel: String,
    val guidance: String,
) {
    NONE("none", "عادي", "بلا تفكير ممتد — الأسرع"),
    LOW("low", "منخفض", "تفكير خفيف"),
    MEDIUM("medium", "متوسط", "متوازن — الافتراضي"),
    HIGH("high", "عالٍ", "للمهام المعقدة"),
    XHIGH("xhigh", "عالٍ جدًا", "استكشاف وتحقق أعمق"),
    MAX("max", "أقصى", "لأصعب المهام التي تفضّل الجودة"),
    ;

    companion object {
        fun fromApiValue(value: String): CodexReasoningEffort =
            entries.firstOrNull { it.apiValue == value } ?: MEDIUM
    }
}

data class ProviderDescriptor(
    val id: String,
    val name: String,
    val modelId: String? = null,
    val capabilities: Set<Capability>,
    val costClass: CostClass,
    val quota: String,
    val quotaReset: String? = null,
    val privacyGrade: String,
    val licenseInfo: String,
    val commercialUse: String,
    val geoEligibility: String,
    val health: ProviderHealth,
    val reliability: Double,
    val latency: String,
    val requiresCard: Boolean,
    val requiresSubscription: Boolean,
    val requiresHumanBridge: Boolean,
    val availability: ProviderAvailability,
    val enabled: Boolean,
)

data class LeadBrainContext(
    val conversationId: String,
    val goal: String,
    val state: ExecutionState,
)

data class ProviderAction(val state: ExecutionState, val message: String)
data class ProviderEvent(val state: ExecutionState, val message: String)

interface LeadBrainProvider {
    val descriptor: ProviderDescriptor
    suspend fun createConversation(context: LeadBrainContext): ProviderAction
    suspend fun sendMessage(context: LeadBrainContext, message: String): ProviderAction
    suspend fun createPlan(context: LeadBrainContext): ProviderAction
    suspend fun approvePlan(context: LeadBrainContext): ProviderAction
    suspend fun execute(context: LeadBrainContext): ProviderAction
    suspend fun cancel(context: LeadBrainContext): ProviderAction
    suspend fun getStatus(context: LeadBrainContext): ProviderAction
    fun streamEvents(conversationId: String): Flow<ProviderEvent>
}

interface CapabilityProvider {
    val descriptor: ProviderDescriptor
    fun supports(capability: Capability): Boolean = capability in descriptor.capabilities
}

data class TaskPack(
    val taskId: String,
    val conversationId: String,
    val providerId: String,
    val requestedModel: String?,
    val reasoningEffort: String?,
    val goal: String,
    val context: String,
    val constraints: List<String>,
    val question: String,
    val expectedOutput: String,
    val relevantEvidence: List<String>,
    val attachmentsMetadata: List<String>,
) {
    fun asShareText(): String = buildString {
        appendLine("Hassan AI TaskPack")
        appendLine("Task: $taskId")
        requestedModel?.let { appendLine("Requested model: $it") }
        reasoningEffort?.let { appendLine("Requested reasoning effort: $it") }
        appendLine("Goal: $goal")
        appendLine("Context: $context")
        appendLine("Constraints:")
        constraints.forEach { appendLine("- $it") }
        appendLine("Question: $question")
        appendLine("Expected output: $expectedOutput")
        if (relevantEvidence.isNotEmpty()) appendLine("Evidence: ${relevantEvidence.joinToString()}")
        appendLine("Return the answer explicitly to Hassan AI using Share or Paste.")
    }
}

interface HumanGatedProvider : LeadBrainProvider {
    fun buildTaskPack(context: LeadBrainContext, taskId: String): TaskPack
}

class HumanGatedLeadBrain(
    override val descriptor: ProviderDescriptor,
    private val codexReasoningEffort: CodexReasoningEffort = CodexReasoningEffort.MEDIUM,
) : HumanGatedProvider, CapabilityProvider {
    override suspend fun createConversation(context: LeadBrainContext) = gated("Conversation ready")
    override suspend fun sendMessage(context: LeadBrainContext, message: String) = gated("Human bridge required")
    override suspend fun createPlan(context: LeadBrainContext) = gated("TaskPack is ready")
    override suspend fun approvePlan(context: LeadBrainContext) = gated("Approval recorded locally")
    override suspend fun execute(context: LeadBrainContext) = gated("Waiting for a returned provider response")
    override suspend fun cancel(context: LeadBrainContext) = ProviderAction(ExecutionState.REJECTED, "Cancelled")
    override suspend fun getStatus(context: LeadBrainContext) = gated("Human-gated")
    override fun streamEvents(conversationId: String): Flow<ProviderEvent> = emptyFlow()

    override fun buildTaskPack(context: LeadBrainContext, taskId: String) = TaskPack(
        taskId = taskId,
        conversationId = context.conversationId,
        providerId = descriptor.id,
        requestedModel = descriptor.modelId,
        reasoningEffort = descriptor.modelId?.let { codexReasoningEffort.apiValue },
        goal = context.goal,
        context = "Personal single-user Hassan AI project.",
        constraints = listOf(
            "No additional spending; actual cost must remain 0.",
            "Do not use paid APIs, scraping, Accessibility, cookies, or session extraction.",
            "Separate verified facts from assumptions.",
            "If a model is requested above, verify it in Codex before execution; the Android Share bridge cannot enforce external app model selection.",
        ),
        question = "Discuss the goal and return a concrete, verifiable response.",
        expectedOutput = "A concise response or implementation plan with risks and verification.",
        relevantEvidence = emptyList(),
        attachmentsMetadata = emptyList(),
    )

    private fun gated(message: String) = ProviderAction(ExecutionState.NEEDS_INPUT, message)
}

object ProviderCatalog {
    val chatGpt = ProviderDescriptor(
        id = "chatgpt", name = "Codex 5.6 Sol", modelId = "gpt-5.6-sol",
        capabilities = setOf(Capability.CODING, Capability.RESEARCH, Capability.REVIEW),
        costClass = CostClass.PREPAID, quota = "Subscription limits", privacyGrade = "B", licenseInfo = "Subscription terms",
        commercialUse = "Per service terms", geoEligibility = "Account-dependent", health = ProviderHealth.UNKNOWN,
        reliability = 0.0, latency = "Human-gated", requiresCard = false, requiresSubscription = true,
        requiresHumanBridge = true, availability = ProviderAvailability.HUMAN_GATED, enabled = true,
    )
    val gemini = ProviderDescriptor(
        id = "gemini", name = "Gemini", capabilities = setOf(Capability.RESEARCH, Capability.CODING, Capability.REVIEW),
        costClass = CostClass.PREPAID, quota = "Subscription limits", privacyGrade = "B", licenseInfo = "Subscription terms",
        commercialUse = "Per service terms", geoEligibility = "Account-dependent", health = ProviderHealth.UNKNOWN,
        reliability = 0.0, latency = "Human-gated", requiresCard = false, requiresSubscription = true,
        requiresHumanBridge = true, availability = ProviderAvailability.HUMAN_GATED, enabled = true,
    )
    val deepSeek = ProviderDescriptor(
        id = "deepseek", name = "DeepSeek", capabilities = setOf(Capability.REVIEW, Capability.RESEARCH),
        costClass = CostClass.FREE, quota = "App limits", privacyGrade = "C", licenseInfo = "App terms",
        commercialUse = "Review required", geoEligibility = "App availability", health = ProviderHealth.UNKNOWN,
        reliability = 0.0, latency = "Human-gated", requiresCard = false, requiresSubscription = false,
        requiresHumanBridge = true, availability = ProviderAvailability.HUMAN_GATED, enabled = true,
    )
    val localRadar = ProviderDescriptor(
        id = "official-radar", name = "Official Source Radar", capabilities = setOf(Capability.RESEARCH, Capability.WEB),
        costClass = CostClass.FREE, quota = "Public endpoint limits", privacyGrade = "A", licenseInfo = "Source-specific",
        commercialUse = "Metadata only", geoEligibility = "Global web", health = ProviderHealth.HEALTHY,
        reliability = 1.0, latency = "Network", requiresCard = false, requiresSubscription = false,
        requiresHumanBridge = false, availability = ProviderAvailability.AVAILABLE, enabled = true,
    )
    val meteredBlock = ProviderDescriptor(
        id = "metered-api", name = "Metered APIs", capabilities = Capability.entries.toSet(),
        costClass = CostClass.METERED, quota = "Blocked", privacyGrade = "Unknown", licenseInfo = "Unknown",
        commercialUse = "Blocked", geoEligibility = "Blocked", health = ProviderHealth.OFFLINE,
        reliability = 0.0, latency = "Blocked", requiresCard = true, requiresSubscription = false,
        requiresHumanBridge = false, availability = ProviderAvailability.BLOCKED, enabled = false,
    )
    val all = listOf(chatGpt, gemini, deepSeek, localRadar, meteredBlock)
}

object DeterministicAutoRouter {
    private val codingWords = listOf("كود", "برمج", "android", "build", "اختبار", "apk")
    private val reviewWords = listOf("راجع", "انتقد", "تدقيق", "review")
    private val researchWords = listOf("ابحث", "بحث", "رادار", "مصادر", "research")
    private val diagnosticWords = listOf("افحص", "فحص", "تشخيص", "diagnostic", "هاتف")

    fun classify(goal: String, intent: ConversationIntent = ConversationIntent.EXECUTION): Capability {
        if (intent == ConversationIntent.RESEARCH) return Capability.RESEARCH
        if (intent == ConversationIntent.CHAT) return Capability.CHAT
        return classifyExecution(goal)
    }

    private fun classifyExecution(goal: String): Capability {
        val normalized = goal.lowercase()
        return when {
            diagnosticWords.any(normalized::contains) -> Capability.DIAGNOSTIC
            reviewWords.any(normalized::contains) -> Capability.REVIEW
            researchWords.any(normalized::contains) -> Capability.RESEARCH
            codingWords.any(normalized::contains) -> Capability.CODING
            else -> Capability.CODING
        }
    }

    fun route(
        goal: String,
        intent: ConversationIntent = ConversationIntent.EXECUTION,
        providers: List<ProviderDescriptor> = ProviderCatalog.all,
    ): ProviderDescriptor? {
        val capability = classify(goal, intent)
        if (capability == Capability.CHAT) return null
        val order = when (capability) {
            Capability.RESEARCH, Capability.WEB, Capability.DIAGNOSTIC ->
                listOf("official-radar", "gemini", "deepseek", "chatgpt")
            Capability.REVIEW -> listOf("deepseek", "chatgpt", "gemini")
            else -> listOf("chatgpt", "gemini", "deepseek")
        }
        return order.asSequence()
            .mapNotNull { id -> providers.firstOrNull { it.id == id } }
            .firstOrNull { it.enabled && it.availability != ProviderAvailability.BLOCKED && it.costClass != CostClass.METERED }
    }
}
