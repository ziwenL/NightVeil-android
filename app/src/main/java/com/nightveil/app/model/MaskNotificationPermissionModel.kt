package com.nightveil.app.model

object MaskNotificationPermissionModel {
    fun shouldRequestNotificationPermission(
        sdkInt: Int,
        permissionGranted: Boolean
    ): Boolean {
        return sdkInt >= 33 && !permissionGranted
    }
}

