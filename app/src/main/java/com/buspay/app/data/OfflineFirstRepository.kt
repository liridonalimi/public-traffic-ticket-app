package com.buspay.app.data

import com.buspay.app.domain.Ticket

class OfflineFirstRepository {
    private val pendingTickets = mutableListOf<Ticket>()

    fun saveTicket(ticket: Ticket) {
        pendingTickets += ticket
    }

    fun pendingTicketCount(): Int = pendingTickets.count { !it.synced }
}
