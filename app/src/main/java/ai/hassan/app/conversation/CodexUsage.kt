package ai.hassan.app.conversation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

data class CodexUsageWindow(val label: String, val remainingPercent: Int)

data class CodexUsageSnapshot(val windows: List<CodexUsageWindow>)

/** Converts the rate-limit shape returned by Codex into display-only remaining percentages. */
internal object CodexUsageParser {
    private val supportedOrder = listOf("5 ساعات", "يومي", "أسبوعي", "شهري")

    fun parse(element: JsonElement?): CodexUsageSnapshot? {
        val found = linkedMapOf<String, CodexUsageWindow>()
        collect(element, null, found)
        return supportedOrder.mapNotNull(found::get).takeIf { it.isNotEmpty() }?.let(::CodexUsageSnapshot)
    }

    private fun collect(element: JsonElement?, parentKey: String?, destination: MutableMap<String, CodexUsageWindow>) {
        if (element is JsonArray) {
            element.forEach { collect(it, parentKey, destination) }
            return
        }
        if (element !is JsonObject) return
        val minutes = element.number("window_minutes", "windowMinutes", "window_duration_mins", "windowDurationMins")?.toInt()
        val label = labelFor(parentKey, minutes)
        val remaining = element.number("remaining_percent", "remainingPercent")
            ?: element.number("used_percent", "usedPercent")?.let { 100.0 - it }
        if (label != null && remaining != null) {
            destination[label] = CodexUsageWindow(label, remaining.toInt().coerceIn(0, 100))
        }
        element.forEach { (key, value) -> collect(value, key, destination) }
    }

    private fun JsonObject.number(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.doubleOrNull }

    private fun labelFor(key: String?, minutes: Int?): String? {
        val normalized = key.orEmpty().lowercase().replace("-", "_")
        return when {
            normalized in setOf("five_hours", "five_hour", "5_hours", "5_hour", "5h") -> "5 ساعات"
            normalized in setOf("daily", "day", "one_day") -> "يومي"
            normalized in setOf("weekly", "week", "one_week", "secondary") -> "أسبوعي"
            normalized in setOf("monthly", "month", "one_month") -> "شهري"
            minutes == 300 -> "5 ساعات"
            minutes == 1_440 -> "يومي"
            minutes == 10_080 -> "أسبوعي"
            minutes != null && minutes in 40_000..45_000 -> "شهري"
            else -> null
        }
    }
}
