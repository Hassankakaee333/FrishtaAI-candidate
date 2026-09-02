package ai.hassan.app.selfupdate

enum class SelfImprovementAction {
    /** Install attached/remote APK after backup. */
    APPLY_UPDATE,
    /** Request cloud build of candidate improvements, then install. */
    REQUEST_SELF_IMPROVE,
    ROLLBACK,
    CHECK_UPDATE,
}

/**
 * Detects when the user wants Hassan to modify or update itself on-device.
 */
object SelfImprovementRouter {
    private val rollbackPhrases = listOf(
        "ارجع للنسخة السابقة",
        "استرجع النسخة الاحتياطية",
        "ارجع للنسخة الاحتياطية",
        "rollback",
        "restore backup",
    )

    private val updatePhrases = listOf(
        "حدث التطبيق",
        "حدّث التطبيق",
        "حدث حسن",
        "حدّث حسن",
        "حدث نفسك",
        "حدّث نفسك",
        "طبق التحديث على نفسك",
        "ثبت التحديث",
        "update app",
        "update hassan",
        "install update",
    )

    private val modifyPhrases = listOf(
        "عدل التطبيق",
        "عدّل التطبيق",
        "عدل جزء من",
        "عدّل جزء من",
        "غيّر جزء من التطبيق",
        "غير جزء من التطبيق",
        "عدل واجهة",
        "عدّل واجهة",
        "عدل قسم من",
        "عدّل قسم من",
        "حسّن التطبيق",
        "حسن التطبيق",
        "طور التطبيق",
        "طوّر التطبيق",
        "صلح التطبيق",
        "صلّح التطبيق",
        "عدّل نفسك",
        "عدل نفسك",
        "improve the app",
        "fix the app",
        "self improve",
        "self-improve",
    )

    fun classify(message: String): SelfImprovementAction? {
        val normalized = message.trim().lowercase().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null
        if (rollbackPhrases.any { normalized.contains(it) }) return SelfImprovementAction.ROLLBACK
        // Modify/self-improve before generic "update" so «حسّن التطبيق» is not treated as APK-only.
        if (modifyPhrases.any { normalized.contains(it) }) return SelfImprovementAction.REQUEST_SELF_IMPROVE
        if (updatePhrases.any { normalized.contains(it) }) return SelfImprovementAction.APPLY_UPDATE
        if (normalized.contains("تحقق من التحديث") || normalized.contains("check for update")) {
            return SelfImprovementAction.CHECK_UPDATE
        }
        return null
    }
}
