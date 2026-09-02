package ai.hassan.app.bench

data class HassanBenchCase(
    val id: String,
    val title: String,
    val category: String,
    val hidden: Boolean,
    val passCriteria: String,
)

data class HassanBenchResult(
    val caseId: String,
    val passed: Boolean,
    val latencyMs: Long,
    val regression: Boolean,
    val crash: Boolean,
    val qualityScore: Double,
)

interface TrustedEvaluator {
    fun evaluate(case: HassanBenchCase, evidence: Map<String, Boolean>): HassanBenchResult
}

class ReadOnlyHassanBenchEvaluator : TrustedEvaluator {
    override fun evaluate(case: HassanBenchCase, evidence: Map<String, Boolean>): HassanBenchResult {
        val start = System.nanoTime()
        val passed = evidence[case.id] == true
        return HassanBenchResult(
            caseId = case.id,
            passed = passed,
            latencyMs = (System.nanoTime() - start) / 1_000_000,
            regression = !passed,
            crash = false,
            qualityScore = if (passed) 1.0 else 0.0,
        )
    }
}

object HassanBenchCatalog {
    val publicCases = listOf(
        HassanBenchCase("composer_overlap", "Composer remains visible", "UI", false, "Composer is displayed and clickable"),
        HassanBenchCase("settings_access", "Settings stays reachable", "UI", false, "Drawer opens Settings"),
        HassanBenchCase("share_persistence", "Share Sheet persists", "DATA", false, "Incoming text remains after restart"),
        HassanBenchCase("candidate_build", "Candidate builds", "BUILD", false, "APK exists and package is candidate"),
        HassanBenchCase("zero_cost", "Zero cost policy", "SECURITY", false, "METERED and positive spend are blocked"),
    )
}
