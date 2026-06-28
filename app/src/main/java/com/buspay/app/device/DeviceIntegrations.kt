package com.buspay.app.device

interface GpsTracker {
    fun start()
    fun stop()
}

interface TicketPrinter {
    fun printTicket(ticketCode: String, priceCents: Int)
}

interface StopRequestInput {
    fun listen(onStopRequested: () -> Unit)
}
