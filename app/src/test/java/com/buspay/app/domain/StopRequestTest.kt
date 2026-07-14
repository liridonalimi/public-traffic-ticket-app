package com.buspay.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopRequestTest {
    private val request = StopRequest(
        shiftId = "shift-1",
        requestedStopIndex = 2,
        requestedAtMillis = 100L
    )

    @Test
    fun `request remains active before requested stop`() {
        val progress = progressAt(index = 1)

        assertFalse(isStopRequestReached(request, progress))
    }

    @Test
    fun `request clears at requested stop`() {
        val progress = progressAt(index = 2)

        assertTrue(isStopRequestReached(request, progress))
    }

    @Test
    fun `request clears when progress passes requested stop`() {
        val progress = progressAt(index = 3)

        assertTrue(isStopRequestReached(request, progress))
    }

    @Test
    fun `progress from another shift cannot clear request`() {
        val progress = progressAt(index = 3).copy(shiftId = "shift-2")

        assertFalse(isStopRequestReached(request, progress))
    }

    private fun progressAt(index: Int) = RouteProgress(
        shiftId = "shift-1",
        currentStopIndex = index,
        updatedAtMillis = 200L,
        source = RouteProgressSource.GPS
    )
}
