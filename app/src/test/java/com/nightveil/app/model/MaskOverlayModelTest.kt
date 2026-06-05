package com.nightveil.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskOverlayModelTest {

    @Test
    fun `coerceOpacity keeps value inside allowed range`() {
        assertEquals(20, MaskOverlayModel.coerceOpacity(-5))
        assertEquals(45, MaskOverlayModel.coerceOpacity(45))
        assertEquals(90, MaskOverlayModel.coerceOpacity(140))
    }

    @Test
    fun `increaseOpacity moves by step until max`() {
        assertEquals(60, MaskOverlayModel.increaseOpacity(50))
        assertEquals(90, MaskOverlayModel.increaseOpacity(88))
    }

    @Test
    fun `decreaseOpacity moves by step until min`() {
        assertEquals(40, MaskOverlayModel.decreaseOpacity(50))
        assertEquals(20, MaskOverlayModel.decreaseOpacity(24))
    }

    @Test
    fun `preset detects supported quick values`() {
        assertTrue(MaskOverlayModel.isPreset(50))
        assertFalse(MaskOverlayModel.isPreset(57))
    }
}

