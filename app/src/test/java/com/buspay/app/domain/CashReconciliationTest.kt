package com.buspay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CashReconciliationTest {
    private val closedShift = Shift(
        id = "shift",
        driverId = "driver",
        busId = "bus",
        routeId = "route",
        startedAtMillis = 100L,
        endedAtMillis = 200L
    )

    @Test
    fun `euro input parses without floating point rounding`() {
        assertEquals(290, parseEuroAmountToCents("2.90"))
        assertEquals(290, parseEuroAmountToCents("2,9"))
        assertEquals(200, parseEuroAmountToCents("2"))
        assertEquals(0, parseEuroAmountToCents("0.00"))
        assertNull(parseEuroAmountToCents("-1.00"))
        assertNull(parseEuroAmountToCents("2.999"))
        assertNull(parseEuroAmountToCents("not cash"))
    }

    @Test
    fun `shift derives matched shortage surplus and legacy states`() {
        assertEquals(CashReconciliationStatus.NOT_RECORDED, closedShift.cashReconciliationStatus)
        assertEquals(
            CashReconciliationStatus.MATCHED,
            closedShift.copy(
                expectedCashCents = 290,
                declaredCashCents = 290,
                reconciledAtMillis = 200L
            ).cashReconciliationStatus
        )
        assertEquals(
            CashReconciliationStatus.SHORTAGE,
            closedShift.copy(
                expectedCashCents = 290,
                declaredCashCents = 250,
                reconciledAtMillis = 200L
            ).cashReconciliationStatus
        )
        val surplus = closedShift.copy(
            expectedCashCents = 290,
            declaredCashCents = 300,
            reconciledAtMillis = 200L
        )
        assertEquals(CashReconciliationStatus.SURPLUS, surplus.cashReconciliationStatus)
        assertEquals(10, surplus.cashVarianceCents)
    }
}
