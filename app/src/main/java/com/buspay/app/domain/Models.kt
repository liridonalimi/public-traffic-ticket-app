package com.buspay.app.domain

data class Driver(
    val id: String,
    val name: String
)

data class Bus(
    val id: String,
    val plateNumber: String
)

data class Route(
    val id: String,
    val name: String,
    val stops: List<Stop>
)

data class Stop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val zoneId: String = "1"
)

enum class TripDirection { OUTBOUND, INBOUND }

data class ServiceCalendar(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val activeWeekdays: Set<Int>
)

data class ScheduledStopTime(
    val stopId: String,
    val arrivalMinutes: Int,
    val departureMinutes: Int
)

data class ScheduledTrip(
    val id: String,
    val routeId: String,
    val serviceCalendarId: String,
    val departureMinutes: Int,
    val direction: TripDirection,
    val stopTimes: List<ScheduledStopTime>
) {
    val endMinutes: Int get() = stopTimes.maxOfOrNull(ScheduledStopTime::departureMinutes)
        ?: departureMinutes
}

data class TripAssignment(
    val id: String,
    val tripId: String,
    val serviceDate: String,
    val driverId: String,
    val busId: String
)

data class DriverDuty(
    val assignment: TripAssignment,
    val trip: ScheduledTrip,
    val route: Route,
    val bus: Bus
)

data class Shift(
    val id: String,
    val driverId: String,
    val busId: String,
    val routeId: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val synced: Boolean = false,
    val expectedCashCents: Int? = null,
    val declaredCashCents: Int? = null,
    val reconciledAtMillis: Long? = null,
    val scheduledTripId: String? = null,
    val assignmentId: String? = null
) {
    val cashVarianceCents: Int? = expectedCashCents?.let { expected ->
        declaredCashCents?.minus(expected)
    }

    val cashReconciliationStatus: CashReconciliationStatus
        get() = when {
            expectedCashCents == null || declaredCashCents == null || reconciledAtMillis == null ->
                CashReconciliationStatus.NOT_RECORDED
            declaredCashCents < expectedCashCents -> CashReconciliationStatus.SHORTAGE
            declaredCashCents > expectedCashCents -> CashReconciliationStatus.SURPLUS
            else -> CashReconciliationStatus.MATCHED
        }
}

enum class CashReconciliationStatus {
    NOT_RECORDED,
    MATCHED,
    SHORTAGE,
    SURPLUS
}

enum class RouteProgressSource {
    SHIFT_START,
    GPS,
    MANUAL
}

data class RouteProgress(
    val shiftId: String,
    val currentStopIndex: Int,
    val updatedAtMillis: Long,
    val source: RouteProgressSource
)

data class RouteStopStatus(
    val currentStop: Stop?,
    val nextStop: Stop?,
    val isRouteComplete: Boolean
)

data class StopRequest(
    val shiftId: String,
    val requestedStopIndex: Int,
    val requestedAtMillis: Long
)

data class FareType(
    val id: String,
    val name: String,
    val priceCents: Int,
    val eligibility: String? = null,
    val additionalZoneCents: Int = 0,
    val offPeakDiscountCents: Int = 0,
    val offPeakStartMinutes: Int? = null,
    val offPeakEndMinutes: Int? = null,
    val transferWindowMinutes: Int = 0,
    val routeId: String? = null
)

data class FareQuote(
    val priceCents: Int,
    val zoneCount: Int,
    val offPeakApplied: Boolean,
    val transferValidUntilMillis: Long?
)

fun calculateFareQuote(
    fareType: FareType,
    route: Route,
    originStop: Stop,
    destinationStop: Stop,
    soldAtMillis: Long,
    minuteOfDay: Int
): FareQuote {
    val orderedStops = route.stops.sortedBy(Stop::order)
    val originIndex = orderedStops.indexOfFirst { it.id == originStop.id }.coerceAtLeast(0)
    val destinationIndex = orderedStops.indexOfFirst { it.id == destinationStop.id }
        .coerceAtLeast(originIndex)
    val zoneCount = orderedStops.subList(originIndex, destinationIndex + 1)
        .map(Stop::zoneId).distinct().size.coerceAtLeast(1)
    val offPeakApplied = fareType.offPeakStartMinutes?.let { start ->
        fareType.offPeakEndMinutes?.let { end ->
            if (start <= end) minuteOfDay in start until end
            else minuteOfDay >= start || minuteOfDay < end
        }
    } ?: false
    val price = fareType.priceCents + ((zoneCount - 1) * fareType.additionalZoneCents) -
        if (offPeakApplied) fareType.offPeakDiscountCents else 0
    return FareQuote(
        priceCents = price.coerceAtLeast(0),
        zoneCount = zoneCount,
        offPeakApplied = offPeakApplied,
        transferValidUntilMillis = fareType.transferWindowMinutes.takeIf { it > 0 }
            ?.let { soldAtMillis + (it * 60_000L) }
    )
}

