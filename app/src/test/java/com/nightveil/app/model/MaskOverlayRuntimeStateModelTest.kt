package com.nightveil.app.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskOverlayRuntimeStateModelTest {

    @Test
    fun `stale persisted running state resets when service is no longer active`() {
        assertTrue(
            MaskOverlayRuntimeStateModel.shouldResetPersistedRunningState(
                persistedIsRunning = true,
                serviceIsRunning = false
            )
        )
    }

    @Test
    fun `active service keeps persisted running state intact`() {
        assertFalse(
            MaskOverlayRuntimeStateModel.shouldResetPersistedRunningState(
                persistedIsRunning = true,
                serviceIsRunning = true
            )
        )
    }

    @Test
    fun `already stopped state stays unchanged`() {
        assertFalse(
            MaskOverlayRuntimeStateModel.shouldResetPersistedRunningState(
                persistedIsRunning = false,
                serviceIsRunning = false
            )
        )
    }
}
