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
        assertEquals("SHIFT ACTIVE", driverShiftStatus(state))
    }

    @Test
    fun operationsToolsAreAvailableBetweenShifts() {
        val state = DriverShiftUiState(
            signedInDriver = Driver("driver-001", "Test Driver")
        )

        assertTrue(canOpenOperationsTools(state))
        assertEquals("READY TO START", driverShiftStatus(state))
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

        assertEquals(
            "2 closed shift(s) waiting for synchronization",
            driverSyncSummary(waiting)
        )
        assertEquals(
            "All closed shifts are synchronized with the local server",
            driverSyncSummary(synchronized)
        )
    }
}
