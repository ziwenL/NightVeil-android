package com.nightveil.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskReleaseSigningConfigSpecTest {

    @Test
    fun `release signing keys stay stable`() {
        assertEquals("app/nightveil.keystore", MaskReleaseSigningConfigSpec.KEYSTORE_RELATIVE_PATH)
        assertEquals("nightveil.signing.storeFile", MaskReleaseSigningConfigSpec.STORE_FILE_KEY)
        assertEquals("nightveil.signing.storePassword", MaskReleaseSigningConfigSpec.STORE_PASSWORD_KEY)
        assertEquals("nightveil.signing.keyAlias", MaskReleaseSigningConfigSpec.KEY_ALIAS_KEY)
        assertEquals("nightveil.signing.keyPassword", MaskReleaseSigningConfigSpec.KEY_PASSWORD_KEY)
    }
}
