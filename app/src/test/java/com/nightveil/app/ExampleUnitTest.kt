package com.nightveil.app

import com.nightveil.app.model.MaskOverlayModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun defaultOpacity_matchesProductDecision() {
        assertEquals(50, MaskOverlayModel.DEFAULT_OPACITY)
    }
}

