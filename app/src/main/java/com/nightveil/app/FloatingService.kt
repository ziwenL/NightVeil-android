package com.nightveil.app

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.databinding.WidgetMaskViewBinding
import com.nightveil.app.model.MaskForegroundServiceSpec
import com.nightveil.app.model.MaskOverlayModel
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.notification.MaskNotificationFactory
import com.nightveil.app.overlay.MaskOverlayController
import com.nightveil.app.utils.FloatingWindowHelper

class FloatingService : Service() {

    private lateinit var floatingWindowHelper: FloatingWindowHelper
    private lateinit var binding: WidgetMaskViewBinding
    private lateinit var preferences: MaskPreferences
    private lateinit var overlayController: MaskOverlayController

    override fun onCreate() {
        super.onCreate()
        preferences = MaskPreferences(this)
        floatingWindowHelper = FloatingWindowHelper(this)
        binding = WidgetMaskViewBinding.inflate(LayoutInflater.from(this), null, false)
        overlayController = MaskOverlayController(binding)
        floatingWindowHelper.addOverlayView(binding.root)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val requestedOpacity = intent?.getIntExtra(
            MaskServiceAction.EXTRA_OPACITY,
            preferences.lastOpacity
        ) ?: preferences.lastOpacity

        when (action) {
            MaskServiceAction.START.action -> startOverlay(requestedOpacity)
            MaskServiceAction.STOP.action -> stopOverlay()
            MaskServiceAction.SET_OPACITY.action -> setOpacity(requestedOpacity)
            MaskServiceAction.INCREASE.action -> setOpacity(
                MaskOverlayModel.increaseOpacity(preferences.lastOpacity)
            )

            MaskServiceAction.DECREASE.action -> setOpacity(
                MaskOverlayModel.decreaseOpacity(preferences.lastOpacity)
            )

            else -> startOverlay(preferences.lastOpacity)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        preferences.isRunning = false
        floatingWindowHelper.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOverlay(opacity: Int) {
        if (!FloatingWindowHelper.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val safeOpacity = MaskOverlayModel.coerceOpacity(opacity)
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
        overlayController.show(safeOpacity)
    }

    private fun setOpacity(opacity: Int) {
        val safeOpacity = MaskOverlayModel.coerceOpacity(opacity)
        preferences.lastOpacity = safeOpacity
        preferences.isRunning = true
        overlayController.updateOpacity(safeOpacity)
        val notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        )
        notificationManager?.notify(
            MaskNotificationFactory.NOTIFICATION_ID,
            MaskNotificationFactory.build(this, safeOpacity)
        )
    }

    private fun stopOverlay() {
        preferences.isRunning = false
        overlayController.hide {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
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

