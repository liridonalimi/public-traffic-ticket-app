package com.buspay.app.domain

data class AdminReportFilter(
    val fromStartedAtMillis: Long? = null,
    val toStartedAtMillis: Long? = null,
    val driverId: String? = null,
    val busId: String? = null,
    val routeId: String? = null,
    val fareTypeId: String? = null
)

enum class ReportingSyncStatus {
    PENDING,
    PARTIALLY_SYNCED,
    SYNCED
}

data class TicketReport(
    val ticketId: String,
    val shiftId: String,
    val fareTypeId: String,
    val fareName: String,
    val priceCents: Int,
    val soldAtMillis: Long,
    val synced: Boolean,
    val printStatus: TicketPrintStatus
)

data class ShiftReport(
    val shiftId: String,
    val driverId: String,
    val driverName: String,
    val busId: String,
    val busPlateNumber: String,
    val routeId: String,
    val routeName: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationMillis: Long,
    val tickets: List<TicketReport>,
    val ticketCount: Int,
    val cashTotalCents: Int,
    val fareTypeSummaries: List<FareTypeSummary>,
    val syncStatus: ReportingSyncStatus
)

data class DriverReport(
    val driverId: String,
    val driverName: String,
    val shiftCount: Int,
    val ticketCount: Int,
    val cashTotalCents: Int,
    val shiftIds: List<String>
)

data class AdminReportTotals(
    val driverCount: Int,
    val shiftCount: Int,
    val ticketCount: Int,
    val cashTotalCents: Int,
    val syncedShiftCount: Int,
    val partiallySyncedShiftCount: Int,
    val pendingShiftCount: Int,
    val fareTypeSummaries: List<FareTypeSummary>
)

data class AdminReportDataQuality(
    val unmatchedTicketCount: Int,
    val unmatchedTicketCashCents: Int,
    val unknownDriverShiftCount: Int,
    val unknownBusShiftCount: Int,
    val unknownRouteShiftCount: Int
)

data class AdminReport(
    val contractVersion: Int,
    val generatedAtMillis: Long,
    val filter: AdminReportFilter,
    val totals: AdminReportTotals,
    val drivers: List<DriverReport>,
    val shifts: List<ShiftReport>,
    val dataQuality: AdminReportDataQuality
) {
    companion object {
        const val CURRENT_CONTRACT_VERSION = 1
    }
}

