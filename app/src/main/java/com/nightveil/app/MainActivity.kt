package com.nightveil.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onStart() {
        super.onStart()
        registerOverlayStateReceiver()
        refreshUi()
    }

    override fun onStop() {
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
        binding.sbTransparency.valueTo = MaskOverlayModel.MAX_OPACITY.toFloat()
        binding.sbTransparency.value = preferences.lastOpacity.toFloat()
        binding.sbTransparency.addOnChangeListener { _: Slider, value: Float, fromUser: Boolean ->
            val opacity = MaskOverlayModel.coerceOpacity(value.toInt())
            if (preferences.lastOpacity != opacity) {
                preferences.lastOpacity = opacity
                binding.tvOpacityValue.text = getString(R.string.opacity_value, opacity)
                binding.tvLastUsed.text = getString(R.string.last_used_value, opacity)
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
        renderHomeState(homeState)
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
                binding.btnShow.setBackgroundResource(R.drawable.bg_primary_button)
                binding.btnShow.setTextColor(ContextCompat.getColor(this, R.color.mask_bg_start))
            }

            MaskHomeUiModel.PrimaryAction.STOP -> {
                binding.btnShow.text = getString(R.string.stop_overlay)
                binding.btnShow.setBackgroundResource(R.drawable.bg_running_button)
                binding.btnShow.setTextColor(ContextCompat.getColor(this, R.color.mask_bg_start))
            }

            MaskHomeUiModel.PrimaryAction.GRANT_PERMISSION -> {
                binding.btnShow.text = getString(R.string.grant_and_start)
                binding.btnShow.setBackgroundResource(R.drawable.bg_primary_button)
                binding.btnShow.setTextColor(ContextCompat.getColor(this, R.color.mask_bg_start))
            }
        }
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

