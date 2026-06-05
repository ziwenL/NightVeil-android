package com.nightveil.app.data

import com.nightveil.app.model.MaskServiceAction
import org.junit.Assert.assertEquals
import org.junit.Test

class MaskPreferencesKeyTest {

    @Test
    fun `preference keys stay stable`() {
        assertEquals("mask_overlay_prefs", MaskPreferences.FILE_NAME)
        assertEquals("last_opacity", MaskPreferences.KEY_LAST_OPACITY)
        assertEquals("is_running", MaskPreferences.KEY_IS_RUNNING)
        assertEquals("has_requested_permission", MaskPreferences.KEY_HAS_REQUESTED_PERMISSION)
    }

    @Test
    fun `service action names stay stable`() {
        assertEquals("com.nightveil.app.action.START", MaskServiceAction.START.action)
        assertEquals("com.nightveil.app.action.STOP", MaskServiceAction.STOP.action)
        assertEquals("com.nightveil.app.action.SET_OPACITY", MaskServiceAction.SET_OPACITY.action)
        assertEquals("com.nightveil.app.action.INCREASE", MaskServiceAction.INCREASE.action)
        assertEquals("com.nightveil.app.action.DECREASE", MaskServiceAction.DECREASE.action)
    }
}

