package ai.hassan.app

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.hassan.app.identity.DeviceIdentityManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceIdentityInstrumentedTest {
    @Test
    fun keystoreIdentitySignsAndVerifiesOnRealDevice() {
        val context = ApplicationProvider.getApplicationContext<HassanApplication>()
        val identity = DeviceIdentityManager(context)
        val summary = identity.ensureIdentity()
        val payload = """{"action":"DEVICE_TEST","version":1}"""
        val signed = identity.sign(payload)

        Log.i(
            "HassanIdentityTest",
            "hardwareBacked=${summary.insideSecureHardware}, strongBox=${summary.strongBoxBacked}, fingerprint=${summary.publicKeyFingerprint}",
        )
        assertTrue(identity.verify(payload, signed.signatureBase64))
        assertTrue(summary.publicKeyFingerprint.isNotBlank())
        assertNotNull(signed.publicKeyBase64)
    }
}
