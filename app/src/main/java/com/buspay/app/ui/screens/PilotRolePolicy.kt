package com.buspay.app.ui.screens

enum class PilotWorkspace {
    DRIVER,
    OPERATIONS
}

enum class DriverShiftStatus {
    SIGN_IN_REQUIRED,
    READY_TO_START,
    ACTIVE
}

sealed interface DriverSyncStatus {
    data object Synchronizing : DriverSyncStatus
    data class Waiting(val closedShiftCount: Int) : DriverSyncStatus
    data object DemoReady : DriverSyncStatus
    data object LocalSynchronized : DriverSyncStatus
    data object ProductionSynchronized : DriverSyncStatus
}

fun canOpenOperationsTools(state: DriverShiftUiState): Boolean {
    return !state.isShiftActive && !state.isPrinting
}

fun driverSyncStatus(state: DriverShiftUiState): DriverSyncStatus {
    if (state.isSyncing) return DriverSyncStatus.Synchronizing
    if (state.pendingShiftCount > 0) {
        return DriverSyncStatus.Waiting(state.pendingShiftCount)
    }
    return when (state.syncRuntimeMode) {
        com.buspay.app.data.SyncRuntimeMode.DEMO -> DriverSyncStatus.DemoReady
        com.buspay.app.data.SyncRuntimeMode.LOCAL_VALIDATION ->
            DriverSyncStatus.LocalSynchronized
        com.buspay.app.data.SyncRuntimeMode.PRODUCTION -> DriverSyncStatus.ProductionSynchronized
    }
}

fun driverShiftStatus(state: DriverShiftUiState): DriverShiftStatus {
    return when {
        state.isShiftActive -> DriverShiftStatus.ACTIVE
        state.isDriverSignedIn -> DriverShiftStatus.READY_TO_START
        else -> DriverShiftStatus.SIGN_IN_REQUIRED
    }
}
