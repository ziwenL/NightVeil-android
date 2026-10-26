package com.nightveil.app.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nightveil.app.data.MaskPreferences

object MaskSleepTimer {
    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, 301, Intent(context, SleepTimerReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(context: Context, deadline: Long) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(alarmIntent(context))
        if (deadline <= 0L) return
        // No additional permission prompt: use the best scheduling precision available.
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            if (Build.VERSION.SDK_INT >= 23) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, alarmIntent(context))
            } else {
                alarms.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, alarmIntent(context))
            }
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, deadline, alarmIntent(context))
        }
    }

    fun cancel(context: Context) {
        MaskPreferences(context).timerDeadline = 0L
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(alarmIntent(context))
    }
}
