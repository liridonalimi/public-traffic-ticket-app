package com.buspay.app.data

import com.buspay.app.domain.SyncAcknowledgement
import com.buspay.app.domain.SyncBatch
import com.buspay.app.domain.SyncResult
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductionSyncConfig(
    val endpointUrl: String,
    val accessToken: String,
    val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
    internal val allowLoopbackHttp: Boolean = false
) {
    init {
        val endpoint = runCatching { URI(endpointUrl) }
            .getOrElse { throw IllegalArgumentException("Sync endpoint must be a valid HTTPS URL") }
        require(
            endpoint.scheme.equals("https", ignoreCase = true) ||
                (allowLoopbackHttp && endpoint.isLoopbackHttp())
        ) {
            "Sync endpoint must use HTTPS; debug validation may use loopback HTTP only"
        }
        require(!endpoint.host.isNullOrBlank() && endpoint.userInfo == null && endpoint.fragment == null) {
            "Sync endpoint must be an absolute HTTPS URL without credentials or a fragment"
        }
        require(accessToken.isNotBlank() && '\n' !in accessToken && '\r' !in accessToken) {
            "A valid bearer access token is required"
        }
        require(connectTimeoutMillis in 1..MAX_TIMEOUT_MILLIS) {
            "Connect timeout must be between 1 and $MAX_TIMEOUT_MILLIS milliseconds"
        }
        require(readTimeoutMillis in 1..MAX_TIMEOUT_MILLIS) {
            "Read timeout must be between 1 and $MAX_TIMEOUT_MILLIS milliseconds"
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 20_000
        const val MAX_TIMEOUT_MILLIS = 60_000

        fun localValidation(
            endpointUrl: String,
            accessToken: String,
            connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
            readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS
        ): ProductionSyncConfig = ProductionSyncConfig(
            endpointUrl = endpointUrl,
            accessToken = accessToken,
            connectTimeoutMillis = connectTimeoutMillis,
            readTimeoutMillis = readTimeoutMillis,
            allowLoopbackHttp = true
        )
    }
}

data class SyncHttpRequest(
    val endpointUrl: String,
    val headers: Map<String, String>,
    val body: String,
    val connectTimeoutMillis: Int,
    val readTimeoutMillis: Int,
    val method: String = "POST"
)

data class SyncHttpResponse(
    val statusCode: Int,
    val body: String
)

fun interface SyncHttpTransport {
    suspend fun execute(request: SyncHttpRequest): SyncHttpResponse
}

class UrlConnectionSyncTransport(
    private val allowLoopbackHttp: Boolean = false
) : SyncHttpTransport {
    override suspend fun execute(request: SyncHttpRequest): SyncHttpResponse =
        withContext(Dispatchers.IO) {
            val endpoint = URI(request.endpointUrl)
            val connection = endpoint.toURL().openConnection()
            require(
                connection is HttpsURLConnection ||
                    (allowLoopbackHttp && endpoint.isLoopbackHttp() && connection is HttpURLConnection)
            ) { "Only HTTPS connections or debug loopback HTTP are allowed" }

            connection as HttpURLConnection

            try {
                connection.requestMethod = request.method
                connection.connectTimeout = request.connectTimeoutMillis
                connection.readTimeout = request.readTimeoutMillis
                connection.instanceFollowRedirects = false
                connection.doOutput = request.method != "GET" && request.body.isNotEmpty()
                request.headers.forEach(connection::setRequestProperty)
                if (connection.doOutput) {
                    connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write(request.body)
                    }
                }

                val statusCode = connection.responseCode
                val responseStream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val responseBody = responseStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { reader -> reader.readText().take(MAX_RESPONSE_CHARACTERS) }
                    .orEmpty()
                SyncHttpResponse(statusCode = statusCode, body = responseBody)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val MAX_RESPONSE_CHARACTERS = 1_000_000
    }
}

class ProductionTransitSyncClient(
    private val config: ProductionSyncConfig,
    private val transport: SyncHttpTransport = UrlConnectionSyncTransport(
        allowLoopbackHttp = config.allowLoopbackHttp
    ),
    private val clockMillis: () -> Long = System::currentTimeMillis
) : TransitSyncClient {
    override suspend fun sync(batch: SyncBatch): SyncResult {
        if (batch.shifts.isEmpty() && batch.tickets.isEmpty()) {
            return SyncResult.Failure("The sync batch is empty")
        }
        if (batch.shifts.any { it.endedAtMillis == null }) {
            return SyncResult.Failure("Only closed shifts can be synchronized")
        }

        val request = SyncHttpRequest(
            endpointUrl = config.endpointUrl,
            headers = mapOf(
                "Authorization" to "Bearer ${config.accessToken}",
                "Content-Type" to "application/json; charset=utf-8",
                "Accept" to "application/json",
                "Idempotency-Key" to batch.requestId,
                "X-BusPay-Contract-Version" to CONTRACT_VERSION.toString()
            ),
            body = batch.toContractJson(sentAtMillis = clockMillis()),
            connectTimeoutMillis = config.connectTimeoutMillis,
            readTimeoutMillis = config.readTimeoutMillis
        )

        val response = try {
            transport.execute(request)
        } catch (_: IOException) {
            return SyncResult.Failure("The production sync service is unreachable")
        } catch (_: SecurityException) {
            return SyncResult.Failure("The secure connection was rejected")
        } catch (_: Exception) {
            return SyncResult.Failure("The production sync request failed")
        }

        if (response.statusCode !in 200..299) {
            return SyncResult.Failure(response.failureMessage())
        }

        val acknowledgement = runCatching {
            parseAcknowledgement(response.body, expectedRequestId = batch.requestId)
        }.getOrElse {
            return SyncResult.Failure("The server returned an invalid sync acknowledgement")
        }

        val shiftIds = batch.shifts.mapTo(mutableSetOf()) { it.id }
        val ticketIds = batch.tickets.mapTo(mutableSetOf()) { it.id }
        if (!shiftIds.containsAll(acknowledgement.acknowledgedShiftIds) ||
            !ticketIds.containsAll(acknowledgement.acknowledgedTicketIds)
        ) {
            return SyncResult.Failure("The server acknowledged records outside this sync batch")
        }

        return SyncResult.Success(acknowledgement)
    }

    companion object {
        const val CONTRACT_VERSION = 1
    }
}

private fun URI.isLoopbackHttp(): Boolean =
    scheme.equals("http", ignoreCase = true) &&
        (host.equals("127.0.0.1", ignoreCase = true) ||
            host.equals("localhost", ignoreCase = true) ||
            host == "::1" ||
            host == "[::1]") &&
        userInfo == null &&
        fragment == null

private fun SyncBatch.toContractJson(sentAtMillis: Long): String = buildString {
    append('{')
    appendJsonNumber("contractVersion", ProductionTransitSyncClient.CONTRACT_VERSION)
    append(',')
    appendJsonString("requestId", requestId)
    append(',')
    appendJsonNumber("sentAtMillis", sentAtMillis)
    append(',')
    append("\"shifts\":[")
    shifts.forEachIndexed { index, shift ->
        if (index > 0) append(',')
        append('{')
        appendJsonString("id", shift.id)
        append(',')
        appendJsonString("driverId", shift.driverId)
        append(',')
        appendJsonString("busId", shift.busId)
        append(',')
        appendJsonString("routeId", shift.routeId)
        append(',')
        appendJsonNumber("startedAtMillis", shift.startedAtMillis)
        append(',')
        appendJsonNumber("endedAtMillis", requireNotNull(shift.endedAtMillis))
        append(',')
        appendJsonNullableNumber("expectedCashCents", shift.expectedCashCents)
        append(',')
        appendJsonNullableNumber("declaredCashCents", shift.declaredCashCents)
        append(',')
        appendJsonNullableNumber("reconciledAtMillis", shift.reconciledAtMillis)
        append(',')
        appendJsonNullableString("scheduledTripId", shift.scheduledTripId)
        append(',')
        appendJsonNullableString("assignmentId", shift.assignmentId)
        append('}')
    }
    append("],\"tickets\":[")
    tickets.forEachIndexed { index, ticket ->
        if (index > 0) append(',')
        append('{')
        appendJsonString("id", ticket.id)
        append(',')
        appendJsonString("shiftId", ticket.shiftId)
        append(',')
        appendJsonString("fareTypeId", ticket.fareTypeId)
        append(',')
        appendJsonNumber("priceCents", ticket.priceCents)
        append(',')
        appendJsonNumber("soldAtMillis", ticket.soldAtMillis)
        append('}')
    }
    append("]}")
}

private fun StringBuilder.appendJsonString(name: String, value: String) {
    append('"').append(name).append("\":\"").append(value.jsonEscaped()).append('"')
}

private fun StringBuilder.appendJsonNullableString(name: String, value: String?) {
    append('"').append(name).append("\":")
    if (value == null) append("null") else append('"').append(value.jsonEscaped()).append('"')
}

private fun StringBuilder.appendJsonNumber(name: String, value: Number) {
    append('"').append(name).append("\":").append(value)
}

private fun StringBuilder.appendJsonNullableNumber(name: String, value: Number?) {
    append('"').append(name).append("\":")
    if (value == null) append("null") else append(value)
}

private fun String.jsonEscaped(): String = buildString {
    this@jsonEscaped.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u%04x".format(character.code))
            } else {
                append(character)
            }
        }
    }
}

