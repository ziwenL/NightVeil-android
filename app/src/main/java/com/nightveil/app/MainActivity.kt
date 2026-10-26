package com.nightveil.app

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.content.ComponentName
import android.app.StatusBarManager
import android.graphics.drawable.Icon
import com.nightveil.app.timer.SleepTimerUi
import com.nightveil.app.tile.MaskTileService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.databinding.ActivityMainBinding
import com.nightveil.app.model.MaskHomeUiModel
import com.nightveil.app.model.MaskOverlayStateSignal
import com.nightveil.app.model.MaskNotificationPermissionModel
import com.nightveil.app.model.MaskOverlayModel
import com.nightveil.app.model.MaskOverlayRuntimeStateModel
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.utils.EdgeToEdgeInsetsHelper
import com.nightveil.app.utils.FloatingWindowHelper
import com.google.android.material.slider.Slider

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: MaskPreferences
    private lateinit var basePadding: EdgeToEdgeInsetsHelper.PaddingState
    private var pendingStartAfterNotificationPermission = false
    private var overlayStateReceiverRegistered = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private val refreshTimer = object : Runnable {
        override fun run() {
            binding.btnTimer.text = SleepTimerUi.summary(this@MainActivity)
            timerHandler.postDelayed(this, 30_000L)
        }
    }
    private val overlayStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MaskOverlayStateSignal.ACTION_STATE_CHANGED) {
                refreshUi()
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (pendingStartAfterNotificationPermission) {
                pendingStartAfterNotificationPermission = false
                if (!granted) {
                    toast(R.string.toast_notification_permission_denied)
                }
                preferences.isRunning = true
                refreshUi()
                startMaskService(MaskServiceAction.START, preferences.lastOpacity)
                toast(R.string.toast_overlay_started)
            }
        }

    private val presetViews: List<Pair<TextView, Int>>
        get() = listOf(
            binding.chipPreset30 to 30,
            binding.chipPreset50 to 50,
            binding.chipPreset65 to 65,
            binding.chipPreset80 to 80,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        preferences = MaskPreferences(this)
        synchronizePersistedRunningState()
        setContentView(binding.root)

        basePadding = EdgeToEdgeInsetsHelper.PaddingState(
            left = binding.contentRoot.paddingLeft,
            top = binding.contentRoot.paddingTop,
            right = binding.contentRoot.paddingRight,
            bottom = binding.contentRoot.paddingBottom
        )

        applyWindowInsets()
        setupSlider()
        setupPresetClicks()
        setupPrimaryButton()
        binding.btnTimer.setOnClickListener {
            SleepTimerUi.show(this) { binding.btnTimer.text = SleepTimerUi.summary(this) }
        }
        setupQuickTile()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onStart() {
        super.onStart()
        synchronizePersistedRunningState()
        registerOverlayStateReceiver()
        refreshUi()
        timerHandler.post(refreshTimer)
    }

    override fun onStop() {
        timerHandler.removeCallbacks(refreshTimer)
        unregisterOverlayStateReceiver()
        super.onStop()
    }

    private fun setupPrimaryButton() {
        binding.btnShow.setOnClickListener {
            when (resolveHomeState().primaryAction) {
                MaskHomeUiModel.PrimaryAction.START -> {
                    startOverlayFlow()
                }

                MaskHomeUiModel.PrimaryAction.STOP -> {
                    preferences.isRunning = false
                    refreshUi()
                    startMaskService(MaskServiceAction.STOP)
                    toast(R.string.toast_overlay_stopped)
                }

                MaskHomeUiModel.PrimaryAction.GRANT_PERMISSION -> {
                    preferences.hasRequestedPermission = true
                    FloatingWindowHelper.canDrawOverlays(this@MainActivity, true)
                    refreshUi()
                    toast(R.string.toast_permission_required)
                }
            }
        }
    }

    private fun setupSlider() {
        binding.sbTransparency.valueFrom = MaskOverlayModel.MIN_OPACITY.toFloat()
        binding.sbTransparency.valueTo = preferences.maximumOpacity.toFloat()
        binding.sbTransparency.value = preferences.lastOpacity.toFloat()
        binding.sbTransparency.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
            val opacity = MaskOverlayModel.coerceOpacity(value.toInt())
            if (preferences.lastOpacity != opacity) {
                preferences.lastOpacity = opacity
                binding.tvOpacityValue.text = getString(R.string.opacity_value, opacity)
                binding.tvLastUsed.text = getString(R.string.last_used_value, opacity)
                renderPresets(opacity)
                if (preferences.isRunning && fromUser) {
                    startMaskService(MaskServiceAction.SET_OPACITY, opacity)
                }
            }
        }
    }

    private fun configureEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, window.decorView)?.let { controller ->
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentRoot) { view, windowInsets ->
            val systemInsets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val merged = EdgeToEdgeInsetsHelper.merge(
                base = basePadding,
                insets = systemInsets.toState()
            )
            view.updatePadding(
                left = merged.left,
                top = merged.top,
                right = merged.right,
                bottom = merged.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(binding.contentRoot)
    }

    private fun setupPresetClicks() {
        presetViews.forEach { (view, value) ->
            view.setOnClickListener {
                preferences.lastOpacity = value
                binding.sbTransparency.value = value.toFloat()
                binding.tvOpacityValue.text = getString(R.string.opacity_value, value)
                binding.tvLastUsed.text = getString(R.string.last_used_value, value)
                renderPresets(value)
                if (preferences.isRunning) {
                    startMaskService(MaskServiceAction.SET_OPACITY, value)
                }
            }
        }
    }

    private fun refreshUi() {
        val opacity = preferences.lastOpacity
        val overlayPermissionGranted = FloatingWindowHelper.canDrawOverlays(this)
        val notificationPermissionGranted = isNotificationPermissionGranted()
        val homeState = MaskHomeUiModel.resolve(
            permissionGranted = overlayPermissionGranted,
            isRunning = preferences.isRunning
        )

        binding.tvOpacityValue.text = getString(R.string.opacity_value, opacity)
        binding.tvLastUsed.text = getString(R.string.last_used_value, opacity)
        binding.tvPermissionState.text =
            getString(if (overlayPermissionGranted) R.string.permission_ready else R.string.permission_missing)
        binding.tvNotificationState.text =
            getString(
                if (notificationPermissionGranted) {
                    R.string.notification_ready
                } else {
                    R.string.notification_missing
                }
            )
        binding.sbTransparency.value = opacity.toFloat()
        binding.btnTimer.text = SleepTimerUi.summary(this)
        renderPresets(opacity)
        renderHomeState(homeState)
    }

    private fun renderPresets(opacity: Int) {
        presetViews.forEach { (view, value) ->
            val selected = value == opacity
            view.isSelected = selected
            view.setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_default)
            view.setTextColor(ContextCompat.getColor(this,
                if (selected) R.color.mask_bg_start else R.color.mask_text_secondary))
        }
    }

    private fun synchronizePersistedRunningState() {
        if (MaskOverlayRuntimeStateModel.shouldResetPersistedRunningState(
                persistedIsRunning = preferences.isRunning,
                serviceIsRunning = isFloatingServiceRunning()
            )
        ) {
            preferences.isRunning = false
        }
    }

    private fun renderHomeState(state: MaskHomeUiModel.State) {
        when (state.overlayState) {
            MaskHomeUiModel.OverlayState.RUNNING -> {
                binding.tvOverlayState.text = getString(R.string.overlay_running)
                binding.tvOverlayState.setTextColor(ContextCompat.getColor(this, R.color.mask_success))
            }

            MaskHomeUiModel.OverlayState.STOPPED -> {
                binding.tvOverlayState.text = getString(R.string.overlay_stopped)
                binding.tvOverlayState.setTextColor(ContextCompat.getColor(this, R.color.mask_text_secondary))
            }
        }

        when (state.primaryAction) {
            MaskHomeUiModel.PrimaryAction.START -> {
                binding.btnShow.text = getString(R.string.start_overlay)
                binding.btnShow.setTextColor(ContextCompat.getColor(this, R.color.mask_bg_start))
            }

            MaskHomeUiModel.PrimaryAction.STOP -> {
                binding.btnShow.text = getString(R.string.stop_overlay)
                binding.btnShow.setTextColor(ContextCompat.getColor(this, R.color.mask_bg_start))
            }

            MaskHomeUiModel.PrimaryAction.GRANT_PERMISSION -> {
                binding.btnShow.text = getString(R.string.grant_and_start)
                binding.btnShow.setTextColor(ContextCompat.getColor(this, R.color.mask_bg_start))
            }
        }
        val stopping = state.primaryAction == MaskHomeUiModel.PrimaryAction.STOP
        binding.btnShow.backgroundTintList = ContextCompat.getColorStateList(this,
            if (stopping) R.color.mask_control else R.color.mask_primary)
        val foreground = ContextCompat.getColorStateList(this,
            if (stopping) R.color.mask_text_primary else R.color.mask_bg_start)
        binding.btnShow.setTextColor(foreground)
        binding.btnShow.iconTint = foreground
    }

    private fun resolveHomeState(): MaskHomeUiModel.State {
        return MaskHomeUiModel.resolve(
            permissionGranted = FloatingWindowHelper.canDrawOverlays(this),
            isRunning = preferences.isRunning
        )
    }

    private fun startMaskService(action: MaskServiceAction, opacity: Int? = null) {
        val intent = FloatingService.createIntent(this, action, opacity)
        if (action == MaskServiceAction.START) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show()
    }

    private fun setupQuickTile() {
        if (Build.VERSION.SDK_INT < 24) {
            binding.btnAddTile.visibility = View.GONE
            return
        }
        binding.btnAddTile.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                getSystemService(StatusBarManager::class.java).requestAddTileService(
                    ComponentName(this, MaskTileService::class.java),
                    getString(R.string.app_name), Icon.createWithResource(this, R.drawable.ic_tile_veil),
                    mainExecutor
                ) { result ->
                    if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                        result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                        toast(R.string.tile_added)
                    }
                }
            } else {
                MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_NightVeil_TimerDialog)
                    .setTitle(R.string.tile_add)
                    .setMessage(R.string.tile_add_instructions)
                    .setPositiveButton(android.R.string.ok, null).show()
            }
        }
    }

    private fun startOverlayFlow() {
        if (MaskNotificationPermissionModel.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION.SDK_INT,
                permissionGranted = isNotificationPermissionGranted()
            )
        ) {
            pendingStartAfterNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        preferences.isRunning = true
        refreshUi()
        startMaskService(MaskServiceAction.START, preferences.lastOpacity)
        toast(R.string.toast_overlay_started)
    }

    private fun isFloatingServiceRunning(): Boolean {
        val activityManager = ContextCompat.getSystemService(this, ActivityManager::class.java)
            ?: return false

        @Suppress("DEPRECATION")
        return activityManager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == FloatingService::class.java.name }
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

    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag") // Flags overload is guarded by API 33 below.
    private fun registerOverlayStateReceiver() {
        if (overlayStateReceiverRegistered) return

        val filter = IntentFilter(MaskOverlayStateSignal.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(overlayStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(overlayStateReceiver, filter)
        }
        overlayStateReceiverRegistered = true
    }

    private fun unregisterOverlayStateReceiver() {
        if (!overlayStateReceiverRegistered) return
        unregisterReceiver(overlayStateReceiver)
        overlayStateReceiverRegistered = false
    }

    private fun Insets.toState(): EdgeToEdgeInsetsHelper.InsetsState {
        return EdgeToEdgeInsetsHelper.InsetsState(
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }
}

