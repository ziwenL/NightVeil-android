package com.nightveil.app

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.databinding.WidgetMaskViewBinding
import com.nightveil.app.model.MaskOverlayStateSignal
import com.nightveil.app.model.MaskOverlayModel
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.notification.MaskNotificationFactory
import com.nightveil.app.overlay.MaskOverlayController
import com.nightveil.app.utils.FloatingWindowHelper
import com.nightveil.app.model.MaskTimerModel
import com.nightveil.app.timer.MaskSleepTimer
import com.nightveil.app.tile.MaskTileService

class FloatingService : Service() {

    private lateinit var floatingWindowHelper: FloatingWindowHelper
    private lateinit var binding: WidgetMaskViewBinding
    private lateinit var preferences: MaskPreferences
    private lateinit var overlayController: MaskOverlayController
    private var active = false
    private val handler = Handler(Looper.getMainLooper())
    private val timerTick = object : Runnable {
        override fun run() {
            val deadline = preferences.timerDeadline
            if (!active || deadline == 0L) return
            if (MaskTimerModel.hasExpired(deadline, SystemClock.elapsedRealtime())) {
                stopOverlay()
            } else {
                updateNotification()
                broadcastStateChanged()
                handler.postDelayed(this, minOf(60_000L, deadline - SystemClock.elapsedRealtime()).coerceAtLeast(1L))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = MaskPreferences(this)
        floatingWindowHelper = FloatingWindowHelper(this)
        binding = WidgetMaskViewBinding.inflate(LayoutInflater.from(this), null, false)
        overlayController = MaskOverlayController(binding)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null && (!preferences.isRunning ||
                MaskTimerModel.hasExpired(preferences.timerDeadline, SystemClock.elapsedRealtime()))) {
            stopSelf()
            return START_NOT_STICKY
        }
        val requestedOpacity = intent?.getIntExtra(
            MaskServiceAction.EXTRA_OPACITY,
            preferences.lastOpacity
        ) ?: preferences.lastOpacity

        when (action) {
            MaskServiceAction.START.action -> startOverlay(requestedOpacity)
            MaskServiceAction.STOP.action -> stopOverlay()
            MaskServiceAction.SET_OPACITY.action -> setOpacity(requestedOpacity)
            MaskServiceAction.SET_TIMER.action -> {
                if (active) {
                    preferences.timerMinutes = intent.getIntExtra(MaskServiceAction.EXTRA_TIMER_MINUTES, 0)
                    restartTimer()
                    broadcastStateChanged()
                    updateNotification()
                } else stopSelf()
            }
            MaskServiceAction.INCREASE.action -> setOpacity(
                MaskOverlayModel.increaseOpacity(preferences.lastOpacity)
            )

            MaskServiceAction.DECREASE.action -> setOpacity(
                MaskOverlayModel.decreaseOpacity(preferences.lastOpacity)
            )

            else -> startOverlay(preferences.lastOpacity, restoreTimer = true)
        }

        return if (active) START_STICKY else START_NOT_STICKY
    }

    override fun onDestroy() {
        preferences.isRunning = false
        active = false
        handler.removeCallbacksAndMessages(null)
        MaskSleepTimer.cancel(this)
        overlayController.cancelAnimations()
        floatingWindowHelper.destroy()
        broadcastStateChanged()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOverlay(opacity: Int, restoreTimer: Boolean = false) {
        if (!FloatingWindowHelper.canDrawOverlays(this)) {
            preferences.isRunning = false
            broadcastStateChanged()
            stopSelf()
            return
        }

        val wasActive = active
        val safeOpacity = MaskOverlayModel.coerceOpacity(opacity, Build.VERSION.SDK_INT)
        preferences.lastOpacity = safeOpacity
        preferences.isRunning = true

        val notification = MaskNotificationFactory.build(this, safeOpacity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                MaskNotificationFactory.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                MaskNotificationFactory.NOTIFICATION_ID,
                notification
            )
        }
        if (!wasActive) {
            try {
                floatingWindowHelper.addOverlayView(binding.root)
            } catch (_: SecurityException) {
                stopSelf()
                return
            }
        }
        active = true
        floatingWindowHelper.updateOpacity(binding.root, safeOpacity)
        if (!wasActive) {
            if (restoreTimer && preferences.timerDeadline > 0L) {
                MaskSleepTimer.schedule(this, preferences.timerDeadline)
                handler.post(timerTick)
            } else restartTimer()
        }
        broadcastStateChanged()
        if (wasActive) overlayController.updateOpacity(safeOpacity) else overlayController.show(safeOpacity)
    }

    private fun setOpacity(opacity: Int) {
        // Ignore slider events queued after Stop; they must never turn the veil back on.
        if (!active || !preferences.isRunning || !FloatingWindowHelper.canDrawOverlays(this)) {
            if (!active) stopSelf()
            return
        }
        val safeOpacity = MaskOverlayModel.coerceOpacity(opacity, Build.VERSION.SDK_INT)
        preferences.lastOpacity = safeOpacity
        floatingWindowHelper.updateOpacity(binding.root, safeOpacity)
        overlayController.updateOpacity(safeOpacity)
        broadcastStateChanged()
        updateNotification()
    }

    private fun updateNotification() {
        val notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        )
        notificationManager?.notify(
            MaskNotificationFactory.NOTIFICATION_ID,
            MaskNotificationFactory.build(this, preferences.lastOpacity)
        )
    }

    private fun stopOverlay() {
        if (!active) {
            stopSelf()
            return
        }
        active = false
        preferences.isRunning = false
        handler.removeCallbacks(timerTick)
        MaskSleepTimer.cancel(this)
        broadcastStateChanged()
        // Remove immediately: a queued fade completion could otherwise stop a newly restarted veil.
        binding.viewMask.alpha = 0f
        overlayController.cancelAnimations()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun restartTimer() {
        handler.removeCallbacks(timerTick)
        preferences.timerDeadline = MaskTimerModel.deadline(preferences.timerMinutes, SystemClock.elapsedRealtime())
        MaskSleepTimer.schedule(this, preferences.timerDeadline)
        if (preferences.timerDeadline > 0L) handler.post(timerTick)
    }

    private fun broadcastStateChanged() {
        sendBroadcast(MaskOverlayStateSignal.intent(packageName))
        if (Build.VERSION.SDK_INT >= 24) MaskTileService.refresh(this)
    }

    companion object {
        fun createIntent(
            context: Context,
            action: MaskServiceAction,
            opacity: Int? = null
        ): Intent {
            return Intent(context, FloatingService::class.java).apply {
                this.action = action.action
                if (opacity != null) {
                    putExtra(MaskServiceAction.EXTRA_OPACITY, opacity)
                }
            }
        }
    }
}

