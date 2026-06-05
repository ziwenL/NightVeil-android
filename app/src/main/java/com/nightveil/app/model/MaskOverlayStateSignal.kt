package com.nightveil.app.model

import android.content.Intent

object MaskOverlayStateSignal {
    const val ACTION_STATE_CHANGED = "com.nightveil.app.overlay.STATE_CHANGED"

    fun intent(packageName: String): Intent {
        return Intent(ACTION_STATE_CHANGED).setPackage(packageName)
    }
}
