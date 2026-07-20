package com.buspay.app.data

import com.buspay.app.domain.Bus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.FareType
import com.buspay.app.domain.ManagedCatalog
import com.buspay.app.domain.Route
import com.buspay.app.domain.ScheduledStopTime
import com.buspay.app.domain.ScheduledTrip
import com.buspay.app.domain.ServiceCalendar
import com.buspay.app.domain.Stop
import com.buspay.app.domain.TripAssignment
import com.buspay.app.domain.TripDirection
import com.buspay.app.domain.dutiesForDriver
import com.buspay.app.domain.isOperationallyValid
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManagedCatalogTest {
    @Test
    fun `sync endpoint maps to same-origin catalog endpoint`() {
        assertEquals(
            "https://sync.example.test/v1/catalog",
            catalogEndpoint("https://sync.example.test/v1/sync")
        )
        assertEquals(
            "http://127.0.0.1:8080/v1/catalog",
            catalogEndpoint("http://127.0.0.1:8080/v1/sync")
        )
    }

    @Test
    fun `managed catalog requires complete unique operational data`() {
        val valid = catalog()

        assertTrue(valid.isOperationallyValid())
        assertFalse(valid.copy(drivers = emptyList()).isOperationallyValid())
        assertFalse(
            valid.copy(drivers = valid.drivers + valid.drivers.first()).isOperationallyValid()
        )
        assertFalse(
            valid.copy(routes = valid.routes.map { it.copy(stops = emptyList()) })
                .isOperationallyValid()
        )
    }

    @Test
    fun `catalog refresh distinguishes authentication from missing permission`() = runBlocking {
        fun client(statusCode: Int) = ManagedCatalogClient(
            config = ProductionSyncConfig(
                endpointUrl = "https://sync.example.test/v1/sync",
                accessToken = "role-token"
            ),
            transport = SyncHttpTransport { SyncHttpResponse(statusCode, "{}") }
        )

        val unauthorized = client(401).fetch() as CatalogRefreshResult.Failure
        val forbidden = client(403).fetch() as CatalogRefreshResult.Failure

        assertTrue(unauthorized.message.contains("authentication was rejected"))
        assertTrue(forbidden.message.contains("cannot read"))
    }

    @Test
    fun `scheduled duties resolve offline and reject driver or bus overlap`() {
        val scheduled = catalog().copy(
            serviceCalendars = listOf(
                ServiceCalendar("daily", "Daily", "2026-01-01", "2030-12-31", setOf(1, 2, 3, 4, 5, 6, 7))
            ),
            scheduledTrips = listOf(
                ScheduledTrip(
                    "trip-1", "route-1", "daily", 480, TripDirection.OUTBOUND,
                    listOf(ScheduledStopTime("stop-1", 480, 500))
                ),
                ScheduledTrip(
                    "trip-2", "route-1", "daily", 490, TripDirection.INBOUND,
                    listOf(ScheduledStopTime("stop-1", 490, 510))
                )
            ),
            tripAssignments = listOf(
                TripAssignment("assignment-1", "trip-1", "2026-07-20", "driver-1", "bus-1")
            )
        )

        assertTrue(scheduled.isOperationallyValid())
        assertEquals("assignment-1", scheduled.dutiesForDriver("driver-1").single().assignment.id)
        assertFalse(
            scheduled.copy(
                tripAssignments = scheduled.tripAssignments +
                    TripAssignment("assignment-2", "trip-2", "2026-07-20", "driver-1", "bus-1")
            ).isOperationallyValid()
        )
    }

    private fun catalog() = ManagedCatalog(
        revision = 2,
        updatedAtMillis = 100L,
        drivers = listOf(Driver("driver-1", "Driver One")),
        buses = listOf(Bus("bus-1", "01-100-KS")),
        routes = listOf(
            Route(
                id = "route-1",
                name = "Route One",
                stops = listOf(Stop("stop-1", "Stop One", 42.0, 21.0, 1))
            )
        ),
        fareTypes = listOf(FareType("standard", "Standard", 50))
    )
}
