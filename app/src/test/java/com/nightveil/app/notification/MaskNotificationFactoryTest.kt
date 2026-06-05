package com.nightveil.app.notification

import com.nightveil.app.model.MaskServiceAction
import org.junit.Assert.assertEquals
import org.junit.Test

class MaskNotificationFactoryTest {
    @Test
    fun `action request codes remain stable`() {
        assertEquals(201, MaskNotificationFactory.REQUEST_DECREASE)
        assertEquals(202, MaskNotificationFactory.REQUEST_INCREASE)
        assertEquals(203, MaskNotificationFactory.REQUEST_STOP)
    }

    @Test
    fun `action order matches notification layout`() {
        assertEquals(MaskServiceAction.DECREASE.action, MaskNotificationFactory.notificationActions[0])
        assertEquals(MaskServiceAction.INCREASE.action, MaskNotificationFactory.notificationActions[1])
        assertEquals(MaskServiceAction.STOP.action, MaskNotificationFactory.notificationActions[2])
        assertEquals(3, MaskNotificationFactory.notificationActions.size)
    }
}

