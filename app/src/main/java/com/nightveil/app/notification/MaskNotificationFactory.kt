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
import com.nightveil.app.MainActivity
import com.nightveil.app.R
import com.nightveil.app.model.MaskServiceAction

object MaskNotificationFactory {
    const val CHANNEL_ID = "mask.overlay.foreground"
    const val NOTIFICATION_ID = 205
    const val REQUEST_DECREASE = 201
    const val REQUEST_INCREASE = 202
    const val REQUEST_STOP = 203
    private const val REQUEST_OPEN_APP = 204

    val notificationActions = listOf(
        MaskServiceAction.DECREASE.action,
        MaskServiceAction.INCREASE.action,
        MaskServiceAction.STOP.action,
    )

    fun build(context: Context, opacity: Int): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.opacity_value, opacity))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent(context))
            .addAction(
                0,
                context.getString(R.string.notification_decrease),
                actionPendingIntent(context, MaskServiceAction.DECREASE, REQUEST_DECREASE)
            )
            .addAction(
                0,
                context.getString(R.string.notification_increase),
                actionPendingIntent(context, MaskServiceAction.INCREASE, REQUEST_INCREASE)
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

