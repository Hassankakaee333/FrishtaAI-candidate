package ai.hassan.app.bench

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HassanBenchTest {
    @Test
    fun evaluatorUsesEvidenceAndCatalogContainsRealRegressions() {
        val evaluator = ReadOnlyHassanBenchEvaluator()
        val case = HassanBenchCatalog.publicCases.first { it.id == "share_persistence" }
        assertTrue(evaluator.evaluate(case, mapOf(case.id to true)).passed)
        assertFalse(evaluator.evaluate(case, emptyMap()).passed)
        assertTrue(HassanBenchCatalog.publicCases.any { it.id == "composer_overlap" })
    }
}
