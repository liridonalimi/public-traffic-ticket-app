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
    val order: Int
)

data class Shift(
    val id: String,
    val driverId: String,
    val busId: String,
    val routeId: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null
)

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

data class FareType(
    val id: String,
    val name: String,
    val priceCents: Int,
    val eligibility: String? = null
)

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
    val lastPrintError: String? = null
) {
    companion object {
        const val STANDARD_FARE_TYPE_ID = "standard"
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
    val fareTypeSummaries: List<FareTypeSummary> = emptyList()
)

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
    currentStopIndex: Int
): Int {
    if (stops.isEmpty()) return 0
    val safeCurrentIndex = currentStopIndex.coerceIn(0, stops.lastIndex)

    return stops.indices
        .filter { it >= safeCurrentIndex }
        .minByOrNull { index ->
            distanceMeters(
                latitude1 = latitude,
                longitude1 = longitude,
                latitude2 = stops[index].latitude,
                longitude2 = stops[index].longitude
            )
        }
        ?: safeCurrentIndex
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
