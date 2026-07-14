package com.buspay.app.ui.screens

import com.buspay.app.domain.Route
import com.buspay.app.domain.RouteProgress
import com.buspay.app.domain.RouteProgressSource
import com.buspay.app.domain.Shift
import com.buspay.app.domain.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverShiftUiStateTest {
    private val closedRoute = Route(
        id = "closed-route",
        name = "Closed route",
        stops = listOf(
            Stop("first", "First stop", 42.0, 21.0, 1),
            Stop("last", "Last stop", 42.1, 21.1, 2)
        )
    )
    private val finalProgress = RouteProgress(
        shiftId = "closed-shift",
        currentStopIndex = 1,
        updatedAtMillis = 123L,
        source = RouteProgressSource.MANUAL
    )

    @Test
    fun `closed shift snapshot keeps passenger display available`() {
        val state = DriverShiftUiState(
            lastClosedRoute = closedRoute,
            lastClosedRouteProgress = finalProgress
        )

        assertTrue(state.canOpenPassengerDisplay)
        assertEquals("Closed route", state.passengerRoute?.name)
        assertEquals("Last stop", state.passengerRouteStopStatus.currentStop?.name)
        assertTrue(state.passengerRouteStopStatus.isRouteComplete)
    }

    @Test
    fun `closed snapshot is independent from the next selected route`() {
        val nextRoute = closedRoute.copy(id = "next-route", name = "Next route")
        val state = DriverShiftUiState(
            selectedRoute = nextRoute,
            lastClosedRoute = closedRoute,
            lastClosedRouteProgress = finalProgress
        )

        assertEquals("Closed route", state.passengerRoute?.name)
    }

    @Test
    fun `passenger display is unavailable without active or closed progress`() {
        val state = DriverShiftUiState(selectedRoute = closedRoute)

        assertFalse(state.canOpenPassengerDisplay)
    }

    @Test
    fun `active shift uses live route instead of closed snapshot`() {
        val activeRoute = closedRoute.copy(id = "active-route", name = "Active route")
        val activeProgress = finalProgress.copy(shiftId = "active-shift", currentStopIndex = 0)
        val state = DriverShiftUiState(
            selectedRoute = activeRoute,
            activeShift = Shift(
                id = "active-shift",
                driverId = "driver",
                busId = "bus",
                routeId = activeRoute.id,
                startedAtMillis = 1L
            ),
            routeProgress = activeProgress,
            lastClosedRoute = closedRoute,
            lastClosedRouteProgress = finalProgress
        )

        assertEquals("Active route", state.passengerRoute?.name)
        assertEquals("First stop", state.passengerRouteStopStatus.currentStop?.name)
    }
}
