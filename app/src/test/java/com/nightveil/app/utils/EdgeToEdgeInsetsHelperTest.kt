package com.nightveil.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeToEdgeInsetsHelperTest {

    @Test
    fun `merge adds system bar insets to base padding`() {
        val result = EdgeToEdgeInsetsHelper.merge(
            base = EdgeToEdgeInsetsHelper.PaddingState(20, 20, 20, 20),
            insets = EdgeToEdgeInsetsHelper.InsetsState(0, 96, 0, 120)
        )

        assertEquals(20, result.left)
        assertEquals(116, result.top)
        assertEquals(20, result.right)
        assertEquals(140, result.bottom)
    }
}

