package com.nightveil.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskOverlayStateSignalTest {

    @Test
    fun `state change action stays stable`() {
        assertEquals(
            "com.nightveil.app.overlay.STATE_CHANGED",
            MaskOverlayStateSignal.ACTION_STATE_CHANGED
        )
    }
}
