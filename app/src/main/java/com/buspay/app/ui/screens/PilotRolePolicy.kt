package com.buspay.app.ui.screens

import com.buspay.app.data.SyncRuntimeMode

enum class PilotWorkspace {
    DRIVER,
    OPERATIONS
}

fun canOpenOperationsTools(state: DriverShiftUiState): Boolean {
    return !state.isShiftActive && !state.isPrinting
}

fun driverSyncSummary(state: DriverShiftUiState): String {
    if (state.isSyncing) return "Synchronizing closed shifts…"
    if (state.pendingShiftCount > 0) {
        return "${state.pendingShiftCount} closed shift(s) waiting for synchronization"
    }
    return when (state.syncRuntimeMode) {
        SyncRuntimeMode.DEMO -> "Local records are ready; demo synchronization is selected"
        SyncRuntimeMode.LOCAL_VALIDATION -> "All closed shifts are synchronized with the local server"
        SyncRuntimeMode.PRODUCTION -> "All closed shifts are synchronized"
    }
}

fun driverShiftStatus(state: DriverShiftUiState): String {
    return when {
        state.isShiftActive -> "SHIFT ACTIVE"
        state.isDriverSignedIn -> "READY TO START"
        else -> "DRIVER SIGN-IN REQUIRED"
    }
}
