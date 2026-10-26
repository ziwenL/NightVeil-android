package com.nightveil.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.notification.MaskNotificationFactory
import com.nightveil.app.timer.MaskSleepTimer
import com.nightveil.app.timer.SleepTimerReceiver
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NightVeilVersionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = MaskPreferences(context)

    @Before fun requireOverlayPermission() {
        // The device fixture grants overlay access before the run; no shell pipe on the test thread.
        org.junit.Assume.assumeTrue(com.nightveil.app.utils.FloatingWindowHelper.canDrawOverlays(context))
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(500L)
    }

    private fun await(message: String, condition: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 5_000L
        while (!condition() && SystemClock.elapsedRealtime() < end) Thread.sleep(50)
        assertTrue(message, condition())
    }

    @Test fun timerSelectionCancellationAndAlarmExpiry() {
        val previousMinutes = preferences.timerMinutes
        context.stopService(Intent(context, FloatingService::class.java))
        try {
            run {
                preferences.timerMinutes = 15
                assertEquals(15, preferences.timerMinutes)
                context.startService(FloatingService.createIntent(context, MaskServiceAction.START))
                await("Timer should start with the veil") { preferences.isRunning && preferences.timerDeadline > 0L }

                context.startService(FloatingService.createIntent(context, MaskServiceAction.SET_TIMER)
                    .putExtra(MaskServiceAction.EXTRA_TIMER_MINUTES, 30))
                await("Changing timer should restart the deadline") {
                    preferences.timerMinutes == 30 && preferences.timerDeadline - SystemClock.elapsedRealtime() > 1_790_000L
                }
                context.startService(FloatingService.createIntent(context, MaskServiceAction.SET_TIMER)
                    .putExtra(MaskServiceAction.EXTRA_TIMER_MINUTES, 0))
                await("Cancelling the timer should keep the veil running") { preferences.timerDeadline == 0L && preferences.isRunning }

                // A delayed broadcast from the cancelled timer must not switch off the veil.
                instrumentation.runOnMainSync { SleepTimerReceiver().onReceive(context, Intent()) }
                assertTrue(preferences.isRunning)

                // Deliver the expiry broadcast deterministically, independent of OS alarm batching.
                preferences.timerDeadline = SystemClock.elapsedRealtime() - 1L
                instrumentation.runOnMainSync { SleepTimerReceiver().onReceive(context, Intent()) }
                await("Expiry receiver should stop the veil") { !preferences.isRunning && preferences.timerDeadline == 0L }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                await("Expired timer should remove the foreground notification") {
                    manager.activeNotifications.none { it.id == MaskNotificationFactory.NOTIFICATION_ID }
                }
            }
        } finally {
            context.stopService(Intent(context, FloatingService::class.java))
            MaskSleepTimer.cancel(context)
            preferences.timerMinutes = previousMinutes
        }
    }

    @Test fun notificationOpensStandalonePanelAndStopRejectsLateSliderEvents() {
        val previousMinutes = preferences.timerMinutes
        preferences.timerMinutes = 0
        try {
            run {
                context.startService(FloatingService.createIntent(context, MaskServiceAction.START))
                await("Veil should start") { preferences.isRunning }
                val monitor = instrumentation.addMonitor(BrightnessPanelActivity::class.java.name, null, false)
                try {
                    MaskNotificationFactory.build(context, preferences.lastOpacity).actions[0].actionIntent.send()
                    val panel = monitor.waitForActivityWithTimeout(3_000L)
                    assertNotNull("Notification action should open the adjustment panel", panel)
                    instrumentation.runOnMainSync {
                        assertNotNull(panel.findViewById<android.view.View>(R.id.sliderIntensity))
                        assertNotNull(panel.findViewById<android.view.View>(R.id.btn_timer))
                    }
                    instrumentation.runOnMainSync { panel.findViewById<android.view.View>(R.id.btnDone).performClick() }
                } finally { instrumentation.removeMonitor(monitor) }

                context.startService(FloatingService.createIntent(context, MaskServiceAction.STOP))
                context.startService(FloatingService.createIntent(context, MaskServiceAction.SET_OPACITY, 80))
                await("Stop should keep the veil off despite a queued slider update") { !preferences.isRunning }
                assertEquals(0L, preferences.timerDeadline)
            }
        } finally {
            context.stopService(Intent(context, FloatingService::class.java))
            preferences.timerMinutes = previousMinutes
        }
    }
}
