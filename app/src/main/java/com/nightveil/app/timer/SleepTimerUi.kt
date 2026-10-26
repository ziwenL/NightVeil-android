package com.nightveil.app.timer

import android.app.Activity
import android.os.SystemClock
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nightveil.app.FloatingService
import com.nightveil.app.R
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.model.MaskTimerModel
import android.content.Context

object SleepTimerUi {
    fun summary(context: Context): String {
        val preferences = MaskPreferences(context)
        return when {
            preferences.isRunning && preferences.timerDeadline > 0L -> context.getString(
                R.string.timer_remaining,
                MaskTimerModel.remainingMinutes(preferences.timerDeadline, SystemClock.elapsedRealtime())
            )
            preferences.timerMinutes > 0 -> context.getString(R.string.timer_on_start, preferences.timerMinutes)
            else -> context.getString(R.string.timer_off)
        }
    }

    fun show(activity: Activity, onChanged: () -> Unit): androidx.appcompat.app.AlertDialog {
        val preferences = MaskPreferences(activity)
        val binding = com.nightveil.app.databinding.DialogSleepTimerBinding.inflate(activity.layoutInflater)
        binding.tvTimerHint.setText(if (preferences.isRunning) R.string.timer_hint_running else R.string.timer_hint_stopped)
        val dialog = MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_NightVeil_TimerDialog)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        val options = listOf(binding.optionOff, binding.option15, binding.option30, binding.option60)
        options.zip(MaskTimerModel.OPTIONS).forEach { (button, minutes) ->
            val selected = preferences.timerMinutes == minutes
            button.isCheckable = true
            button.isChecked = selected
            button.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(activity,
                if (selected) R.color.mask_primary else R.color.mask_control)
            val foreground = androidx.core.content.ContextCompat.getColorStateList(activity,
                if (selected) R.color.mask_bg_start else R.color.mask_text_primary)
            button.setTextColor(foreground)
            button.iconTint = foreground
            button.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_END
            if (selected) button.icon = androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.ic_check)
            button.setOnClickListener {
                if (preferences.isRunning) {
                    activity.startService(FloatingService.createIntent(activity, MaskServiceAction.SET_TIMER)
                        .putExtra(MaskServiceAction.EXTRA_TIMER_MINUTES, minutes))
                } else {
                    preferences.timerMinutes = minutes
                    MaskSleepTimer.cancel(activity)
                }
                dialog.dismiss()
                onChanged()
            }
        }
        dialog.show()
        return dialog
    }
}
