package com.nightveil.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.nightveil.app.FloatingService
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.model.MaskOverlayStateSignal
import com.nightveil.app.model.MaskTimerModel
import com.nightveil.app.tile.MaskTileService

class SleepTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val preferences = MaskPreferences(context)
        // Ignore an old alarm already dispatched when the user resets or cancels the timer.
        if (!MaskTimerModel.hasExpired(preferences.timerDeadline, SystemClock.elapsedRealtime())) return
        preferences.isRunning = false
        MaskSleepTimer.cancel(context)
        context.stopService(Intent(context, FloatingService::class.java))
        context.sendBroadcast(MaskOverlayStateSignal.intent(context.packageName))
        if (android.os.Build.VERSION.SDK_INT >= 24) MaskTileService.refresh(context)
    }
}
