package com.buspay.app.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptLayoutTest {
    @Test
    fun alignsPriceAtTheRightEdgeWithoutOverflow() {
        val row = alignedReceiptRow("Pensionist 65+", "EUR 0.25", 32)

        assertEquals(32, row.length)
        assertTrue(row.startsWith("Pensionist 65+"))
        assertTrue(row.endsWith("EUR 0.25"))
    }

    @Test
    fun wrapsLongRouteAtWordBoundaries() {
        val lines = wrapReceiptText("Linja: Qendër Bregu i Diellit dhe Spitali", 20)

        assertTrue(lines.size > 1)
        assertTrue(lines.all { it.length <= 20 })
        assertEquals("Linja: Qendër Bregu", lines.first())
    }

    @Test
    fun mapsBothPdfSimulatorsToTheirPaperProfiles() {
        assertEquals(
            ReceiptPaperProfile.MM58,
            PdfTicketPrinter.paperProfile(PdfTicketPrinter.TEST_DEVICE)
        )
        assertEquals(
            ReceiptPaperProfile.MM80,
            PdfTicketPrinter.paperProfile(PdfTicketPrinter.TEST_DEVICE_80MM)
        )
        assertNull(PdfTicketPrinter.paperProfile(PrinterDevice("Physical", "AA:BB")))
    }
}
