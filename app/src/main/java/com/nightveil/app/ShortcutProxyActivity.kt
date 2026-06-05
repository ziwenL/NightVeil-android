package com.nightveil.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.model.MaskNotificationPermissionModel
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.model.MaskShortcutAction
import com.nightveil.app.utils.FloatingWindowHelper

class ShortcutProxyActivity : ComponentActivity() {
    private lateinit var preferences: MaskPreferences
    private var pendingShortcutAction: MaskShortcutAction? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (pendingShortcutAction == MaskShortcutAction.START) {
                startOverlay()
            }
            pendingShortcutAction = null
            finishSilently()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = MaskPreferences(this)

        when (MaskShortcutAction.fromAction(intent?.action)) {
            MaskShortcutAction.START -> handleStartShortcut()
            MaskShortcutAction.STOP -> handleStopShortcut()
            null -> finishSilently()
        }
    }

    private fun handleStartShortcut() {
        if (!FloatingWindowHelper.canDrawOverlays(this)) {
            FloatingWindowHelper.canDrawOverlays(this, true)
            finishSilently()
            return
        }

        if (MaskNotificationPermissionModel.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION.SDK_INT,
                permissionGranted = isNotificationPermissionGranted()
            )
        ) {
            pendingShortcutAction = MaskShortcutAction.START
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        startOverlay()
        finishSilently()
    }

    private fun handleStopShortcut() {
        preferences.isRunning = false
        startMaskService(MaskServiceAction.STOP)
        finishSilently()
    }

    private fun startOverlay() {
        preferences.isRunning = true
        startMaskService(MaskServiceAction.START, preferences.lastOpacity)
    }

    private fun startMaskService(action: MaskServiceAction, opacity: Int? = null) {
        val intent = FloatingService.createIntent(this, action, opacity)
        if (action == MaskServiceAction.START) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun finishSilently() {
        finish()
        overridePendingTransition(0, 0)
    }
}
