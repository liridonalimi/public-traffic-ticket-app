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

data class Ticket(
    val id: String,
    val shiftId: String,
    val priceCents: Int,
    val soldAtMillis: Long,
    val synced: Boolean
)
