package com.nightveil.app.model

enum class MaskServiceAction(val action: String) {
    START("com.nightveil.app.action.START"),
    STOP("com.nightveil.app.action.STOP"),
    SET_OPACITY("com.nightveil.app.action.SET_OPACITY"),
    INCREASE("com.nightveil.app.action.INCREASE"),
    DECREASE("com.nightveil.app.action.DECREASE");

    companion object {
        const val EXTRA_OPACITY = "extra_opacity"
    }
}

