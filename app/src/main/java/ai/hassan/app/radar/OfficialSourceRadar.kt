package ai.hassan.app.radar

import ai.hassan.app.data.RadarFindingEntity
import ai.hassan.app.data.RadarStatuses
import ai.hassan.app.policy.CostClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class RadarSource(
    val id: String,
    val displayName: String,
    val apiUrl: String,
    val publicUrl: String,
    val license: String,
)

interface RadarScanner {
    suspend fun scan(): List<RadarFindingEntity>
}

/**
 * Small real radar: queries public release endpoints belonging to authoritative
 * open-source repositories. It never sends credentials and never enables a provider.
 */
class OfficialSourceRadar(
    private val client: OkHttpClient,
    private val sources: List<RadarSource> = DEFAULT_SOURCES,
) : RadarScanner {
    override suspend fun scan(): List<RadarFindingEntity> = withContext(Dispatchers.IO) {
        sources.map { source -> scanOne(source) }
    }

    private fun scanOne(source: RadarSource): RadarFindingEntity {
        val now = System.currentTimeMillis()
        return runCatching {
            val request = Request.Builder()
                .url(source.apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Hassan-AI-ZeroCost-Radar/2")
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val body = response.body.string()
                require(body.length <= 512_000) { "Response exceeds radar safety limit" }
                val json = JSONObject(body)
                val version = json.optString("tag_name").ifBlank { "latest" }
                val title = json.optString("name").ifBlank { "${source.displayName} $version" }
                RadarFindingEntity(
                    id = "${source.id}:$version",
                    sourceId = source.id,
                    title = title,
                    summary = "إصدار موثّق من المستودع الرسمي: $version",
                    sourceUrl = json.optString("html_url").ifBlank { source.publicUrl },
                    version = version,
                    status = RadarStatuses.VERIFIED,
                    costClass = CostClass.FREE.name,
                    license = source.license,
                    discoveredAt = now,
                    lastVerifiedAt = now,
                    sourceEvidence = source.apiUrl,
                )
            }
        }.getOrElse { error ->
            RadarFindingEntity(
                id = "${source.id}:check",
                sourceId = source.id,
                title = source.displayName,
                summary = "تعذر التحقق الآن: ${error.message ?: error::class.java.simpleName}",
                sourceUrl = source.publicUrl,
                version = "unknown",
                status = RadarStatuses.FAILED,
                costClass = CostClass.FREE.name,
                license = source.license,
                discoveredAt = now,
                lastVerifiedAt = now,
                sourceEvidence = source.apiUrl,
            )
        }
    }

    companion object {
        val DEFAULT_SOURCES = listOf(
            RadarSource(
                id = "ollama",
                displayName = "Ollama",
                apiUrl = "https://api.github.com/repos/ollama/ollama/releases/latest",
                publicUrl = "https://github.com/ollama/ollama",
                license = "MIT",
            ),
            RadarSource(
                id = "llama-cpp",
                displayName = "llama.cpp",
                apiUrl = "https://api.github.com/repos/ggml-org/llama.cpp/releases/latest",
                publicUrl = "https://github.com/ggml-org/llama.cpp",
                license = "MIT",
            ),
            RadarSource(
                id = "transformers",
                displayName = "Transformers",
                apiUrl = "https://api.github.com/repos/huggingface/transformers/releases/latest",
                publicUrl = "https://github.com/huggingface/transformers",
                license = "Apache-2.0",
            ),
        )
    }
}
