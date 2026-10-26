package com.nightveil.app.model

enum class MaskServiceAction(val action: String) {
    START("com.nightveil.app.action.START"),
    STOP("com.nightveil.app.action.STOP"),
    SET_OPACITY("com.nightveil.app.action.SET_OPACITY"),
    SET_TIMER("com.nightveil.app.action.SET_TIMER"),
    INCREASE("com.nightveil.app.action.INCREASE"),
    DECREASE("com.nightveil.app.action.DECREASE");

    companion object {
        const val EXTRA_OPACITY = "extra_opacity"
        const val EXTRA_TIMER_MINUTES = "extra_timer_minutes"
    }
}

