package com.nightveil.app.data

import android.content.Context
import com.nightveil.app.model.MaskOverlayModel

class MaskPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    val maximumOpacity: Int get() = MaskOverlayModel.maximumOpacity(android.os.Build.VERSION.SDK_INT)

    var lastOpacity: Int
        get() = MaskOverlayModel.coerceOpacity(
            preferences.getInt(KEY_LAST_OPACITY, MaskOverlayModel.DEFAULT_OPACITY), android.os.Build.VERSION.SDK_INT
        )
        set(value) {
            preferences.edit()
                .putInt(KEY_LAST_OPACITY, MaskOverlayModel.coerceOpacity(value, android.os.Build.VERSION.SDK_INT))
                .apply()
        }

    var isRunning: Boolean
        get() = preferences.getBoolean(KEY_IS_RUNNING, false)
        set(value) {
            preferences.edit().putBoolean(KEY_IS_RUNNING, value).apply()
        }

    var hasRequestedPermission: Boolean
        get() = preferences.getBoolean(KEY_HAS_REQUESTED_PERMISSION, false)
        set(value) {
            preferences.edit().putBoolean(KEY_HAS_REQUESTED_PERMISSION, value).apply()
        }

    var timerMinutes: Int
        get() = preferences.getInt("timer_minutes", 0).takeIf { it in listOf(0, 15, 30, 60) } ?: 0
        set(value) { preferences.edit().putInt("timer_minutes", value).apply() }

    var timerDeadline: Long
        get() = preferences.getLong("timer_deadline_elapsed", 0L)
        set(value) { preferences.edit().putLong("timer_deadline_elapsed", value).apply() }

    companion object {
        const val FILE_NAME = "mask_overlay_prefs"
        const val KEY_LAST_OPACITY = "last_opacity"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_HAS_REQUESTED_PERMISSION = "has_requested_permission"
    }
}

