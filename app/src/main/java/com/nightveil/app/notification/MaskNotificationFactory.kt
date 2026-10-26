package com.nightveil.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nightveil.app.FloatingService
import com.nightveil.app.BrightnessPanelActivity
import com.nightveil.app.MainActivity
import com.nightveil.app.R
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.model.MaskTimerModel
import android.os.SystemClock

object MaskNotificationFactory {
    const val CHANNEL_ID = "mask.overlay.foreground"
    const val NOTIFICATION_ID = 205
    const val REQUEST_ADJUST = 205
    const val ACTION_ADJUST = "com.nightveil.app.action.ADJUST"
    const val REQUEST_STOP = 203
    private const val REQUEST_OPEN_APP = 204

    val notificationActions = listOf(
        ACTION_ADJUST,
        MaskServiceAction.STOP.action,
    )

    fun build(context: Context, opacity: Int): Notification {
        ensureChannel(context)
        val deadline = MaskPreferences(context).timerDeadline
        val content = if (deadline > 0L) context.getString(
            R.string.notification_with_timer, opacity,
            MaskTimerModel.remainingMinutes(deadline, SystemClock.elapsedRealtime())
        ) else context.getString(R.string.last_used_value, opacity)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_veil)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent(context))
            .addAction(
                0,
                context.getString(R.string.notification_adjust),
                PendingIntent.getActivity(
                    context,
                    REQUEST_ADJUST,
                    Intent(context, BrightnessPanelActivity::class.java).apply {
                        action = ACTION_ADJUST
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0,
                context.getString(R.string.notification_close),
                actionPendingIntent(context, MaskServiceAction.STOP, REQUEST_STOP)
            )
            .build()
    }

    private fun actionPendingIntent(
        context: Context,
        action: MaskServiceAction,
        requestCode: Int
    ): PendingIntent {
        val intent = FloatingService.createIntent(context, action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }
}