data class ManagedCatalog(
    val revision: Int,
    val updatedAtMillis: Long,
    val drivers: List<Driver>,
    val buses: List<Bus>,
    val routes: List<Route>,
    val fareTypes: List<FareType>,
    val serviceCalendars: List<ServiceCalendar> = emptyList(),
    val scheduledTrips: List<ScheduledTrip> = emptyList(),
    val tripAssignments: List<TripAssignment> = emptyList()
)

fun ManagedCatalog.isOperationallyValid(): Boolean {
    if (revision < 1 || drivers.isEmpty() || buses.isEmpty() || routes.isEmpty() || fareTypes.isEmpty()) {
        return false
    }
    if (routes.any { it.stops.isEmpty() }) return false
    if (listOf(
            drivers.map(Driver::id),
            buses.map(Bus::id),
            routes.map(Route::id),
            fareTypes.map(FareType::id)
        ).any { values -> values.size != values.toSet().size }
    ) {
        return false
    }
    val stops = routes.flatMap(Route::stops)
    return stops.map(Stop::id).let { it.size == it.toSet().size } &&
        stops.all { it.zoneId.isNotBlank() } &&
        routes.all { route ->
            route.stops.map(Stop::order).let { orders ->
                orders.all { it >= 1 } && orders.size == orders.toSet().size
            }
        } &&
        fareTypes.all {
            it.priceCents >= 0 && it.additionalZoneCents >= 0 &&
                it.offPeakDiscountCents >= 0 && it.transferWindowMinutes >= 0 &&
                (it.routeId == null || routes.any { route -> route.id == it.routeId }) &&
                ((it.offPeakStartMinutes == null && it.offPeakEndMinutes == null) ||
                    (it.offPeakStartMinutes in 0..1439 && it.offPeakEndMinutes in 0..1439 &&
                        it.offPeakStartMinutes != it.offPeakEndMinutes))
        } && routes.all { route ->
            fareTypes.any { fare -> fare.routeId == null || fare.routeId == route.id }
        } && isScheduleValid()
}

private fun ManagedCatalog.isScheduleValid(): Boolean {
    if (listOf(
            serviceCalendars.map(ServiceCalendar::id),
            scheduledTrips.map(ScheduledTrip::id),
            tripAssignments.map(TripAssignment::id)
        ).any { it.size != it.toSet().size }
    ) return false
    val routeById = routes.associateBy(Route::id)
    val calendars = serviceCalendars.associateBy(ServiceCalendar::id)
    val trips = scheduledTrips.associateBy(ScheduledTrip::id)
    if (serviceCalendars.any {
            !it.startDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
                !it.endDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
                it.startDate > it.endDate || it.activeWeekdays.isEmpty() ||
                it.activeWeekdays.any { day -> day !in 1..7 }
        }
    ) return false
    if (scheduledTrips.any { trip ->
            val route = routeById[trip.routeId]
            route == null || trip.serviceCalendarId !in calendars || trip.departureMinutes !in 0..1439 ||
                trip.stopTimes.map(ScheduledStopTime::stopId) != route.stops.sortedBy(Stop::order).map(Stop::id) ||
                trip.stopTimes.any { it.arrivalMinutes !in 0..2879 || it.departureMinutes < it.arrivalMinutes } ||
                trip.stopTimes.zipWithNext().any { (left, right) -> right.arrivalMinutes < left.departureMinutes }
        }
    ) return false
    if (tripAssignments.any { assignment ->
            val trip = trips[assignment.tripId]
            val calendar = trip?.let { calendars[it.serviceCalendarId] }
            trip == null || assignment.driverId !in drivers.map(Driver::id) ||
                assignment.busId !in buses.map(Bus::id) || calendar == null ||
                assignment.serviceDate < calendar.startDate || assignment.serviceDate > calendar.endDate
        }
    ) return false
    return tripAssignments.groupBy(TripAssignment::serviceDate).values.all { assignments ->
        assignments.indices.all { leftIndex -> assignments.indices.drop(leftIndex + 1).all { rightIndex ->
            val left = assignments[leftIndex]
            val right = assignments[rightIndex]
            val leftTrip = requireNotNull(trips[left.tripId])
            val rightTrip = requireNotNull(trips[right.tripId])
            val overlaps = leftTrip.departureMinutes < rightTrip.endMinutes &&
                rightTrip.departureMinutes < leftTrip.endMinutes
            !overlaps || (left.driverId != right.driverId && left.busId != right.busId)
        } }
    }
}

