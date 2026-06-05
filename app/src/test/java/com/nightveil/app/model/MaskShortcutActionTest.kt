package com.nightveil.app.model

import com.nightveil.app.ShortcutProxyActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaskShortcutActionTest {

    @Test
    fun `shortcut actions stay stable`() {
        assertEquals("com.nightveil.app.shortcut.START", MaskShortcutAction.START.action)
        assertEquals("com.nightveil.app.shortcut.STOP", MaskShortcutAction.STOP.action)
    }

    @Test
    fun `from action resolves known shortcut intents`() {
        assertEquals(
            MaskShortcutAction.START,
            MaskShortcutAction.fromAction("com.nightveil.app.shortcut.START")
        )
        assertEquals(
            MaskShortcutAction.STOP,
            MaskShortcutAction.fromAction("com.nightveil.app.shortcut.STOP")
        )
        assertNull(MaskShortcutAction.fromAction("android.intent.action.MAIN"))
    }

    @Test
    fun `shortcut proxy activity name stays stable`() {
        assertEquals("com.nightveil.app.ShortcutProxyActivity", ShortcutProxyActivity::class.java.name)
    }
}
