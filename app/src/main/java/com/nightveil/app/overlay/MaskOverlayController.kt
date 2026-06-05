package com.nightveil.app.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import com.nightveil.app.databinding.WidgetMaskViewBinding

class MaskOverlayController(
    private val binding: WidgetMaskViewBinding
) {
    fun show(opacity: Int) {
        binding.viewMask.post {
            val normalizedAlpha = opacity / 100f
            val centerX = binding.viewMask.width / 2
            val centerY = binding.viewMask.height / 2
            val finalRadius = kotlin.math.hypot(centerX.toDouble(), centerY.toDouble()).toFloat()

            binding.viewMask.isVisible = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && finalRadius > 0f) {
                binding.viewMask.alpha = normalizedAlpha
                ViewAnimationUtils.createCircularReveal(
                    binding.viewMask,
                    centerX,
                    centerY,
                    0f,
                    finalRadius
                ).apply {
                    duration = 280L
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            } else {
                binding.viewMask.alpha = 0f
                binding.viewMask.animate()
                    .alpha(normalizedAlpha)
                    .setDuration(220L)
                    .start()
            }
        }
    }

    fun updateOpacity(opacity: Int) {
        binding.viewMask.animate().cancel()
        binding.viewMask.alpha = opacity / 100f
        binding.viewMask.isVisible = true
    }

    fun hide(onComplete: () -> Unit) {
        binding.viewMask.animate()
            .alpha(0f)
            .setDuration(180L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.viewMask.isVisible = false
                    binding.viewMask.animate().setListener(null)
                    onComplete()
                }
            })
            .start()
    }
}

