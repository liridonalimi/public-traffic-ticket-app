package com.buspay.app.data

import com.buspay.app.domain.SyncAcknowledgement
import com.buspay.app.domain.SyncBatch
import com.buspay.app.domain.SyncResult

interface TransitSyncClient {
    suspend fun sync(batch: SyncBatch): SyncResult
}

class DemoTransitSyncClient : TransitSyncClient {
    var isAvailable: Boolean = true

    override suspend fun sync(batch: SyncBatch): SyncResult {
        if (!isAvailable) {
            return SyncResult.Failure("Demo server is offline")
        }

        return SyncResult.Success(
            SyncAcknowledgement(
                acknowledgedShiftIds = batch.shifts.mapTo(mutableSetOf()) { it.id },
                acknowledgedTicketIds = batch.tickets.mapTo(mutableSetOf()) { it.id },
                acknowledgedTicketActionIds = batch.ticketActions.mapTo(mutableSetOf()) { it.id }
            )
        )
    }
}
