package com.buspay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProgressTest {
    private val stops = listOf(
        Stop("a", "Central Station", 42.6629, 21.1655, 1),
        Stop("b", "Mother Teresa Boulevard", 42.6608, 21.1622, 2),
        Stop("c", "University Hospital", 42.6488, 21.1612, 3)
    )

    @Test
    fun `gps chooses the nearest stop at or after current progress`() {
        val stopIndex = nearestForwardStopIndex(
            stops = stops,
            latitude = stops[1].latitude,
            longitude = stops[1].longitude,
            currentStopIndex = 0
        )

        assertEquals(1, stopIndex)
    }

    @Test
    fun `gps never moves route progress backwards`() {
        val stopIndex = nearestForwardStopIndex(
            stops = stops,
            latitude = stops[0].latitude,
            longitude = stops[0].longitude,
            currentStopIndex = 1
        )

        assertEquals(1, stopIndex)
    }

    @Test
    fun `gps outside arrival radius does not advance progress`() {
        val stopIndex = nearestForwardStopIndex(
            stops = stops,
            latitude = 52.5200,
            longitude = 13.4050,
            currentStopIndex = 0
        )

        assertEquals(0, stopIndex)
    }

    @Test
    fun `route status exposes current and next stop`() {
        val status = routeStopStatus(stops, currentStopIndex = 1)

        assertEquals("Mother Teresa Boulevard", status.currentStop?.name)
        assertEquals("University Hospital", status.nextStop?.name)
        assertFalse(status.isRouteComplete)
    }

    @Test
    fun `last stop completes route and has no next stop`() {
        val status = routeStopStatus(stops, currentStopIndex = stops.lastIndex)

        assertEquals("University Hospital", status.currentStop?.name)
        assertNull(status.nextStop)
        assertTrue(status.isRouteComplete)
    }

    @Test
    fun `empty route is safe`() {
        val status = routeStopStatus(emptyList(), currentStopIndex = 3)

        assertNull(status.currentStop)
        assertNull(status.nextStop)
        assertFalse(status.isRouteComplete)
    }
}
