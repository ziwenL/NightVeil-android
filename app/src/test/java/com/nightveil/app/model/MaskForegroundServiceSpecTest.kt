package com.nightveil.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskForegroundServiceSpecTest {

    @Test
    fun `special use subtype stays stable`() {
        assertEquals(
            "screen_dimming_overlay_for_night_eye_comfort",
            MaskForegroundServiceSpec.SPECIAL_USE_SUBTYPE
        )
    }
}

