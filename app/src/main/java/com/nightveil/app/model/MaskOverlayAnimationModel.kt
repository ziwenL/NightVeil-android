package com.nightveil.app.model

import android.os.Build
import kotlin.math.hypot

object MaskOverlayAnimationModel {
    const val SHOW_DURATION_MS = 280L
    const val HIDE_DURATION_MS = 240L
    const val FADE_DURATION_MS = 180L

    fun shouldUseCircularReveal(
        sdkInt: Int,
        width: Int,
        height: Int
    ): Boolean {
        return sdkInt >= Build.VERSION_CODES.LOLLIPOP && width > 0 && height > 0
    }

    fun finalRadius(width: Int, height: Int): Float {
        return hypot((width / 2.0), (height / 2.0)).toFloat()
    }
}
