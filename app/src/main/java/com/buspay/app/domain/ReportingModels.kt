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
    val printStatus: TicketPrintStatus,
    val effectiveFareTypeId: String,
    val effectivePriceCents: Int,
    val revenueStatus: String
)

data class TicketActionReport(
    val actionId: String,
    val originalTicketId: String,
    val actionType: TicketActionType,
    val reason: TicketActionReason,
    val supervisorId: String,
    val authorizedAtMillis: Long,
    val correctedFareTypeId: String?,
    val correctedPriceCents: Int?,
    val synced: Boolean
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
    val grossCashTotalCents: Int,
    val expectedCashCents: Int?,
    val declaredCashCents: Int?,
    val cashVarianceCents: Int?,
    val cashReconciliationStatus: CashReconciliationStatus,
    val fareTypeSummaries: List<FareTypeSummary>,
    val syncStatus: ReportingSyncStatus,
    val ticketActions: List<TicketActionReport>
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
    val expectedCashTotalCents: Int,
    val declaredCashTotalCents: Int,
    val cashVarianceTotalCents: Int,
    val reconciledShiftCount: Int,
    val unreconciledShiftCount: Int,
    val voidCount: Int,
    val correctionCount: Int,
    val reprintCount: Int,
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
    generatedAtMillis: Long = System.currentTimeMillis(),
    ticketActions: List<TicketAction> = emptyList()
): AdminReport {
    val driversById = drivers.associateBy(Driver::id)
    val busesById = buses.associateBy(Bus::id)
    val routesById = routes.associateBy(Route::id)
    val faresById = fareTypes.associateBy(FareType::id)
    val allClosedShiftIds = closedShifts.mapTo(mutableSetOf(), Shift::id)
    val unmatchedTickets = tickets.filter { it.shiftId !in allClosedShiftIds }
    val ticketsByShiftId = tickets.groupBy(Ticket::shiftId)
    val actionsByShiftId = ticketActions.groupBy(TicketAction::shiftId)

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
            val shiftActions = actionsByShiftId[shift.id].orEmpty().sortedBy(TicketAction::createdAtMillis)
            if (filter.fareTypeId != null && shiftTickets.isEmpty()) return@mapNotNull null

            val ticketReports = shiftTickets.map { ticket ->
                val financialAction = shiftActions.firstOrNull {
                    it.originalTicketId == ticket.id &&
                        it.actionType in setOf(TicketActionType.VOID, TicketActionType.CORRECTION)
                }
                TicketReport(
                    ticketId = ticket.id,
                    shiftId = ticket.shiftId,
                    fareTypeId = ticket.fareTypeId,
                    fareName = faresById[ticket.fareTypeId]?.name ?: "Unknown fare",
                    priceCents = ticket.priceCents,
                    soldAtMillis = ticket.soldAtMillis,
                    synced = ticket.synced,
                    printStatus = ticket.printStatus,
                    effectiveFareTypeId = financialAction?.correctedFareTypeId ?: ticket.fareTypeId,
                    effectivePriceCents = ticketRevenueCents(ticket, shiftActions),
                    revenueStatus = financialAction?.actionType?.name ?: "SALE"
                )
            }
            val syncStatus = when {
                shift.synced && shiftTickets.all(Ticket::synced) && shiftActions.all(TicketAction::synced) ->
                    ReportingSyncStatus.SYNCED
                shift.synced || shiftTickets.any(Ticket::synced) || shiftActions.any(TicketAction::synced) ->
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
                cashTotalCents = ticketReports.sumOf(TicketReport::effectivePriceCents),
                grossCashTotalCents = shiftTickets.sumOf(Ticket::priceCents),
                expectedCashCents = shift.expectedCashCents,
                declaredCashCents = shift.declaredCashCents,
                cashVarianceCents = shift.cashVarianceCents,
                cashReconciliationStatus = shift.cashReconciliationStatus,
                fareTypeSummaries = summarizeTicketsByFare(shiftTickets, fareTypes),
                syncStatus = syncStatus,
                ticketActions = shiftActions.map { action -> TicketActionReport(
                    actionId = action.id,
                    originalTicketId = action.originalTicketId,
                    actionType = action.actionType,
                    reason = action.reason,
                    supervisorId = action.supervisorId,
                    authorizedAtMillis = action.authorizedAtMillis,
                    correctedFareTypeId = action.correctedFareTypeId,
                    correctedPriceCents = action.correctedPriceCents,
                    synced = action.synced
                ) }
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
            fareTypeId = report.effectiveFareTypeId,
            priceCents = report.effectivePriceCents,
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
            expectedCashTotalCents = shiftReports.sumOf { report ->
                report.expectedCashCents ?: report.cashTotalCents
            },
            declaredCashTotalCents = shiftReports.sumOf { it.declaredCashCents ?: 0 },
            cashVarianceTotalCents = shiftReports.sumOf { it.cashVarianceCents ?: 0 },
            reconciledShiftCount = shiftReports.count {
                it.cashReconciliationStatus != CashReconciliationStatus.NOT_RECORDED
            },
            unreconciledShiftCount = shiftReports.count {
                it.cashReconciliationStatus == CashReconciliationStatus.NOT_RECORDED
            },
            voidCount = ticketActions.count { it.actionType == TicketActionType.VOID },
            correctionCount = ticketActions.count { it.actionType == TicketActionType.CORRECTION },
            reprintCount = ticketActions.count { it.actionType == TicketActionType.REPRINT },
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
