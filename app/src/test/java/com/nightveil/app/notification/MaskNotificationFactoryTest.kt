package com.nightveil.app.notification

import com.nightveil.app.model.MaskServiceAction
import org.junit.Assert.assertEquals
import org.junit.Test

class MaskNotificationFactoryTest {
    @Test
    fun `action request codes remain stable`() {
        assertEquals(205, MaskNotificationFactory.REQUEST_ADJUST)
        assertEquals(203, MaskNotificationFactory.REQUEST_STOP)
    }

    @Test
    fun `action order matches notification layout`() {
        assertEquals(MaskNotificationFactory.ACTION_ADJUST, MaskNotificationFactory.notificationActions[0])
        assertEquals(MaskServiceAction.STOP.action, MaskNotificationFactory.notificationActions[1])
        assertEquals(2, MaskNotificationFactory.notificationActions.size)
    }
}

