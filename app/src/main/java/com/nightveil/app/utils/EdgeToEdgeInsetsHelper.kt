package com.nightveil.app.utils

object EdgeToEdgeInsetsHelper {
    data class PaddingState(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    data class InsetsState(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    fun merge(base: PaddingState, insets: InsetsState): PaddingState {
        return PaddingState(
            left = base.left + insets.left,
            top = base.top + insets.top,
            right = base.right + insets.right,
            bottom = base.bottom + insets.bottom
        )
    }
}

