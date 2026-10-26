package com.nightveil.app.utils

import android.app.Service
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class FloatingWindowHelper(context: Context) {
    private var windowManager: WindowManager? =
        context.getSystemService(WINDOW_SERVICE) as? WindowManager
    private val childViewMap = HashMap<View, WindowManager.LayoutParams>()

    companion object {
        data class OverlayLayoutConfig(
            val width: Int,
            val height: Int,
            val flags: Int,
            val format: Int,
            val gravity: Int,
            val layoutInDisplayCutoutMode: Int?
        )

        fun canDrawOverlays(context: Context, isApplyAuthorization: Boolean = false): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    true
                } else {
                    if (isApplyAuthorization) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        if (context is Service) {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                    false
                }
            } else {
                true
            }
        }

        fun createOverlayLayoutParams(type: Int): WindowManager.LayoutParams {
            val config = createOverlayLayoutConfig()
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.width = config.width
            layoutParams.height = config.height
            layoutParams.type = type
            layoutParams.flags = config.flags
            layoutParams.format = config.format
            layoutParams.gravity = config.gravity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // The veil must include system-bar areas rather than fit inside their insets.
                layoutParams.setFitInsetsTypes(0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && config.layoutInDisplayCutoutMode != null) {
                layoutParams.layoutInDisplayCutoutMode = config.layoutInDisplayCutoutMode
            }
            return layoutParams
        }

        fun createOverlayLayoutConfig(): OverlayLayoutConfig {
            return OverlayLayoutConfig(
                width = WindowManager.LayoutParams.MATCH_PARENT,
                height = WindowManager.LayoutParams.MATCH_PARENT,
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                format = PixelFormat.TRANSLUCENT,
                gravity = Gravity.START or Gravity.TOP,
                layoutInDisplayCutoutMode = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    else -> null
                }
            )
        }
    }

    fun addOverlayView(view: View) {
        if (windowManager == null || childViewMap.containsKey(view)) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = createOverlayLayoutParams(type)
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            view.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        childViewMap[view] = layoutParams
        windowManager?.addView(view, layoutParams)
    }

    fun destroy() {
        if (windowManager == null) return

        val views = childViewMap.keys.toList()
        views.forEach { view ->
            windowManager?.removeView(view)
        }
        childViewMap.clear()
        windowManager = null
    }
}

