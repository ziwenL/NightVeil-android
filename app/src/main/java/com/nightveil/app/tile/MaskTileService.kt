package com.nightveil.app.tile

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.nightveil.app.FloatingService
import com.nightveil.app.MainActivity
import com.nightveil.app.R
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.utils.FloatingWindowHelper

@RequiresApi(24)
class MaskTileService : TileService() {
    private var lastClickAt = 0L
    private lateinit var statePreferences: SharedPreferences
    private val stateListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == MaskPreferences.KEY_IS_RUNNING) updateTile()
    }

    override fun onCreate() {
        super.onCreate()
        statePreferences = getSharedPreferences(MaskPreferences.FILE_NAME, Context.MODE_PRIVATE)
        statePreferences.registerOnSharedPreferenceChangeListener(stateListener)
    }

    override fun onDestroy() {
        statePreferences.unregisterOnSharedPreferenceChangeListener(stateListener)
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val now = android.os.SystemClock.elapsedRealtime()
        // Some control centers dispatch the same tap twice. Treat it as one toggle.
        if (now - lastClickAt < 350L) return
        lastClickAt = now
        if (isLocked) {
            unlockAndRun { toggle() }
        } else {
            toggle()
        }
    }

    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated") // Intent overload is required below API 34.
    private fun toggle() {
        val preferences = MaskPreferences(this)
        // Stopping must remain available even if notification permission was revoked while running.
        if (preferences.isRunning) {
            preferences.isRunning = false
            com.nightveil.app.timer.MaskSleepTimer.cancel(this)
            stopService(Intent(this, FloatingService::class.java))
            sendBroadcast(com.nightveil.app.model.MaskOverlayStateSignal.intent(packageName))
            updateTile()
            return
        }
        if (!FloatingWindowHelper.canDrawOverlays(this) ||
            (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED)
        ) {
            // Keep permission education in the app, where the user can act on it.
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(
                    this, 302, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        ContextCompat.startForegroundService(this, FloatingService.createIntent(
            this, MaskServiceAction.START, preferences.lastOpacity
        ))
    }

    private fun updateTile() {
        qsTile?.apply {
            val running = MaskPreferences(this@MaskTileService).isRunning &&
                FloatingWindowHelper.canDrawOverlays(this@MaskTileService)
            state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(this@MaskTileService, R.drawable.ic_tile_veil)
            label = getString(R.string.app_name)
            contentDescription = getString(if (running) R.string.stop_overlay else R.string.start_overlay)
            if (Build.VERSION.SDK_INT >= 29) {
                subtitle = getString(if (running) R.string.overlay_running else R.string.overlay_stopped)
            }
            updateTile()
        }
    }

    companion object {
        fun refresh(context: Context) {
            if (Build.VERSION.SDK_INT >= 24) {
                requestListeningState(context, ComponentName(context, MaskTileService::class.java))
            }
        }
    }
}
