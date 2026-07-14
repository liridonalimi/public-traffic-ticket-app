package com.buspay.app.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class EscPosTicketTest {
    @Test
    fun encodesTicketDetailsAndEscPosCommands() {
        val bytes = buildEscPosTicketBytes(
            PrintableTicket(
                ticketCode = "ABC12345",
                busPlateNumber = "01-101-KS",
                routeName = "Line 1",
                fareName = "Student",
                priceCents = 30,
                soldAtText = "14/07/26, 10:30",
                operatorName = "Arben Krasniqi"
            )
        )
        val text = bytes.toString(StandardCharsets.UTF_8)

        assertEquals(0x1B.toByte(), bytes.first())
        assertTrue(text.contains("Ticket: ABC12345"))
        assertTrue(text.contains("Bus: 01-101-KS"))
        assertTrue(text.contains("Route: Line 1"))
        assertTrue(text.contains("Student       1   EUR 0.30"))
        assertTrue(text.contains("TOTAL: EUR 0.30"))
        assertTrue(text.contains("Operator: Arben Krasniqi"))
        assertTrue(text.contains("TEST - NOT FISCAL"))
        assertTrue(bytes.takeLast(3).toByteArray().contentEquals(byteArrayOf(0x1D, 0x56, 0x00)))
    }
}
