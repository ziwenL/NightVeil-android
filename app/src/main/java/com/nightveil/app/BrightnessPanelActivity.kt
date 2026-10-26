package com.nightveil.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.nightveil.app.data.MaskPreferences
import com.nightveil.app.databinding.ActivityBrightnessPanelBinding
import com.nightveil.app.model.MaskOverlayModel
import com.nightveil.app.model.MaskOverlayStateSignal
import com.nightveil.app.model.MaskServiceAction
import com.nightveil.app.utils.FloatingWindowHelper

/** A separate dialog task keeps notification adjustments out of the main app screen. */
class BrightnessPanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrightnessPanelBinding
    private lateinit var preferences: MaskPreferences
    private var receiverRegistered = false
    private var dragging = false
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = MaskPreferences(this)
        if (!canAdjust()) {
            finish()
            return
        }
        binding = ActivityBrightnessPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFinishOnTouchOutside(true)
        val width = minOf(resources.displayMetrics.widthPixels - dp(32), dp(360))
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)

        binding.sliderIntensity.apply {
            valueFrom = MaskOverlayModel.MIN_OPACITY.toFloat()
            valueTo = MaskOverlayModel.MAX_OPACITY.toFloat()
            value = preferences.lastOpacity.toFloat()
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    if (!canAdjust()) {
                        finish()
                        return@addOnChangeListener
                    }
                    val opacity = MaskOverlayModel.coerceOpacity(value.toInt())
                    binding.tvIntensity.text = getString(R.string.last_used_value, opacity)
                    startService(FloatingService.createIntent(
                        this@BrightnessPanelActivity, MaskServiceAction.SET_OPACITY, opacity
                    ))
                }
            }
            addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                    dragging = true
                }

                override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                    dragging = false
                }
            })
        }
        binding.btnDone.setOnClickListener { finish() }
        refreshState()
    }

    override fun onStart() {
        super.onStart()
        if (!::binding.isInitialized) return
        val filter = IntentFilter(MaskOverlayStateSignal.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(stateReceiver, filter)
        }
        receiverRegistered = true
        refreshState()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
        super.onStop()
        // Do not leave a stale adjustment task behind after Home or another app is opened.
        if (!isChangingConfigurations) finish()
    }

    private fun canAdjust() = preferences.isRunning && FloatingWindowHelper.canDrawOverlays(this)

    private fun refreshState() {
        if (!canAdjust()) {
            finish()
            return
        }
        if (!dragging) {
            binding.sliderIntensity.value = preferences.lastOpacity.toFloat()
            binding.tvIntensity.text = getString(R.string.last_used_value, preferences.lastOpacity)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
