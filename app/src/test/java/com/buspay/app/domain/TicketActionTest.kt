package com.buspay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketActionTest {
    private val ticket = Ticket(
        id = "ticket-1",
        shiftId = "shift-1",
        fareTypeId = "standard",
        priceCents = 100,
        soldAtMillis = 100L,
        synced = true,
        printStatus = TicketPrintStatus.PRINTED
    )

    private fun action(
        id: String,
        type: TicketActionType,
        correctedPrice: Int? = null
    ) = TicketAction(
        id = id,
        originalTicketId = ticket.id,
        shiftId = ticket.shiftId,
        actionType = type,
        reason = TicketActionReason.WRONG_FARE,
        supervisorId = "supervisor-1",
        authorizedAtMillis = 200L,
        createdAtMillis = 200L,
        correctedFareTypeId = correctedPrice?.let { "student" },
        correctedPriceCents = correctedPrice
    )

    @Test
    fun `void changes effective revenue exactly once without mutating ticket`() {
        val void = action("action-void", TicketActionType.VOID)

        assertTrue(validateTicketAction(ticket, emptyList(), void))
        assertEquals(0, ticketRevenueCents(ticket, listOf(void)))
        assertEquals(100, ticket.priceCents)
        assertFalse(validateTicketAction(ticket, listOf(void), action("action-2", TicketActionType.VOID)))
    }

    @Test
    fun `correction replaces projected value while reprints have no revenue effect`() {
        val correction = action("action-correction", TicketActionType.CORRECTION, correctedPrice = 60)
        val reprint = action("action-reprint", TicketActionType.REPRINT)

        assertTrue(validateTicketAction(ticket, emptyList(), correction))
        assertEquals(60, ticketRevenueCents(ticket, listOf(correction)))
        assertTrue(validateTicketAction(ticket, listOf(reprint), action("action-reprint-2", TicketActionType.REPRINT)))
        assertEquals(100, ticketRevenueCents(ticket, listOf(reprint)))
    }
}
