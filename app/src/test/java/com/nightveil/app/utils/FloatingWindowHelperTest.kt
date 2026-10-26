package com.nightveil.app.utils

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingWindowHelperTest {

    @Test
    fun `overlay layout config includes full screen coverage flags`() {
        val config = FloatingWindowHelper.createOverlayLayoutConfig()

        assertEquals(WindowManager.LayoutParams.MATCH_PARENT, config.width)
        assertEquals(WindowManager.LayoutParams.MATCH_PARENT, config.height)
        assertEquals(Gravity.START or Gravity.TOP, config.gravity)
        assertEquals(PixelFormat.TRANSLUCENT, config.format)
        assertTrue(config.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
        assertTrue(config.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
        assertTrue(config.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0)
        assertTrue(config.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS != 0)
        assertTrue(config.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0)
    }
}