fun buildAdminReport(
    closedShifts: List<Shift>,
    tickets: List<Ticket>,
    drivers: List<Driver>,
    buses: List<Bus>,
    routes: List<Route>,
    fareTypes: List<FareType>,
    filter: AdminReportFilter = AdminReportFilter(),
    generatedAtMillis: Long = System.currentTimeMillis()
): AdminReport {
    val driversById = drivers.associateBy(Driver::id)
    val busesById = buses.associateBy(Bus::id)
    val routesById = routes.associateBy(Route::id)
    val faresById = fareTypes.associateBy(FareType::id)
    val allClosedShiftIds = closedShifts.mapTo(mutableSetOf(), Shift::id)
    val unmatchedTickets = tickets.filter { it.shiftId !in allClosedShiftIds }
    val ticketsByShiftId = tickets.groupBy(Ticket::shiftId)

    val shiftReports = closedShifts
        .asSequence()
        .filter { it.endedAtMillis != null }
        .filter { shift -> filter.fromStartedAtMillis?.let { shift.startedAtMillis >= it } ?: true }
        .filter { shift -> filter.toStartedAtMillis?.let { shift.startedAtMillis <= it } ?: true }
        .filter { shift -> filter.driverId?.let { shift.driverId == it } ?: true }
        .filter { shift -> filter.busId?.let { shift.busId == it } ?: true }
        .filter { shift -> filter.routeId?.let { shift.routeId == it } ?: true }
        .mapNotNull { shift ->
            val shiftTickets = ticketsByShiftId[shift.id].orEmpty()
                .filter { ticket ->
                    filter.fareTypeId?.let { ticket.fareTypeId == it } ?: true
                }
                .sortedBy(Ticket::soldAtMillis)
            if (filter.fareTypeId != null && shiftTickets.isEmpty()) return@mapNotNull null

            val ticketReports = shiftTickets.map { ticket ->
                TicketReport(
                    ticketId = ticket.id,
                    shiftId = ticket.shiftId,
                    fareTypeId = ticket.fareTypeId,
                    fareName = faresById[ticket.fareTypeId]?.name ?: "Unknown fare",
                    priceCents = ticket.priceCents,
                    soldAtMillis = ticket.soldAtMillis,
                    synced = ticket.synced,
                    printStatus = ticket.printStatus
                )
            }
            val syncStatus = when {
                shift.synced && shiftTickets.all(Ticket::synced) -> ReportingSyncStatus.SYNCED
                shift.synced || shiftTickets.any(Ticket::synced) ->
                    ReportingSyncStatus.PARTIALLY_SYNCED
                else -> ReportingSyncStatus.PENDING
            }
            val endedAtMillis = requireNotNull(shift.endedAtMillis)

            ShiftReport(
                shiftId = shift.id,
                driverId = shift.driverId,
                driverName = driversById[shift.driverId]?.name ?: "Unknown driver",
                busId = shift.busId,
                busPlateNumber = busesById[shift.busId]?.plateNumber ?: "Unknown bus",
                routeId = shift.routeId,
                routeName = routesById[shift.routeId]?.name ?: "Unknown route",
                startedAtMillis = shift.startedAtMillis,
                endedAtMillis = endedAtMillis,
                durationMillis = (endedAtMillis - shift.startedAtMillis).coerceAtLeast(0L),
                tickets = ticketReports,
                ticketCount = shiftTickets.size,
                cashTotalCents = shiftTickets.sumOf(Ticket::priceCents),
                fareTypeSummaries = summarizeTicketsByFare(shiftTickets, fareTypes),
                syncStatus = syncStatus
            )
        }
        .sortedByDescending(ShiftReport::startedAtMillis)
        .toList()

    val driverReports = shiftReports
        .groupBy(ShiftReport::driverId)
        .map { (driverId, reports) ->
            DriverReport(
                driverId = driverId,
                driverName = reports.first().driverName,
                shiftCount = reports.size,
                ticketCount = reports.sumOf(ShiftReport::ticketCount),
                cashTotalCents = reports.sumOf(ShiftReport::cashTotalCents),
                shiftIds = reports.map(ShiftReport::shiftId)
            )
        }
        .sortedBy(DriverReport::driverName)
    val reportTickets = shiftReports.flatMap(ShiftReport::tickets)
    val reportDomainTickets = reportTickets.map { report ->
        Ticket(
            id = report.ticketId,
            shiftId = report.shiftId,
            fareTypeId = report.fareTypeId,
            priceCents = report.priceCents,
            soldAtMillis = report.soldAtMillis,
            synced = report.synced,
            printStatus = report.printStatus
        )
    }

    return AdminReport(
        contractVersion = AdminReport.CURRENT_CONTRACT_VERSION,
        generatedAtMillis = generatedAtMillis,
        filter = filter,
        totals = AdminReportTotals(
            driverCount = driverReports.size,
            shiftCount = shiftReports.size,
            ticketCount = shiftReports.sumOf(ShiftReport::ticketCount),
            cashTotalCents = shiftReports.sumOf(ShiftReport::cashTotalCents),
            syncedShiftCount = shiftReports.count { it.syncStatus == ReportingSyncStatus.SYNCED },
            partiallySyncedShiftCount = shiftReports.count {
                it.syncStatus == ReportingSyncStatus.PARTIALLY_SYNCED
            },
            pendingShiftCount = shiftReports.count { it.syncStatus == ReportingSyncStatus.PENDING },
            fareTypeSummaries = summarizeTicketsByFare(reportDomainTickets, fareTypes)
        ),
        drivers = driverReports,
        shifts = shiftReports,
        dataQuality = AdminReportDataQuality(
            unmatchedTicketCount = unmatchedTickets.size,
            unmatchedTicketCashCents = unmatchedTickets.sumOf(Ticket::priceCents),
            unknownDriverShiftCount = closedShifts.count { it.driverId !in driversById },
            unknownBusShiftCount = closedShifts.count { it.busId !in busesById },
            unknownRouteShiftCount = closedShifts.count { it.routeId !in routesById }
        )
    )
}
