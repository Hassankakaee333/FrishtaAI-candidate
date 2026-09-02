package ai.hassan.app.conversation

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AttachmentKind {
    IMAGE,
    FILE,
    UNKNOWN,
}

/** Attachment selected in the composer but not yet sent. */
data class PendingAttachment(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val kind: AttachmentKind,
)

@Serializable
data class MessageAttachmentRef(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val kind: String,
    val cloudArtifactId: String? = null,
    val remoteUrl: String? = null,
)

object AttachmentCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(attachments: List<MessageAttachmentRef>): String = json.encodeToString(attachments)

    fun decode(raw: String?): List<MessageAttachmentRef> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<MessageAttachmentRef>>(raw) }.getOrDefault(emptyList())
    }

    fun fromPending(pending: PendingAttachment): MessageAttachmentRef = MessageAttachmentRef(
        uri = pending.uri,
        displayName = pending.displayName,
        mimeType = pending.mimeType,
        kind = pending.kind.name,
    )
}
