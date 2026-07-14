package com.buspay.app.device

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfTicketContentTest {
    private val ticket = PrintableTicket(
        ticketCode = "ABC12345",
        busPlateNumber = "01-101-KS",
        routeName = "Line 1 - Center to Hospital",
        fareName = "Student",
        priceCents = 30,
        soldAtText = "14/07/26, 10:30",
        operatorName = "Arben Krasniqi"
    )

    @Test
    fun exposesEveryTicketFieldForPdfRendering() {
        assertEquals(
            listOf(
                "Nr. biletes: ABC12345",
                "Autobusi: 01-101-KS",
                "Operatori: Arben Krasniqi",
                "Data / Ora: 14/07/26, 10:30"
            ),
            pdfTicketLines(ticket)
        )
    }

    @Test
    fun formatsPdfPriceAsEuros() {
        assertEquals("EUR 0.30", pdfTicketPriceText(ticket))
    }

    @Test
    fun testQrIsExplicitlyNonFiscal() {
        assertEquals(
            "BUSPAY_TEST_NON_FISCAL|ticket=ABC12345|bus=01-101-KS|amount=0.30|sold=14/07/26, 10:30",
            nonFiscalQrPayload(ticket)
        )
    }
}
