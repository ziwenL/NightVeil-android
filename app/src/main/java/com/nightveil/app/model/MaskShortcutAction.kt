package com.nightveil.app.model

enum class MaskShortcutAction(val action: String) {
    START("com.nightveil.app.shortcut.START"),
    STOP("com.nightveil.app.shortcut.STOP");

    companion object {
        fun fromAction(action: String?): MaskShortcutAction? {
            return entries.firstOrNull { it.action == action }
        }
    }
}
