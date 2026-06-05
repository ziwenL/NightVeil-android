package com.nightveil.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskOverlayAnimationModelTest {

    @Test
    fun `circular reveal requires api 21 and positive size`() {
        assertTrue(
            MaskOverlayAnimationModel.shouldUseCircularReveal(
                sdkInt = 21,
                width = 100,
                height = 200
            )
        )
        assertFalse(
            MaskOverlayAnimationModel.shouldUseCircularReveal(
                sdkInt = 20,
                width = 100,
                height = 200
            )
        )
        assertFalse(
            MaskOverlayAnimationModel.shouldUseCircularReveal(
                sdkInt = 21,
                width = 0,
                height = 200
            )
        )
    }

    @Test
    fun `hide animation mirrors show timing language`() {
        assertEquals(280L, MaskOverlayAnimationModel.SHOW_DURATION_MS)
        assertEquals(240L, MaskOverlayAnimationModel.HIDE_DURATION_MS)
    }
}
