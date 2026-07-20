package com.buspay.app.data

import com.buspay.app.domain.Bus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.FareType
import com.buspay.app.domain.ManagedCatalog
import com.buspay.app.domain.Route
import com.buspay.app.domain.Stop
import com.buspay.app.domain.ServiceCalendar
import com.buspay.app.domain.ScheduledStopTime
import com.buspay.app.domain.ScheduledTrip
import com.buspay.app.domain.TripAssignment
import com.buspay.app.domain.TripDirection
import com.buspay.app.domain.isOperationallyValid
import java.io.IOException
import java.net.URI
import org.json.JSONObject

sealed interface CatalogRefreshResult {
    data class Success(val catalog: ManagedCatalog) : CatalogRefreshResult
    data class Failure(val message: String) : CatalogRefreshResult
}

class ManagedCatalogClient(
    private val config: ProductionSyncConfig,
    private val transport: SyncHttpTransport = UrlConnectionSyncTransport(
        allowLoopbackHttp = config.allowLoopbackHttp
    )
) {
    suspend fun fetch(): CatalogRefreshResult {
        val request = SyncHttpRequest(
            endpointUrl = catalogEndpoint(config.endpointUrl),
            headers = mapOf(
                "Authorization" to "Bearer ${config.accessToken}",
                "Accept" to "application/json"
            ),
            body = "",
            connectTimeoutMillis = config.connectTimeoutMillis,
            readTimeoutMillis = config.readTimeoutMillis,
            method = "GET"
        )
        val response = try {
            transport.execute(request)
        } catch (_: IOException) {
            return CatalogRefreshResult.Failure("The managed catalog service is unreachable")
        } catch (_: SecurityException) {
            return CatalogRefreshResult.Failure("The secure catalog connection was rejected")
        } catch (_: Exception) {
            return CatalogRefreshResult.Failure("The managed catalog request failed")
        }
        if (response.statusCode == 401) {
            return CatalogRefreshResult.Failure("Catalog authentication was rejected")
        }
        if (response.statusCode == 403) {
            return CatalogRefreshResult.Failure("This credential cannot read the managed catalog")
        }
        if (response.statusCode !in 200..299) {
            return CatalogRefreshResult.Failure("Catalog service returned HTTP ${response.statusCode}")
        }
        val catalog = runCatching { parseManagedCatalog(response.body) }.getOrNull()
            ?: return CatalogRefreshResult.Failure("The server returned an invalid managed catalog")
        return CatalogRefreshResult.Success(catalog)
    }
}

internal fun catalogEndpoint(syncEndpoint: String): String {
    val endpoint = URI(syncEndpoint)
    val path = endpoint.path.orEmpty()
    val catalogPath = if (path.endsWith("/sync")) {
        path.removeSuffix("/sync") + "/catalog"
    } else {
        path.trimEnd('/') + "/catalog"
    }
    return URI(
        endpoint.scheme,
        endpoint.userInfo,
        endpoint.host,
        endpoint.port,
        catalogPath,
        null,
        null
    ).toString()
}

internal fun parseManagedCatalog(raw: String): ManagedCatalog {
    val root = JSONObject(raw)
    require(root.getInt("contractVersion") == 1)
    val stopsByRoute = root.getJSONArray("stops").let { values ->
        buildList {
            for (index in 0 until values.length()) {
                val value = values.getJSONObject(index)
                add(
                    value.getString("routeId") to Stop(
                        id = value.getString("id"),
                        name = value.getString("name"),
                        latitude = value.getDouble("latitude"),
                        longitude = value.getDouble("longitude"),
                        order = value.getInt("order")
                    )
                )
            }
        }.groupBy({ it.first }, { it.second })
    }
    val catalog = ManagedCatalog(
        revision = root.getInt("revision"),
        updatedAtMillis = root.getLong("updatedAtMillis"),
        drivers = root.getJSONArray("drivers").mapObjects { value ->
            Driver(id = value.getString("id"), name = value.getString("name"))
        },
        buses = root.getJSONArray("buses").mapObjects { value ->
            Bus(id = value.getString("id"), plateNumber = value.getString("plateNumber"))
        },
        routes = root.getJSONArray("routes").mapObjects { value ->
            Route(
                id = value.getString("id"),
                name = value.getString("name"),
                stops = stopsByRoute[value.getString("id")].orEmpty().sortedBy(Stop::order)
            )
        },
        fareTypes = root.getJSONArray("fares").mapObjects { value ->
            FareType(
                id = value.getString("id"),
                name = value.getString("name"),
                priceCents = value.getInt("priceCents"),
                eligibility = if (value.isNull("eligibility")) null else value.getString("eligibility")
            )
        },
        serviceCalendars = root.optJSONArray("serviceCalendars")?.mapObjects { value ->
            ServiceCalendar(
                id = value.getString("id"), name = value.getString("name"),
                startDate = value.getString("startDate"), endDate = value.getString("endDate"),
                activeWeekdays = value.getJSONArray("activeWeekdays").mapInts().toSet()
            )
        }.orEmpty(),
        scheduledTrips = root.optJSONArray("scheduledTrips")?.mapObjects { value ->
            ScheduledTrip(
                id = value.getString("id"), routeId = value.getString("routeId"),
                serviceCalendarId = value.getString("serviceCalendarId"),
                departureMinutes = value.getInt("departureMinutes"),
                direction = TripDirection.valueOf(value.getString("direction")),
                stopTimes = value.getJSONArray("stopTimes").mapObjects { stopTime ->
                    ScheduledStopTime(stopTime.getString("stopId"), stopTime.getInt("arrivalMinutes"), stopTime.getInt("departureMinutes"))
                }
            )
        }.orEmpty(),
        tripAssignments = root.optJSONArray("tripAssignments")?.mapObjects { value ->
            TripAssignment(value.getString("id"), value.getString("tripId"), value.getString("serviceDate"), value.getString("driverId"), value.getString("busId"))
        }.orEmpty()
    )
    require(catalog.isOperationallyValid())
    return catalog
}

private inline fun <T> org.json.JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    buildList {
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }

private fun org.json.JSONArray.mapInts(): List<Int> = buildList {
    for (index in 0 until length()) add(getInt(index))
}
