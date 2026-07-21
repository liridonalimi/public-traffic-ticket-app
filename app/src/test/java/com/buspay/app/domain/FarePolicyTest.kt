package com.buspay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FarePolicyTest {
    private val route = Route(
        id = "route-1",
        name = "Cross-zone route",
        stops = listOf(
            Stop("stop-1", "Zone A origin", 42.0, 21.0, 1, "A"),
            Stop("stop-2", "Zone A center", 42.1, 21.1, 2, "A"),
            Stop("stop-3", "Zone B destination", 42.2, 21.2, 3, "B")
        )
    )

    @Test
    fun `legacy fixed fare remains unchanged`() {
        val quote = calculateFareQuote(
            fareType = FareType("standard", "Standard", 50),
            route = route,
            originStop = route.stops.first(),
            destinationStop = route.stops.last(),
            soldAtMillis = 1_000L,
            minuteOfDay = 600
        )

        assertEquals(50, quote.priceCents)
        assertEquals(2, quote.zoneCount)
        assertFalse(quote.offPeakApplied)
        assertNull(quote.transferValidUntilMillis)
    }

    @Test
    fun `zone surcharge off peak discount and transfer are deterministic`() {
        val fare = FareType(
            id = "flex",
            name = "Flexible",
            priceCents = 50,
            additionalZoneCents = 20,
            offPeakDiscountCents = 10,
            offPeakStartMinutes = 540,
            offPeakEndMinutes = 960,
            transferWindowMinutes = 30
        )

        val quote = calculateFareQuote(
            fareType = fare,
            route = route,
            originStop = route.stops.first(),
            destinationStop = route.stops.last(),
            soldAtMillis = 1_000L,
            minuteOfDay = 600
        )

        assertEquals(60, quote.priceCents)
        assertEquals(2, quote.zoneCount)
        assertTrue(quote.offPeakApplied)
        assertEquals(1_801_000L, quote.transferValidUntilMillis)
    }

    @Test
    fun `overnight off peak interval crosses midnight and price never becomes negative`() {
        val fare = FareType(
            id = "night",
            name = "Night",
            priceCents = 20,
            offPeakDiscountCents = 50,
            offPeakStartMinutes = 1_320,
            offPeakEndMinutes = 300
        )

        val lateQuote = calculateFareQuote(
            fare, route, route.stops.first(), route.stops[1], 0L, 1_380
        )
        val daytimeQuote = calculateFareQuote(
            fare, route, route.stops.first(), route.stops[1], 0L, 720
        )

        assertTrue(lateQuote.offPeakApplied)
        assertEquals(0, lateQuote.priceCents)
        assertFalse(daytimeQuote.offPeakApplied)
        assertEquals(20, daytimeQuote.priceCents)
    }

    @Test
    fun `managed catalog requires a fare for every route`() {
        val secondRoute = Route(
            id = "route-2",
            name = "Unpriced route",
            stops = listOf(Stop("stop-4", "Only stop", 42.3, 21.3, 1, "C"))
        )
        val catalog = ManagedCatalog(
            revision = 1,
            updatedAtMillis = 0,
            drivers = listOf(Driver("driver-1", "Driver")),
            buses = listOf(Bus("bus-1", "01-001-KS")),
            routes = listOf(route, secondRoute),
            fareTypes = listOf(FareType("route-1-only", "Route one", 50, routeId = route.id))
        )

        assertFalse(catalog.isOperationallyValid())
        assertTrue(
            catalog.copy(
                fareTypes = catalog.fareTypes + FareType("network", "Network", 50)
            ).isOperationallyValid()
        )
    }
}
