package com.buspay.app.data

import com.buspay.app.domain.Shift
import com.buspay.app.domain.SyncResult
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.createSyncBatch
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionTransitSyncClientTest {
    private val shift = Shift(
        id = "shift-1",
        driverId = "driver-1",
        busId = "bus-1",
        routeId = "route-1",
        startedAtMillis = 100L,
        endedAtMillis = 200L
    )
    private val ticket = Ticket(
        id = "ticket-1",
        shiftId = shift.id,
        fareTypeId = "student",
        priceCents = 30,
        soldAtMillis = 150L,
        synced = false
    )

    @Test
    fun `configuration accepts only secure endpoint and bearer token`() {
        assertFailsConfiguration("http://sync.example.test/v1/batches", "token")
        assertFailsConfiguration("https://sync.example.test/v1/batches", " ")
        assertFailsConfiguration("https://user@sync.example.test/v1/batches", "token")

        val config = ProductionSyncConfig(
            endpointUrl = "https://sync.example.test/v1/batches",
            accessToken = "token"
        )

        assertEquals("https://sync.example.test/v1/batches", config.endpointUrl)
    }

    @Test
    fun `request uses authentication idempotency and contract version`() = runBlocking {
        val transport = RecordingTransport { request ->
            SyncHttpResponse(statusCode = 200, body = acknowledgement(requestId = "sync-placeholder"))
        }
        val batch = createSyncBatch(listOf(shift), listOf(ticket))
        transport.responseFactory = {
            SyncHttpResponse(statusCode = 200, body = acknowledgement(batch.requestId))
        }
        val client = client(transport)

        val result = client.sync(batch)

        assertTrue(result is SyncResult.Success)
        val request = requireNotNull(transport.lastRequest)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals(batch.requestId, request.headers["Idempotency-Key"])
        assertEquals("1", request.headers["X-BusPay-Contract-Version"])
        assertTrue(request.body.contains("\"contractVersion\":1"))
        assertTrue(request.body.contains("\"driverId\":\"driver-1\""))
        assertTrue(request.body.contains("\"fareTypeId\":\"student\""))
        assertFalse(request.body.contains("test-token"))
    }

    @Test
    fun `valid acknowledgement returns only confirmed records`() = runBlocking {
        val batch = createSyncBatch(listOf(shift), listOf(ticket))
        val transport = RecordingTransport {
            SyncHttpResponse(
                statusCode = 200,
                body = acknowledgement(
                    requestId = batch.requestId,
                    shiftIds = listOf(shift.id),
                    ticketIds = listOf(ticket.id)
                )
            )
        }

        val result = client(transport).sync(batch)

        assertTrue(result is SyncResult.Success)
        val confirmed = (result as SyncResult.Success).acknowledgement
        assertEquals(setOf(shift.id), confirmed.acknowledgedShiftIds)
        assertEquals(setOf(ticket.id), confirmed.acknowledgedTicketIds)
    }

    @Test
    fun `mismatched request or unknown acknowledgement is rejected`() = runBlocking {
        val batch = createSyncBatch(listOf(shift), listOf(ticket))
        val wrongRequest = client(
            RecordingTransport {
                SyncHttpResponse(200, acknowledgement(requestId = "different-request"))
            }
        ).sync(batch)
        val unknownRecord = client(
            RecordingTransport {
                SyncHttpResponse(
                    200,
                    acknowledgement(batch.requestId, shiftIds = listOf("shift-other"))
                )
            }
        ).sync(batch)

        assertFailureContains(wrongRequest, "invalid sync acknowledgement")
        assertFailureContains(unknownRecord, "outside this sync batch")
    }

    @Test
    fun `authentication and network failures do not acknowledge data`() = runBlocking {
        val batch = createSyncBatch(listOf(shift), listOf(ticket))
        val unauthorized = client(
            RecordingTransport { SyncHttpResponse(401, "{\"error\":\"expired\"}") }
        ).sync(batch)
        val unreachable = client(
            SyncHttpTransport { throw IOException("secret infrastructure detail") }
        ).sync(batch)

        assertFailureContains(unauthorized, "authentication was rejected")
        assertFailureContains(unreachable, "service is unreachable")
        assertFalse((unreachable as SyncResult.Failure).message.contains("secret"))
    }

    @Test
    fun `empty batch and active shift are rejected before network access`() = runBlocking {
        val transport = RecordingTransport { SyncHttpResponse(500, "") }
        val emptyResult = client(transport).sync(createSyncBatch(emptyList(), emptyList()))
        val activeResult = client(transport).sync(
            createSyncBatch(listOf(shift.copy(endedAtMillis = null)), emptyList())
        )

        assertFailureContains(emptyResult, "batch is empty")
        assertFailureContains(activeResult, "Only closed shifts")
        assertEquals(0, transport.callCount)
    }

    private fun client(transport: SyncHttpTransport) = ProductionTransitSyncClient(
        config = ProductionSyncConfig(
            endpointUrl = "https://sync.example.test/v1/batches",
            accessToken = "test-token"
        ),
        transport = transport,
        clockMillis = { 999L }
    )

    private fun acknowledgement(
        requestId: String,
        shiftIds: List<String> = emptyList(),
        ticketIds: List<String> = emptyList()
    ): String {
        val shifts = shiftIds.joinToString(",") { "\"$it\"" }
        val tickets = ticketIds.joinToString(",") { "\"$it\"" }
        return """
            {
              "contractVersion": 1,
              "requestId": "$requestId",
              "acknowledgedShiftIds": [$shifts],
              "acknowledgedTicketIds": [$tickets]
            }
        """.trimIndent()
    }

    private fun assertFailsConfiguration(endpointUrl: String, accessToken: String) {
        val failure = runCatching {
            ProductionSyncConfig(endpointUrl = endpointUrl, accessToken = accessToken)
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    private fun assertFailureContains(result: SyncResult, expected: String) {
        assertTrue(result is SyncResult.Failure)
        assertTrue((result as SyncResult.Failure).message.contains(expected))
    }

    private class RecordingTransport(
        var responseFactory: (SyncHttpRequest) -> SyncHttpResponse
    ) : SyncHttpTransport {
        var lastRequest: SyncHttpRequest? = null
        var callCount: Int = 0

        override suspend fun execute(request: SyncHttpRequest): SyncHttpResponse {
            callCount += 1
            lastRequest = request
            return responseFactory(request)
        }
    }
}
