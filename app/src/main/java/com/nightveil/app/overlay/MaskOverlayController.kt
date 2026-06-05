package com.nightveil.app.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import com.nightveil.app.databinding.WidgetMaskViewBinding
import com.nightveil.app.model.MaskOverlayAnimationModel

class MaskOverlayController(
    private val binding: WidgetMaskViewBinding
) {
    fun show(opacity: Int) {
        binding.viewMask.post {
            val normalizedAlpha = opacity / 100f
            val centerX = binding.viewMask.width / 2
            val centerY = binding.viewMask.height / 2
            val finalRadius = MaskOverlayAnimationModel.finalRadius(
                binding.viewMask.width,
                binding.viewMask.height
            )

            binding.viewMask.animate().cancel()
            binding.viewMask.isVisible = true

            if (MaskOverlayAnimationModel.shouldUseCircularReveal(
                    sdkInt = android.os.Build.VERSION.SDK_INT,
                    width = binding.viewMask.width,
                    height = binding.viewMask.height
                ) && finalRadius > 0f
            ) {
                binding.viewMask.alpha = normalizedAlpha
                ViewAnimationUtils.createCircularReveal(
                    binding.viewMask,
                    centerX,
                    centerY,
                    0f,
                    finalRadius
                ).apply {
                    duration = MaskOverlayAnimationModel.SHOW_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            } else {
                binding.viewMask.alpha = 0f
                binding.viewMask.animate()
                    .alpha(normalizedAlpha)
                    .setDuration(MaskOverlayAnimationModel.FADE_DURATION_MS)
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
        binding.viewMask.post {
            val centerX = binding.viewMask.width / 2
            val centerY = binding.viewMask.height / 2
            val finalRadius = MaskOverlayAnimationModel.finalRadius(
                binding.viewMask.width,
                binding.viewMask.height
            )

            binding.viewMask.animate().cancel()

            if (MaskOverlayAnimationModel.shouldUseCircularReveal(
                    sdkInt = android.os.Build.VERSION.SDK_INT,
                    width = binding.viewMask.width,
                    height = binding.viewMask.height
                ) && finalRadius > 0f
            ) {
                ViewAnimationUtils.createCircularReveal(
                    binding.viewMask,
                    centerX,
                    centerY,
                    finalRadius,
                    0f
                ).apply {
                    duration = MaskOverlayAnimationModel.HIDE_DURATION_MS
                    interpolator = AccelerateDecelerateInterpolator()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            binding.viewMask.alpha = 0f
                            binding.viewMask.isVisible = false
                            onComplete()
                        }
                    })
                    start()
                }
            } else {
                binding.viewMask.animate()
                    .alpha(0f)
                    .setDuration(MaskOverlayAnimationModel.FADE_DURATION_MS)
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
    }
}

