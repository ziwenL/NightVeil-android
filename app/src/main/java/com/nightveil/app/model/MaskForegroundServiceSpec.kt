package com.nightveil.app.model

import android.content.pm.ServiceInfo
import android.os.Build

object MaskForegroundServiceSpec {
    const val SPECIAL_USE_SUBTYPE = "screen_dimming_overlay_for_night_eye_comfort"

    val serviceType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
}

