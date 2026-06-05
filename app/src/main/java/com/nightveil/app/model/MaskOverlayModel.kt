package com.nightveil.app.model

object MaskOverlayModel {
    const val MIN_OPACITY = 20
    const val MAX_OPACITY = 90
    const val DEFAULT_OPACITY = 50
    const val STEP = 10

    val PRESETS = listOf(30, 50, 65, 80)

    fun coerceOpacity(value: Int): Int = value.coerceIn(MIN_OPACITY, MAX_OPACITY)

    fun increaseOpacity(value: Int): Int = coerceOpacity(value + STEP)

    fun decreaseOpacity(value: Int): Int = coerceOpacity(value - STEP)

    fun isPreset(value: Int): Boolean = PRESETS.contains(value)
}

