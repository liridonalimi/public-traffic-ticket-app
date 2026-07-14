package com.buspay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncModelsTest {
    private val shift = Shift(
        id = "shift-1",
        driverId = "driver-1",
        busId = "bus-1",
        routeId = "route-1",
        startedAtMillis = 10L,
        endedAtMillis = 20L
    )
    private val ticket = Ticket(
        id = "ticket-1",
        shiftId = shift.id,
        priceCents = 50,
        soldAtMillis = 15L,
        synced = false
    )

    @Test
    fun `batch request id is stable regardless of entity order`() {
        val secondTicket = ticket.copy(id = "ticket-2")

        val first = createSyncBatch(listOf(shift), listOf(ticket, secondTicket))
        val second = createSyncBatch(listOf(shift), listOf(secondTicket, ticket))

        assertEquals(first.requestId, second.requestId)
    }

    @Test
    fun `batch request id changes when payload entities change`() {
        val first = createSyncBatch(listOf(shift), listOf(ticket))
        val second = createSyncBatch(listOf(shift), listOf(ticket.copy(id = "ticket-2")))

        assertNotEquals(first.requestId, second.requestId)
    }

    @Test
    fun `only acknowledged shifts become synchronized`() {
        val unacknowledged = shift.copy(id = "shift-2")

        val updated = acknowledgeShifts(
            shifts = listOf(shift, unacknowledged),
            acknowledgedIds = setOf(shift.id, "unknown-shift")
        )

        assertTrue(updated.first().synced)
        assertFalse(updated.last().synced)
    }

    @Test
    fun `only acknowledged tickets become synchronized`() {
        val unacknowledged = ticket.copy(id = "ticket-2")

        val updated = acknowledgeTickets(
            tickets = listOf(ticket, unacknowledged),
            acknowledgedIds = setOf(ticket.id, "unknown-ticket")
        )

        assertTrue(updated.first().synced)
        assertFalse(updated.last().synced)
    }
}
