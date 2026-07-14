package com.buspay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TicketSummaryTest {
    private val fares = listOf(
        FareType(id = "standard", name = "Standard", priceCents = 50),
        FareType(id = "student", name = "Student", priceCents = 30)
    )

    @Test
    fun summarizesTicketCountsAndCashByFareOrder() {
        val tickets = listOf(
            ticket(id = "1", fareTypeId = "student", priceCents = 30),
            ticket(id = "2", fareTypeId = "standard", priceCents = 50),
            ticket(id = "3", fareTypeId = "student", priceCents = 30)
        )

        val summaries = summarizeTicketsByFare(tickets, fares)

        assertEquals(2, summaries.size)
        assertEquals(
            FareTypeSummary("standard", "Standard", 1, 50),
            summaries[0]
        )
        assertEquals(
            FareTypeSummary("student", "Student", 2, 60),
            summaries[1]
        )
    }

    @Test
    fun retainsTicketsWhoseFareWasRemovedFromCatalog() {
        val summaries = summarizeTicketsByFare(
            tickets = listOf(ticket(id = "1", fareTypeId = "retired", priceCents = 10)),
            fareTypes = fares
        )

        assertEquals(
            listOf(FareTypeSummary("retired", "Unknown fare", 1, 10)),
            summaries
        )
    }

    private fun ticket(id: String, fareTypeId: String, priceCents: Int) = Ticket(
        id = id,
        shiftId = "shift-1",
        fareTypeId = fareTypeId,
        priceCents = priceCents,
        soldAtMillis = 1L,
        synced = false
    )
}
