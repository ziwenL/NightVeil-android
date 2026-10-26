package com.nightveil.app.model

object MaskOverlayModel {
    const val MIN_OPACITY = 20
    const val MAX_OPACITY = 90
    const val DEFAULT_OPACITY = 50
    const val STEP = 10

    val PRESETS = listOf(30, 50, 65, 80)

    // Android 12+ blocks touches through application overlays above 80% window opacity.
    fun maximumOpacity(sdkInt: Int): Int = if (sdkInt >= 31) 80 else MAX_OPACITY

    fun coerceOpacity(value: Int, sdkInt: Int = 0): Int = value.coerceIn(MIN_OPACITY, maximumOpacity(sdkInt))

    fun increaseOpacity(value: Int): Int = coerceOpacity(value + STEP)

    fun decreaseOpacity(value: Int): Int = coerceOpacity(value - STEP)

    fun isPreset(value: Int): Boolean = PRESETS.contains(value)
}

