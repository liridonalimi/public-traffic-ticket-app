package com.buspay.app.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class SyncBatch(
    val requestId: String,
    val shifts: List<Shift>,
    val tickets: List<Ticket>
)

data class SyncAcknowledgement(
    val acknowledgedShiftIds: Set<String>,
    val acknowledgedTicketIds: Set<String>
)

sealed interface SyncResult {
    data class Success(val acknowledgement: SyncAcknowledgement) : SyncResult
    data class Failure(val message: String) : SyncResult
}

fun createSyncBatch(shifts: List<Shift>, tickets: List<Ticket>): SyncBatch {
    val entityKeys = buildList {
        shifts.forEach { add("shift:${it.id}") }
        tickets.forEach { add("ticket:${it.id}") }
    }.sorted()
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(entityKeys.joinToString("|").toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    return SyncBatch(
        requestId = "sync-${digest.take(16)}",
        shifts = shifts,
        tickets = tickets
    )
}

fun acknowledgeShifts(
    shifts: List<Shift>,
    acknowledgedIds: Set<String>
): List<Shift> = shifts.map { shift ->
    if (shift.id in acknowledgedIds) shift.copy(synced = true) else shift
}

fun acknowledgeTickets(
    tickets: List<Ticket>,
    acknowledgedIds: Set<String>
): List<Ticket> = tickets.map { ticket ->
    if (ticket.id in acknowledgedIds) ticket.copy(synced = true) else ticket
}
