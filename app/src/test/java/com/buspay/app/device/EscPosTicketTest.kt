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
        assertTrue(text.contains("Nr. biletes: ABC12345"))
        assertTrue(text.contains("Autobusi: 01-101-KS"))
        assertTrue(text.contains("Linja: Line 1"))
        assertTrue(text.contains("Student"))
        assertTrue(text.contains("TOTALI"))
        assertTrue(text.contains("EUR 0.30"))
        assertTrue(text.contains("Operatori: Arben Krasniqi"))
        assertTrue(text.contains("TEST - JO FISKAL"))
        assertTrue(text.contains("NUK ESHTE KUPON FISKAL"))
        assertTrue(bytes.takeLast(3).toByteArray().contentEquals(byteArrayOf(0x1D, 0x56, 0x00)))
    }

    @Test
    fun respectsTheSelectedPaperWidth() {
        val ticket = PrintableTicket(
            ticketCode = "ABC12345",
            busPlateNumber = "01-101-KS",
            routeName = "Linja 2 - Qendër–Bregu i Diellit",
            fareName = "Pensionist 65+",
            priceCents = 25,
            soldAtText = "16/07/26, 11:47",
            operatorName = "Ilir Gashi"
        )

        ReceiptPaperProfile.entries.forEach { profile ->
            val receipt = escPosTicketText(ticket, profile)
            assertTrue(receipt.lineSequence().all { it.length <= profile.textColumns })
            assertTrue(receipt.contains("Linja:"))
            assertTrue(receipt.contains("EUR 0.25"))
        }
    }
}
