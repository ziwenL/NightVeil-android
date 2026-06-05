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
    private val appContext = context.applicationContext
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

        fun createOverlayLayoutParams(
            type: Int,
            screenHeight: Int,
            statusBarHeight: Int
        ): WindowManager.LayoutParams {
            val config = createOverlayLayoutConfig(screenHeight, statusBarHeight)
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.width = config.width
            layoutParams.height = config.height
            layoutParams.type = type
            layoutParams.flags = config.flags
            layoutParams.format = config.format
            layoutParams.gravity = config.gravity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && config.layoutInDisplayCutoutMode != null) {
                layoutParams.layoutInDisplayCutoutMode = config.layoutInDisplayCutoutMode
            }
            return layoutParams
        }

        fun createOverlayLayoutConfig(
            screenHeight: Int,
            statusBarHeight: Int
        ): OverlayLayoutConfig {
            return OverlayLayoutConfig(
                width = WindowManager.LayoutParams.MATCH_PARENT,
                height = screenHeight + statusBarHeight,
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                format = PixelFormat.TRANSLUCENT,
                gravity = Gravity.START or Gravity.TOP,
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                } else {
                    null
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

        val layoutParams = createOverlayLayoutParams(
            type = type,
            screenHeight = appContext.resources.displayMetrics.heightPixels,
            statusBarHeight = getStatusBarHeight()
        )

        childViewMap[view] = layoutParams
        windowManager?.addView(view, layoutParams)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId != 0) {
            appContext.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
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

