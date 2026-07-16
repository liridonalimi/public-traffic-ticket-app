package com.buspay.app.ui.screens

import com.buspay.app.data.SyncRuntimeMode
import com.buspay.app.domain.Driver
import com.buspay.app.domain.Shift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PilotRolePolicyTest {
    @Test
    fun operationsToolsAreUnavailableDuringAnActiveShift() {
        val state = DriverShiftUiState(
            signedInDriver = Driver("driver-001", "Test Driver"),
            activeShift = Shift(
                id = "shift-001",
                driverId = "driver-001",
                busId = "bus-001",
                routeId = "route-001",
                startedAtMillis = 1L
            )
        )

        assertFalse(canOpenOperationsTools(state))
        assertEquals(DriverShiftStatus.ACTIVE, driverShiftStatus(state))
    }

    @Test
    fun operationsToolsAreAvailableBetweenShifts() {
        val state = DriverShiftUiState(
            signedInDriver = Driver("driver-001", "Test Driver")
        )

        assertTrue(canOpenOperationsTools(state))
        assertEquals(DriverShiftStatus.READY_TO_START, driverShiftStatus(state))
    }

    @Test
    fun driverReceivesAConciseSynchronizationSummary() {
        val waiting = DriverShiftUiState(
            pendingShiftCount = 2,
            syncRuntimeMode = SyncRuntimeMode.LOCAL_VALIDATION
        )
        val synchronized = DriverShiftUiState(
            syncRuntimeMode = SyncRuntimeMode.LOCAL_VALIDATION
        )

        assertEquals(DriverSyncStatus.Waiting(2), driverSyncStatus(waiting))
        assertEquals(DriverSyncStatus.LocalSynchronized, driverSyncStatus(synchronized))
    }
}
