package ai.hassan.app.capabilities

import ai.hassan.app.policy.CostClass

enum class CapabilityStatus {
    WORKING,
    PARTIAL,
    HUMAN_GATED,
    PLACEHOLDER,
    NOT_CONFIGURED,
    UNAVAILABLE,
}

data class CapabilityDescriptor(
    val id: String,
    val label: String,
    val status: CapabilityStatus,
    val costClass: CostClass,
    val providers: List<String>,
    val notes: String = "",
)

/**
 * Registry of what Hassan can actually do — queried by Hassan Auto routing.
 */
object CapabilityRegistry {
    val all: List<CapabilityDescriptor> = listOf(
        CapabilityDescriptor(
            id = "NORMAL_CHAT",
            label = "محادثة طبيعية",
            status = CapabilityStatus.NOT_CONFIGURED,
            costClass = CostClass.FREE,
            providers = listOf("hassan-auto", "chatgpt", "gemini", "deepseek"),
            notes = "يتطلب Hassan Cloud مُعدّاً",
        ),
        CapabilityDescriptor(
            id = "WEB_SEARCH",
            label = "بحث ويب",
            status = CapabilityStatus.PARTIAL,
            costClass = CostClass.FREE,
            providers = listOf("radar", "cloud-research"),
        ),
        CapabilityDescriptor(
            id = "ANDROID_BUILD",
            label = "بناء Android",
            status = CapabilityStatus.PLACEHOLDER,
            costClass = CostClass.FREE,
            providers = listOf("cloud-build-agent"),
        ),
        CapabilityDescriptor(
            id = "CODE_REVIEW",
            label = "مراجعة كود",
            status = CapabilityStatus.PARTIAL,
            costClass = CostClass.FREE,
            providers = listOf("cloud-reviewer-agent"),
        ),
        CapabilityDescriptor(
            id = "PDF_ANALYSIS",
            label = "تحليل PDF",
            status = CapabilityStatus.PARTIAL,
            costClass = CostClass.FREE,
            providers = listOf("cloud-chat"),
            notes = "يتطلب رفع الملف إلى السحابة",
        ),
        CapabilityDescriptor(
            id = "IMAGE_ANALYSIS",
            label = "تحليل صورة",
            status = CapabilityStatus.PARTIAL,
            costClass = CostClass.FREE,
            providers = listOf("cloud-chat"),
        ),
        CapabilityDescriptor(
            id = "HUMAN_BRIDGE",
            label = "جسر بشري",
            status = CapabilityStatus.HUMAN_GATED,
            costClass = CostClass.FREE,
            providers = listOf("chatgpt", "gemini", "deepseek"),
        ),
        CapabilityDescriptor(
            id = "RADAR_DISCOVERY",
            label = "اكتشاف أدوات مجانية",
            status = CapabilityStatus.WORKING,
            costClass = CostClass.FREE,
            providers = listOf("official-source-radar"),
        ),
        CapabilityDescriptor(
            id = "SELF_UPDATE",
            label = "تحديث ذاتي",
            status = CapabilityStatus.WORKING,
            costClass = CostClass.FREE,
            providers = listOf("local"),
        ),
    )

    fun find(id: String): CapabilityDescriptor? = all.firstOrNull { it.id == id }

    fun forIntentKeywords(text: String): List<CapabilityDescriptor> {
        val normalized = text.lowercase()
        return all.filter { cap ->
            when (cap.id) {
                "NORMAL_CHAT" -> true
                "WEB_SEARCH" -> normalized.contains("ابحث") || normalized.contains("search")
                "ANDROID_BUILD" -> normalized.contains("apk") || normalized.contains("ابن")
                "IMAGE_ANALYSIS" -> normalized.contains("صورة") || normalized.contains("image")
                "PDF_ANALYSIS" -> normalized.contains("pdf")
                else -> false
            }
        }
    }
}