private fun parseAcknowledgement(body: String, expectedRequestId: String): SyncAcknowledgement {
    require(body.isNotBlank())
    require(body.numberField("contractVersion") == ProductionTransitSyncClient.CONTRACT_VERSION.toLong())
    require(body.stringField("requestId") == expectedRequestId)
    return SyncAcknowledgement(
        acknowledgedShiftIds = body.stringArrayField("acknowledgedShiftIds").toSet(),
        acknowledgedTicketIds = body.stringArrayField("acknowledgedTicketIds").toSet()
    )
}

private fun String.numberField(name: String): Long {
    val pattern = Regex("\\\"${Regex.escape(name)}\\\"\\s*:\\s*(-?\\d+)")
    return pattern.find(this)?.groupValues?.get(1)?.toLong()
        ?: throw IllegalArgumentException("Missing number field")
}

private fun String.stringField(name: String): String {
    val pattern = Regex("\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
    return pattern.find(this)?.groupValues?.get(1)?.jsonUnescaped()
        ?: throw IllegalArgumentException("Missing string field")
}

private fun String.stringArrayField(name: String): List<String> {
    val fieldPattern = Regex(
        "\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\[((?:\\s*\\\"(?:\\\\.|[^\\\"\\\\])*\\\"\\s*,?)*)]"
    )
    val content = fieldPattern.find(this)?.groupValues?.get(1)
        ?: throw IllegalArgumentException("Missing array field")
    val valuePattern = Regex("\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
    return valuePattern.findAll(content).map { it.groupValues[1].jsonUnescaped() }.toList()
}

private fun String.jsonUnescaped(): String = buildString {
    var index = 0
    while (index < this@jsonUnescaped.length) {
        val character = this@jsonUnescaped[index++]
        if (character != '\\') {
            append(character)
            continue
        }
        require(index < this@jsonUnescaped.length)
        when (val escaped = this@jsonUnescaped[index++]) {
            '"', '\\', '/' -> append(escaped)
            'b' -> append('\b')
            'f' -> append('\u000C')
            'n' -> append('\n')
            'r' -> append('\r')
            't' -> append('\t')
            'u' -> {
                require(index + 4 <= this@jsonUnescaped.length)
                append(this@jsonUnescaped.substring(index, index + 4).toInt(16).toChar())
                index += 4
            }
            else -> throw IllegalArgumentException("Invalid JSON escape")
        }
    }
}

private fun SyncHttpResponse.failureMessage(): String = when (statusCode) {
    400 -> "The server rejected the sync payload"
    401, 403 -> "Production sync authentication was rejected"
    409 -> "The server reported a conflicting sync batch"
    413 -> "The sync batch is too large"
    429 -> "The sync service is busy; retry later"
    in 500..599 -> "The production sync service is temporarily unavailable"
    else -> "The sync service returned HTTP $statusCode"
}
