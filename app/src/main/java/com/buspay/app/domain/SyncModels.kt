package com.buspay.app.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class SyncBatch(
    val requestId: String,
    val shifts: List<Shift>,
    val tickets: List<Ticket>,
    val ticketActions: List<TicketAction> = emptyList()
)

data class SyncAcknowledgement(
    val acknowledgedShiftIds: Set<String>,
    val acknowledgedTicketIds: Set<String>,
    val acknowledgedTicketActionIds: Set<String> = emptySet()
)

sealed interface SyncResult {
    data class Success(val acknowledgement: SyncAcknowledgement) : SyncResult
    data class Failure(val message: String) : SyncResult
}

data class SyncRecords(
    val shifts: List<Shift>,
    val tickets: List<Ticket>,
    val ticketActions: List<TicketAction>
)

/**
 * A pending action always carries its immutable original ticket and closed shift.
 * This repairs the legitimate case where a record was acknowledged by demo mode
 * but has never been inserted into the currently configured real server.
 */
fun collectSyncRecords(
    pendingShifts: List<Shift>,
    pendingTickets: List<Ticket>,
    pendingTicketActions: List<TicketAction>,
    allClosedShifts: List<Shift>,
    allTickets: List<Ticket>
): SyncRecords {
    val requiredTicketIds = pendingTicketActions.mapTo(mutableSetOf(), TicketAction::originalTicketId)
    val actionTickets = allTickets.filter { it.id in requiredTicketIds }
    val requiredShiftIds = buildSet {
        pendingTicketActions.forEach { add(it.shiftId) }
        actionTickets.forEach { add(it.shiftId) }
    }
    return SyncRecords(
        shifts = (pendingShifts + allClosedShifts.filter { it.id in requiredShiftIds })
            .distinctBy(Shift::id),
        tickets = (pendingTickets + actionTickets).distinctBy(Ticket::id),
        ticketActions = pendingTicketActions
    )
}

fun createSyncBatch(
    shifts: List<Shift>,
    tickets: List<Ticket>,
    ticketActions: List<TicketAction> = emptyList()
): SyncBatch {
    val entityKeys = buildList {
        shifts.forEach { add("shift:${it.id}") }
        tickets.forEach { add("ticket:${it.id}") }
        ticketActions.forEach { add("ticket-action:${it.id}") }
    }.sorted()
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(entityKeys.joinToString("|").toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    return SyncBatch(
        requestId = "sync-${digest.take(16)}",
        shifts = shifts,
        tickets = tickets,
        ticketActions = ticketActions
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

fun acknowledgeTicketActions(
    actions: List<TicketAction>,
    acknowledgedIds: Set<String>
): List<TicketAction> = actions.map { action ->
    if (action.id in acknowledgedIds) action.copy(synced = true) else action
}
