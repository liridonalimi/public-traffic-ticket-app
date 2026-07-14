package com.buspay.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminReportingTest {
    private val drivers = listOf(
        Driver("driver-1", "Arben"),
        Driver("driver-2", "Drita")
    )
    private val buses = listOf(Bus("bus-1", "01-101-KS"))
    private val routes = listOf(Route("route-1", "Line 1", emptyList()))
    private val fares = listOf(
        FareType("standard", "Standard", 50),
        FareType("student", "Student", 30)
    )
    private val shifts = listOf(
        closedShift("shift-1", "driver-1", startedAt = 100L, synced = true),
        closedShift("shift-2", "driver-1", startedAt = 300L, synced = false),
        closedShift("shift-3", "driver-2", startedAt = 500L, synced = true)
    )
    private val tickets = listOf(
        ticket("ticket-1", "shift-1", "standard", 50, synced = true),
        ticket("ticket-2", "shift-1", "student", 30, synced = false),
        ticket("ticket-3", "shift-2", "standard", 50, synced = false),
        ticket("ticket-4", "shift-3", "student", 30, synced = true)
    )

    @Test
    fun `report aggregates drivers shifts tickets fares and cash`() {
        val report = buildReport()

        assertEquals(AdminReport.CURRENT_CONTRACT_VERSION, report.contractVersion)
        assertEquals(2, report.totals.driverCount)
        assertEquals(3, report.totals.shiftCount)
        assertEquals(4, report.totals.ticketCount)
        assertEquals(160, report.totals.cashTotalCents)
        assertEquals(2, report.drivers.first { it.driverId == "driver-1" }.shiftCount)
        assertEquals(3, report.drivers.first { it.driverId == "driver-1" }.ticketCount)
        assertEquals(2, report.totals.fareTypeSummaries.size)
        assertEquals(2, report.shifts.first { it.shiftId == "shift-1" }.tickets.size)
    }

    @Test
    fun `report derives synced partial and pending shift states`() {
        val report = buildReport()

        assertEquals(
            ReportingSyncStatus.PARTIALLY_SYNCED,
            report.shifts.first { it.shiftId == "shift-1" }.syncStatus
        )
        assertEquals(
            ReportingSyncStatus.PENDING,
            report.shifts.first { it.shiftId == "shift-2" }.syncStatus
        )
        assertEquals(
            ReportingSyncStatus.SYNCED,
            report.shifts.first { it.shiftId == "shift-3" }.syncStatus
        )
        assertEquals(1, report.totals.syncedShiftCount)
        assertEquals(1, report.totals.partiallySyncedShiftCount)
        assertEquals(1, report.totals.pendingShiftCount)
    }

    @Test
    fun `driver and date filters select matching closed shifts`() {
        val report = buildReport(
            AdminReportFilter(
                fromStartedAtMillis = 200L,
                toStartedAtMillis = 400L,
                driverId = "driver-1"
            )
        )

        assertEquals(listOf("shift-2"), report.shifts.map(ShiftReport::shiftId))
        assertEquals(1, report.totals.shiftCount)
        assertEquals(1, report.totals.ticketCount)
        assertEquals(50, report.totals.cashTotalCents)
    }

    @Test
    fun `fare filter keeps only matching ticket detail and totals`() {
        val report = buildReport(AdminReportFilter(fareTypeId = "student"))

        assertEquals(2, report.totals.shiftCount)
        assertEquals(2, report.totals.ticketCount)
        assertEquals(60, report.totals.cashTotalCents)
        assertTrue(report.shifts.all { shift ->
            shift.tickets.all { it.fareTypeId == "student" }
        })
    }

    @Test
    fun `unmatched legacy tickets are excluded from totals and reported as data quality`() {
        val orphan = ticket("legacy", "missing-shift", "standard", 50, synced = false)

        val report = buildAdminReport(
            closedShifts = shifts,
            tickets = tickets + orphan,
            drivers = drivers,
            buses = buses,
            routes = routes,
            fareTypes = fares,
            generatedAtMillis = 1_000L
        )

        assertEquals(4, report.totals.ticketCount)
        assertEquals(1, report.dataQuality.unmatchedTicketCount)
        assertEquals(50, report.dataQuality.unmatchedTicketCashCents)
    }

    @Test
    fun `unknown transport references are visible in report and quality counters`() {
        val unknownShift = closedShift(
            id = "unknown-shift",
            driverId = "unknown-driver",
            startedAt = 700L,
            synced = false
        ).copy(busId = "unknown-bus", routeId = "unknown-route")

        val report = buildAdminReport(
            closedShifts = shifts + unknownShift,
            tickets = tickets,
            drivers = drivers,
            buses = buses,
            routes = routes,
            fareTypes = fares,
            generatedAtMillis = 1_000L
        )

        val shiftReport = report.shifts.first { it.shiftId == unknownShift.id }
        assertEquals("Unknown driver", shiftReport.driverName)
        assertEquals("Unknown bus", shiftReport.busPlateNumber)
        assertEquals("Unknown route", shiftReport.routeName)
        assertEquals(1, report.dataQuality.unknownDriverShiftCount)
        assertEquals(1, report.dataQuality.unknownBusShiftCount)
        assertEquals(1, report.dataQuality.unknownRouteShiftCount)
    }

    private fun buildReport(filter: AdminReportFilter = AdminReportFilter()): AdminReport {
        return buildAdminReport(
            closedShifts = shifts,
            tickets = tickets,
            drivers = drivers,
            buses = buses,
            routes = routes,
            fareTypes = fares,
            filter = filter,
            generatedAtMillis = 1_000L
        )
    }

    private fun closedShift(
        id: String,
        driverId: String,
        startedAt: Long,
        synced: Boolean
    ) = Shift(
        id = id,
        driverId = driverId,
        busId = "bus-1",
        routeId = "route-1",
        startedAtMillis = startedAt,
        endedAtMillis = startedAt + 100L,
        synced = synced
    )

    private fun ticket(
        id: String,
        shiftId: String,
        fareTypeId: String,
        priceCents: Int,
        synced: Boolean
    ) = Ticket(
        id = id,
        shiftId = shiftId,
        fareTypeId = fareTypeId,
        priceCents = priceCents,
        soldAtMillis = 1L,
        synced = synced,
        printStatus = TicketPrintStatus.PRINTED
    )
}
