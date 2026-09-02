package ai.hassan.app.identity

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.ProviderException
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class SignedPayload(
    val payload: String,
    val signatureBase64: String,
    val publicKeyBase64: String,
)

data class IdentitySummary(
    val alias: String,
    val publicKeyFingerprint: String,
    val insideSecureHardware: Boolean,
    val strongBoxBacked: Boolean,
)

class DeviceIdentityManager(private val context: Context) {
    private val keyStore: KeyStore
        get() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    @Synchronized
    fun ensureIdentity(): IdentitySummary {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKeyPair(preferStrongBox = context.packageManager.hasSystemFeature(
                "android.hardware.strongbox_keystore",
            ))
        }
        return identitySummary()
    }

    fun sign(payload: String): SignedPayload {
        ensureIdentity()
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val signatureBytes = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(entry.privateKey)
            update(payload.toByteArray(Charsets.UTF_8))
            sign()
        }
        return SignedPayload(
            payload = payload,
            signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP),
            publicKeyBase64 = Base64.encodeToString(entry.certificate.publicKey.encoded, Base64.NO_WRAP),
        )
    }

    fun verify(payload: String, signatureBase64: String): Boolean {
        ensureIdentity()
        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
        return Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(publicKey)
            update(payload.toByteArray(Charsets.UTF_8))
            verify(Base64.decode(signatureBase64, Base64.NO_WRAP))
        }
    }

    fun identitySummary(): IdentitySummary {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val keyInfo = KeyFactory
            .getInstance(entry.privateKey.algorithm, ANDROID_KEYSTORE)
            .getKeySpec(entry.privateKey, KeyInfo::class.java)
        val fingerprint = java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(entry.certificate.publicKey.encoded)
            .take(8)
            .joinToString(":") { "%02X".format(it) }
        return IdentitySummary(
            alias = KEY_ALIAS,
            publicKeyFingerprint = fingerprint,
            insideSecureHardware = keyInfo.securityLevel != KeyProperties.SECURITY_LEVEL_SOFTWARE,
            strongBoxBacked = keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX,
        )
    }

    private fun generateKeyPair(preferStrongBox: Boolean) {
        try {
            createGenerator(preferStrongBox).generateKeyPair()
        } catch (_: StrongBoxUnavailableException) {
            createGenerator(false).generateKeyPair()
        } catch (error: ProviderException) {
            if (!preferStrongBox) throw error
            createGenerator(false).generateKeyPair()
        }
    }

    private fun createGenerator(strongBox: Boolean): KeyPairGenerator {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .setIsStrongBoxBacked(strongBox)
            .build()
        return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE).apply {
            initialize(spec)
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "hassan_device_identity_v1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }
}

object CanonicalPayload {
    private val json = Json { prettyPrint = false }

    fun decision(decisionId: String, action: String, timestamp: Long): String {
        val sorted = sortedMapOf(
            "action" to JsonPrimitive(action),
            "decisionId" to JsonPrimitive(decisionId),
            "timestamp" to JsonPrimitive(timestamp),
            "version" to JsonPrimitive(1),
        )
        return json.encodeToString(JsonObject.serializer(), JsonObject(sorted))
    }
}
