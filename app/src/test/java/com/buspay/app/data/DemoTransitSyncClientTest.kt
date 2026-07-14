package com.buspay.app.data

import com.buspay.app.domain.Shift
import com.buspay.app.domain.SyncResult
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.createSyncBatch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTransitSyncClientTest {
    private val shift = Shift(
        id = "shift-1",
        driverId = "driver",
        busId = "bus",
        routeId = "route",
        startedAtMillis = 1L,
        endedAtMillis = 2L
    )
    private val ticket = Ticket(
        id = "ticket-1",
        shiftId = shift.id,
        priceCents = 50,
        soldAtMillis = 2L,
        synced = false
    )

    @Test
    fun `online demo server acknowledges every entity`() = runBlocking {
        val result = DemoTransitSyncClient().sync(
            createSyncBatch(listOf(shift), listOf(ticket))
        )

        assertTrue(result is SyncResult.Success)
        val acknowledgement = (result as SyncResult.Success).acknowledgement
        assertEquals(setOf(shift.id), acknowledgement.acknowledgedShiftIds)
        assertEquals(setOf(ticket.id), acknowledgement.acknowledgedTicketIds)
    }

    @Test
    fun `offline demo server returns failure without acknowledgements`() = runBlocking {
        val client = DemoTransitSyncClient().apply { isAvailable = false }

        val result = client.sync(createSyncBatch(listOf(shift), listOf(ticket)))

        assertTrue(result is SyncResult.Failure)
    }
}
