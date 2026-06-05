package com.nightveil.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskHomeUiModelTest {

    @Test
    fun `running overlay uses stop action and running badge`() {
        val state = MaskHomeUiModel.resolve(permissionGranted = true, isRunning = true)

        assertEquals(MaskHomeUiModel.PrimaryAction.STOP, state.primaryAction)
        assertEquals(MaskHomeUiModel.OverlayState.RUNNING, state.overlayState)
    }

    @Test
    fun `granted permission but stopped overlay uses start action`() {
        val state = MaskHomeUiModel.resolve(permissionGranted = true, isRunning = false)

        assertEquals(MaskHomeUiModel.PrimaryAction.START, state.primaryAction)
        assertEquals(MaskHomeUiModel.OverlayState.STOPPED, state.overlayState)
    }

    @Test
    fun `missing permission uses grant action`() {
        val state = MaskHomeUiModel.resolve(permissionGranted = false, isRunning = false)

        assertEquals(MaskHomeUiModel.PrimaryAction.GRANT_PERMISSION, state.primaryAction)
        assertEquals(MaskHomeUiModel.OverlayState.STOPPED, state.overlayState)
    }
}

