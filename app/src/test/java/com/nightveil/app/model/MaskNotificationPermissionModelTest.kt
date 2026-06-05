package com.nightveil.app.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskNotificationPermissionModelTest {

    @Test
    fun `android 13 plus without permission should request`() {
        assertTrue(
            MaskNotificationPermissionModel.shouldRequestNotificationPermission(
                sdkInt = 33,
                permissionGranted = false
            )
        )
    }

    @Test
    fun `android 13 plus with permission should not request`() {
        assertFalse(
            MaskNotificationPermissionModel.shouldRequestNotificationPermission(
                sdkInt = 33,
                permissionGranted = true
            )
        )
    }

    @Test
    fun `below android 13 should not request`() {
        assertFalse(
            MaskNotificationPermissionModel.shouldRequestNotificationPermission(
                sdkInt = 32,
                permissionGranted = false
            )
        )
    }
}

