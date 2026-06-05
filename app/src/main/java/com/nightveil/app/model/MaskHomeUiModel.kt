package com.nightveil.app.model

object MaskHomeUiModel {
    enum class PrimaryAction {
        START,
        STOP,
        GRANT_PERMISSION
    }

    enum class OverlayState {
        RUNNING,
        STOPPED
    }

    data class State(
        val primaryAction: PrimaryAction,
        val overlayState: OverlayState
    )

    fun resolve(permissionGranted: Boolean, isRunning: Boolean): State {
        if (isRunning) {
            return State(
                primaryAction = PrimaryAction.STOP,
                overlayState = OverlayState.RUNNING
            )
        }

        return State(
            primaryAction = if (permissionGranted) {
                PrimaryAction.START
            } else {
                PrimaryAction.GRANT_PERMISSION
            },
            overlayState = OverlayState.STOPPED
        )
    }
}

