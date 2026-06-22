package com.nightveil.app.model

object MaskOverlayRuntimeStateModel {
    fun shouldResetPersistedRunningState(
        persistedIsRunning: Boolean,
        serviceIsRunning: Boolean
    ): Boolean {
        return persistedIsRunning && !serviceIsRunning
    }
}
