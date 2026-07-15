package com.buspay.app.data

import com.buspay.app.domain.Shift
import com.buspay.app.domain.SyncResult
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.createSyncBatch
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalValidationTransitSyncClientTest {
    @Test
    fun `loopback client sends authenticated batch and accepts server acknowledgement`() {
        val observedAuthorization = AtomicReference<String>()
        val observedContract = AtomicReference<String>()
        val observedIdempotency = AtomicReference<String>()
        val observedBody = AtomicReference<String>()
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val serverFailure = AtomicReference<Throwable>()
        val shift = Shift(
            id = "shift-local-1",
            driverId = "driver-002",
            busId = "bus-002",
            routeId = "route-002",
            startedAtMillis = 100L,
            endedAtMillis = 200L
        )
        val ticket = Ticket(
            id = "ticket-local-1",
            shiftId = shift.id,
            fareTypeId = "student",
            priceCents = 30,
            soldAtMillis = 150L,
            synced = false
        )
        val batch = createSyncBatch(listOf(shift), listOf(ticket))

        val serverThread = Thread {
            runCatching {
                server.accept().use { socket ->
                    val input = socket.getInputStream()
                    val headerBytes = readHttpHeaders(input)
                    val headers = headerBytes.toString(Charsets.UTF_8)
                    val contentLength = headers.lineSequence()
                        .first { it.startsWith("Content-Length:", ignoreCase = true) }
                        .substringAfter(':')
                        .trim()
                        .toInt()
                    observedAuthorization.set(headers.headerValue("Authorization"))
                    observedContract.set(headers.headerValue("X-BusPay-Contract-Version"))
                    observedIdempotency.set(headers.headerValue("Idempotency-Key"))
                    observedBody.set(input.readNBytes(contentLength).toString(Charsets.UTF_8))
                    val response = """
                        {
                          "contractVersion": 1,
                          "requestId": "${batch.requestId}",
                          "acknowledgedShiftIds": ["${shift.id}"],
                          "acknowledgedTicketIds": ["${ticket.id}"]
                        }
                    """.trimIndent().toByteArray()
                    socket.getOutputStream().use { output ->
                        output.write(
                            (
                                "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/json\r\n" +
                                    "Content-Length: ${response.size}\r\n" +
                                    "Connection: close\r\n\r\n"
                                ).toByteArray()
                        )
                        output.write(response)
                    }
                }
            }.onFailure(serverFailure::set)
        }
        serverThread.start()

        try {
            val client = createTransitSyncClient(
                SyncRuntimeConfig.localValidation(
                    endpointUrl = "http://127.0.0.1:${server.localPort}/v1/sync",
                    accessToken = "module-15-test-token"
                )
            )

            val result = runBlocking { client.sync(batch) }

            assertTrue(result is SyncResult.Success)
            assertEquals("Bearer module-15-test-token", observedAuthorization.get())
            assertEquals("1", observedContract.get())
            assertEquals(batch.requestId, observedIdempotency.get())
            assertTrue(observedBody.get().contains("\"driverId\":\"driver-002\""))
            assertTrue(
                observedBody.get().contains("\"tickets\":[{\"id\":\"ticket-local-1\"")
            )
            assertTrue(!observedBody.get().contains("module-15-test-token"))
            serverThread.join(2_000)
            assertEquals(null, serverFailure.get())
        } finally {
            server.close()
        }
    }

    private fun readHttpHeaders(input: java.io.InputStream): ByteArray {
        val bytes = mutableListOf<Byte>()
        while (bytes.size < 64 * 1024) {
            val next = input.read()
            check(next >= 0) { "Connection closed before HTTP headers completed" }
            bytes += next.toByte()
            if (bytes.takeLast(4) == listOf(13, 10, 13, 10).map(Int::toByte)) break
        }
        return bytes.toByteArray()
    }

    private fun String.headerValue(name: String): String = lineSequence()
        .first { it.startsWith("$name:", ignoreCase = true) }
        .substringAfter(':')
        .trim()
}
