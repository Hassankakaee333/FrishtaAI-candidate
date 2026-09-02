package ai.hassan.app.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentCodecTest {
    @Test
    fun roundTripEncodesPendingAttachments() {
        val refs = listOf(
            MessageAttachmentRef(
                uri = "content://test/image.jpg",
                displayName = "image.jpg",
                mimeType = "image/jpeg",
                kind = "IMAGE",
            ),
        )
        val encoded = AttachmentCodec.encode(refs)
        val decoded = AttachmentCodec.decode(encoded)
        assertEquals(1, decoded.size)
        assertEquals("image.jpg", decoded.first().displayName)
    }

    @Test
    fun blankDecodeReturnsEmpty() {
        assertTrue(AttachmentCodec.decode(null).isEmpty())
        assertTrue(AttachmentCodec.decode("").isEmpty())
    }
}