fun ManagedCatalog.dutiesForDriver(driverId: String): List<DriverDuty> {
    val trips = scheduledTrips.associateBy(ScheduledTrip::id)
    val routesById = routes.associateBy(Route::id)
    val busesById = buses.associateBy(Bus::id)
    return tripAssignments.filter { it.driverId == driverId }.mapNotNull { assignment ->
        val trip = trips[assignment.tripId] ?: return@mapNotNull null
        DriverDuty(assignment, trip, routesById[trip.routeId] ?: return@mapNotNull null,
            busesById[assignment.busId] ?: return@mapNotNull null)
    }.sortedWith(compareBy({ it.assignment.serviceDate }, { it.trip.departureMinutes }, { it.assignment.id }))
}

enum class TicketPrintStatus {
    PENDING,
    PRINTED,
    FAILED
}

data class Ticket(
    val id: String,
    val shiftId: String,
    val fareTypeId: String = STANDARD_FARE_TYPE_ID,
    val priceCents: Int,
    val soldAtMillis: Long,
    val synced: Boolean,
    val printStatus: TicketPrintStatus = TicketPrintStatus.PENDING,
    val printAttempts: Int = 0,
    val lastPrintError: String? = null,
    val farePolicyRevision: Int? = null,
    val originStopId: String? = null,
    val destinationStopId: String? = null,
    val zoneCount: Int? = null,
    val offPeakApplied: Boolean? = null,
    val transferValidUntilMillis: Long? = null
) {
    companion object {
        const val STANDARD_FARE_TYPE_ID = "standard"
    }
}

enum class TicketActionType {
    VOID,
    CORRECTION,
    REPRINT
}

enum class TicketActionReason {
    WRONG_FARE,
    WRONG_DESTINATION,
    DUPLICATE_SALE,
    PASSENGER_REQUEST,
    DAMAGED_PRINT,
    PRINTER_FAILURE,
    OTHER
}

/**
 * Immutable post-sale evidence. The original ticket is never edited or deleted.
 * CORRECTION stores the replacement financial value in this event; REPRINT has no
 * revenue effect and remains distinguishable from a new ticket sale.
 */
data class TicketAction(
    val id: String,
    val originalTicketId: String,
    val shiftId: String,
    val actionType: TicketActionType,
    val reason: TicketActionReason,
    val supervisorId: String,
    val authorizedAtMillis: Long,
    val createdAtMillis: Long,
    val correctedFareTypeId: String? = null,
    val correctedPriceCents: Int? = null,
    val synced: Boolean = false
)

fun validateTicketAction(
    ticket: Ticket,
    existingActions: List<TicketAction>,
    action: TicketAction
): Boolean {
    if (action.originalTicketId != ticket.id || action.shiftId != ticket.shiftId) return false
    if (action.supervisorId.isBlank() || action.authorizedAtMillis < ticket.soldAtMillis ||
        action.createdAtMillis < action.authorizedAtMillis
    ) return false
    val hasFinancialAction = existingActions.any {
        it.originalTicketId == ticket.id &&
            it.actionType in setOf(TicketActionType.VOID, TicketActionType.CORRECTION)
    }
    if (action.actionType in setOf(TicketActionType.VOID, TicketActionType.CORRECTION) && hasFinancialAction) {
        return false
    }
    return when (action.actionType) {
        TicketActionType.CORRECTION ->
            !action.correctedFareTypeId.isNullOrBlank() &&
                action.correctedPriceCents != null && action.correctedPriceCents in 0..100_000
        TicketActionType.VOID, TicketActionType.REPRINT ->
            action.correctedFareTypeId == null && action.correctedPriceCents == null
    }
}

fun ticketRevenueCents(ticket: Ticket, actions: List<TicketAction>): Int {
    val financialAction = actions.firstOrNull {
        it.originalTicketId == ticket.id &&
            it.actionType in setOf(TicketActionType.VOID, TicketActionType.CORRECTION)
    }
    return when (financialAction?.actionType) {
        TicketActionType.VOID -> 0
        TicketActionType.CORRECTION -> financialAction.correctedPriceCents ?: ticket.priceCents
        else -> ticket.priceCents
    }
}

