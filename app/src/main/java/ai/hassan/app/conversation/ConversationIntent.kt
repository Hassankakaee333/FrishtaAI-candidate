package ai.hassan.app.conversation

enum class ConversationIntent {
    CHAT,
    RESEARCH,
    EXECUTION,
    PLAN,
    MEDIA,
    AUTOMATION,
    PROJECT,
}

/**
 * Separates normal conversation from execution/research workflows.
 * Default is always [ConversationIntent.CHAT] — never RESEARCH.
 */
object IntentRouter {
    private val researchPhrases = listOf(
        "ابحث عن",
        "ابحث في",
        "ابحث على",
        "ابحث على الإنترنت",
        "search for",
        "ابحث في الرادار",
        "مصادر مجانية",
    )

    private val mediaPhrases = listOf(
        "اصنع لي صورة",
        "اصنع صورة",
        "أنشئ صورة",
        "انشئ صورة",
        "عدل هذه الصورة",
        "عدل الصورة",
        "أنشئ فيديو",
        "انشئ فيديو",
        "اصنع فيديو",
        "حول النص إلى صوت",
        "حول النص الى صوت",
        "استخرج الكلام",
        "حلل هذا الفيديو",
        "generate image",
        "create video",
    )

    private val automationPhrases = listOf(
        "أتمت",
        "اوتمت",
        "جدولة",
        "schedule",
        "automate",
    )

    private val projectPhrases = listOf(
        "ابنِ مشروع",
        "ابن مشروع",
        "أنشئ مشروع",
        "انشئ مشروع",
        "build project",
        "create project",
    )

    private val executionVerbs = listOf(
        "افتح",
        "ثبت",
        "عدّل",
        "عدل",
        "ابنِ",
        "ابن",
        "ابنِ apk",
        "ابن apk",
        "افحص",
        "فحص",
        "شغّل",
        "شغل",
        "نفّذ",
        "نفذ",
        "احذف",
        "غيّر",
        "غير",
        "طبّق",
        "طبق",
        "أزل",
        "ازل",
        "أضف",
        "اضف",
        "طوّر",
        "طور",
        "شغّل الاختبارات",
        "شغل الاختبارات",
        "ابدأ التنفيذ",
        "build",
        "run tests",
        "deploy",
    )

    private val informationalStarts = listOf(
        "ما ",
        "ماذا ",
        "من ",
        "كيف ",
        "لماذا ",
        "هل ",
        "أين ",
        "اين ",
        "متى ",
        "اشرح",
        "لخص",
        "اقترح",
        "ساعدني",
        "أعطني",
        "اعطني",
        "اكتب لي",
        "اكتب",
        "ما رأيك",
        "ما رايك",
        "ما الفرق",
        "ما فائدة",
        "ما هي",
        "ما هو",
    )

    fun classify(message: String): ConversationIntent {
        val normalized = normalize(message)
        if (normalized.isBlank()) return ConversationIntent.CHAT

        if (projectPhrases.any { normalized.contains(it) }) {
            return ConversationIntent.PROJECT
        }

        if (mediaPhrases.any { normalized.contains(it) }) {
            return ConversationIntent.MEDIA
        }

        if (automationPhrases.any { normalized.contains(it) }) {
            return ConversationIntent.AUTOMATION
        }

        if (hasExplicitActionIntent(normalized)) {
            return ConversationIntent.EXECUTION
        }

        if (researchPhrases.any { normalized.contains(it) }) {
            return ConversationIntent.RESEARCH
        }

        return ConversationIntent.CHAT
    }

    private fun hasExplicitActionIntent(text: String): Boolean {
        if (isInformationalQuestion(text)) return false
        return executionVerbs.any { verb -> matchesVerb(text, verb) }
    }

    private fun isInformationalQuestion(text: String): Boolean {
        if (informationalStarts.any { text.startsWith(it) }) return true
        if (text.contains("؟") || text.contains("?")) {
            val hasActionVerb = executionVerbs.any { verb -> matchesVerb(text, verb) }
            if (!hasActionVerb) return true
        }
        return false
    }

    private fun matchesVerb(text: String, verb: String): Boolean =
        text == verb ||
            text.startsWith("$verb ") ||
            text.contains(" $verb ") ||
            text.endsWith(" $verb")

    private fun normalize(message: String): String = message
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}
