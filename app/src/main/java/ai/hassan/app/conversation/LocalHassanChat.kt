package ai.hassan.app.conversation

/**
 * On-device conversational replies when no external LLM is configured.
 * Speaks in the selected provider's identity and knows Frishta capabilities.
 */
object LocalHassanChat {
    fun reply(
        userMessage: String,
        history: List<ConversationMessage> = emptyList(),
        providerId: String? = "frishta",
    ): String {
        val provider = normalizeProvider(providerId)
        val name = displayName(provider)
        val text = userMessage.trim()
        if (text.isBlank()) {
            return "أنا $name داخل تطبيق Frishta AI. تفضل، اكتب سؤالك أو المهمة."
        }
        val lower = text.lowercase()

        if (isGreeting(lower)) {
            return "أهلًا! أنا $name داخل Frishta AI. اسألني أو اطلب تحسينًا للتطبيق."
        }

        if (lower.contains("كيف حالك") || lower.contains("كيفك") || lower.contains("شلونك")) {
            return "بخير، شكرًا. أنا $name وجاهز أساعدك داخل Frishta AI."
        }

        if (
            lower.contains("من أنت") || lower.contains("من انت") ||
            lower.contains("ما اسمك") || lower.contains("اسمك")
        ) {
            return "أنا $name داخل تطبيق Frishta AI."
        }

        if (isCapabilitiesQuestion(lower)) {
            return capabilitiesReply(name)
        }

        if (isSelfImproveIntent(lower)) {
            return selfImproveHint(name, text)
        }

        if (isWeatherQuestion(lower)) {
            return "أنا $name.\nأحتاج اتصال السحابة لجلب درجة الحرارة الحالية بدقة. أعد السؤال بعد التأكد من اتصال Hassan Cloud."
        }

        if (lower.contains("شكرا") || lower.contains("شكرًا") || lower.contains("thanks")) {
            return "العفو! أنا $name وموجود متى ما احتجتني."
        }

        val recentUser = history.count {
            it.role.equals("USER", ignoreCase = true) || it.role.equals("user", ignoreCase = true)
        }
        return buildString {
            append("أنا $name داخل Frishta AI. وصلتني رسالتك")
            if (recentUser > 1) append("، وأنا متابع سياق المحادثة")
            append(".\n\n«")
            append(text.take(160))
            append("»\n\n")
            append("يمكنني المحادثة، الطقس عبر السحابة، أو طلب تحسين التطبيق بعبارة مثل «حسّن التطبيق: …» ثم «ابدأ».")
            if (provider != "frishta" && provider != "gemini") {
                append("\nإذا كان مفتاح $name غير مفعّل على السحابة، الرد هنا بهوية $name فقط وليس API حقيقي.")
            }
        }
    }

    fun displayName(providerId: String?): String = when (normalizeProvider(providerId)) {
        "chatgpt" -> "ChatGPT"
        "gemini" -> "Gemini"
        "claude" -> "Claude"
        "deepseek" -> "DeepSeek"
        else -> "Frishta AI"
    }

    fun normalizeProvider(providerId: String?): String {
        val id = providerId?.lowercase()?.trim().orEmpty()
        return when (id) {
            "", "auto", "hassan", "hassan-local", "local", "frishta" -> "frishta"
            else -> id
        }
    }

    fun capabilitiesReply(name: String): String = buildString {
        appendLine("أنا $name داخل تطبيق Frishta AI. إمكانياتي هنا:")
        appendLine("• محادثة طبيعية (والمزود المختار من الشريط العلوي)")
        appendLine("• الطقس ودرجة الحرارة عبر السحابة")
        appendLine("• كلام بالمايك مع رد صوتي")
        appendLine("• تحسين التطبيق ذاتيًا: اكتب «حسّن التطبيق: …» أو «عدّل جزء من …» ثم «ابدأ»")
        appendLine("• تثبيت APK جاهز: أرفق الملف وقل «ثبت التحديث»")
        appendLine("• الرجوع عند الفشل: «ارجع للنسخة السابقة»")
        appendLine("• مشاريع ومهام عبر Hassan Cloud بدون بطاقة ائتمان")
        append("ما الذي تريد تنفيذه الآن؟")
    }

    private fun selfImproveHint(name: String, text: String): String = buildString {
        appendLine("أنا $name داخل Frishta AI.")
        appendLine("لتعديل هذا التطبيق نفسه (واجهة/إصلاح/تحسين):")
        appendLine("1) اكتب طلبًا واضحًا مثل: «حسّن التطبيق: ${text.take(80)}»")
        appendLine("2) بعد ظهور الخطة قل «ابدأ»")
        appendLine("سيتم أخذ نسخة احتياطية ثم بناء سحابي ثم تثبيت، ويمكن الرجوع بالنسخة السابقة إن فشل الأمر.")
    }

    private fun isCapabilitiesQuestion(lower: String): Boolean =
        lower.contains("امكانيات") || lower.contains("إمكانيات") ||
            lower.contains("ماذا تستطيع") || lower.contains("ما تستطيع") ||
            lower.contains("ماذا تقدر") || lower.contains("وش تقدر") ||
            lower.contains("ما قدراتك") || lower.contains("قدراتك") ||
            lower.contains("what can you") || lower.contains("capabilities") ||
            lower.contains("ماذا تعرف") || lower.contains("ما تعرف")

    private fun isSelfImproveIntent(lower: String): Boolean =
        lower.contains("حسن الواجهة") || lower.contains("حسّن الواجهة") ||
            lower.contains("تحسين الواجهة") || lower.contains("تحسين التصميم") ||
            lower.contains("عدل الواجهة") || lower.contains("عدّل الواجهة") ||
            lower.contains("حسن التطبيق") || lower.contains("حسّن التطبيق") ||
            lower.contains("طور التطبيق") || lower.contains("طوّر التطبيق") ||
            lower.contains("صلح") || lower.contains("صلّح") ||
            lower.contains("تحسين التصميم") || lower.contains("improve the ui") ||
            lower.contains("improve the app")

    private fun isWeatherQuestion(lower: String): Boolean =
        lower.contains("طقس") || lower.contains("الجو") || lower.contains("حرارة") ||
            lower.contains("درجة الحرارة") || lower.contains("weather") || lower.contains("temperature")

    private fun isGreeting(lower: String): Boolean {
        val greetings = listOf(
            "مرحبا", "مرحباً", "اهلا", "أهلا", "السلام", "هلا", "hi", "hello", "hey",
            "صباح", "مساء", "marhaba", "marhaban", "salaam", "salam",
        )
        return greetings.any { lower == it || lower.startsWith("$it ") || lower.startsWith("$it،") || lower.startsWith("$it!") }
    }
}

class LocalHassanConversationProvider(
    private val providerIdProvider: () -> String = { "frishta" },
) : ConversationProvider {
    override val isConfigured: Boolean = true

    override suspend fun sendMessage(
        history: List<ConversationMessage>,
        userMessage: String,
    ): ConversationResult {
        val providerId = LocalHassanChat.normalizeProvider(providerIdProvider())
        return ConversationResult.Success(
            answer = LocalHassanChat.reply(userMessage, history, providerId),
            providerId = providerId,
        )
    }
}