data class FareTypeSummary(
    val fareTypeId: String,
    val fareName: String,
    val ticketCount: Int,
    val cashTotalCents: Int
)

data class DriverShiftSummary(
    val ticketCount: Int,
    val cashTotalCents: Int,
    val fareTypeSummaries: List<FareTypeSummary> = emptyList(),
    val declaredCashCents: Int? = null,
    val cashVarianceCents: Int? = null,
    val cashReconciliationStatus: CashReconciliationStatus = CashReconciliationStatus.NOT_RECORDED
)

fun parseEuroAmountToCents(value: String, maximumCents: Int = 100_000_000): Int? {
    val normalized = value.trim().replace(',', '.')
    if (!normalized.matches(Regex("^(0|[1-9]\\d*)(\\.\\d{1,2})?$"))) return null
    val parts = normalized.split('.')
    val euros = parts[0].toLongOrNull() ?: return null
    val cents = when (val fraction = parts.getOrNull(1).orEmpty()) {
        "" -> 0
        else -> fraction.padEnd(2, '0').toInt()
    }
    val total = euros * 100L + cents
    return total.takeIf { it in 0..maximumCents.toLong() }?.toInt()
}

fun summarizeTicketsByFare(
    tickets: List<Ticket>,
    fareTypes: List<FareType>
): List<FareTypeSummary> {
    val faresById = fareTypes.associateBy(FareType::id)

    return tickets
        .groupBy(Ticket::fareTypeId)
        .map { (fareTypeId, fareTickets) ->
            FareTypeSummary(
                fareTypeId = fareTypeId,
                fareName = faresById[fareTypeId]?.name ?: "Unknown fare",
                ticketCount = fareTickets.size,
                cashTotalCents = fareTickets.sumOf(Ticket::priceCents)
            )
        }
        .sortedBy { summary ->
            fareTypes.indexOfFirst { it.id == summary.fareTypeId }
                .let { if (it == -1) Int.MAX_VALUE else it }
        }
}

fun nearestForwardStopIndex(
    stops: List<Stop>,
    latitude: Double,
    longitude: Double,
    currentStopIndex: Int,
    arrivalRadiusMeters: Double = 150.0
): Int {
    if (stops.isEmpty()) return 0
    val safeCurrentIndex = currentStopIndex.coerceIn(0, stops.lastIndex)

    val nearestForwardStop = stops.indices
        .filter { it >= safeCurrentIndex }
        .map { index ->
            index to distanceMeters(
                latitude1 = latitude,
                longitude1 = longitude,
                latitude2 = stops[index].latitude,
                longitude2 = stops[index].longitude
            )
        }
        .minByOrNull { (_, distance) -> distance }

    return nearestForwardStop
        ?.takeIf { (_, distance) -> distance <= arrivalRadiusMeters }
        ?.first
        ?: safeCurrentIndex
}

fun routeStopStatus(stops: List<Stop>, currentStopIndex: Int): RouteStopStatus {
    if (stops.isEmpty()) {
        return RouteStopStatus(
            currentStop = null,
            nextStop = null,
            isRouteComplete = false
        )
    }

    val safeCurrentIndex = currentStopIndex.coerceIn(0, stops.lastIndex)
    return RouteStopStatus(
        currentStop = stops[safeCurrentIndex],
        nextStop = stops.getOrNull(safeCurrentIndex + 1),
        isRouteComplete = safeCurrentIndex == stops.lastIndex
    )
}

fun isStopRequestReached(request: StopRequest, progress: RouteProgress): Boolean {
    return request.shiftId == progress.shiftId &&
        progress.currentStopIndex >= request.requestedStopIndex
}

private fun distanceMeters(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double
): Double {
    val latitudeDelta = Math.toRadians(latitude2 - latitude1)
    val longitudeDelta = Math.toRadians(longitude2 - longitude1)
    val startLatitude = Math.toRadians(latitude1)
    val endLatitude = Math.toRadians(latitude2)
    val haversine = kotlin.math.sin(latitudeDelta / 2).let { it * it } +
        kotlin.math.cos(startLatitude) * kotlin.math.cos(endLatitude) *
        kotlin.math.sin(longitudeDelta / 2).let { it * it }

    return 6_371_000.0 * 2 * kotlin.math.atan2(
        kotlin.math.sqrt(haversine),
        kotlin.math.sqrt(1 - haversine)
    )
}
