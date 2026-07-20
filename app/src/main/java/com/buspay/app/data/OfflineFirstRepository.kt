package com.buspay.app.data

import android.content.Context
import android.content.SharedPreferences
import com.buspay.app.domain.Driver
import com.buspay.app.domain.Bus
import com.buspay.app.domain.FareType
import com.buspay.app.domain.ManagedCatalog
import com.buspay.app.domain.Route
import com.buspay.app.domain.Shift
import com.buspay.app.domain.Stop
import com.buspay.app.domain.StopRequest
import com.buspay.app.domain.RouteProgress
import com.buspay.app.domain.RouteProgressSource
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.TicketPrintStatus
import com.buspay.app.domain.acknowledgeShifts
import com.buspay.app.domain.acknowledgeTickets
import com.buspay.app.device.PrinterDevice
import org.json.JSONArray
import org.json.JSONObject

class OfflineFirstRepository(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        STORAGE_NAME,
        Context.MODE_PRIVATE
    )

    fun saveActiveShift(shift: Shift) {
        preferences.edit()
            .putString(KEY_ACTIVE_SHIFT, shift.toJson().toString())
            .apply()
    }

    fun loadActiveShift(): Shift? {
        return preferences.getString(KEY_ACTIVE_SHIFT, null)
            ?.let { shiftFromJson(JSONObject(it)) }
    }

    fun saveSignedInDriver(driver: Driver) {
        preferences.edit()
            .putString(KEY_SIGNED_IN_DRIVER, driver.toJson().toString())
            .apply()
    }

    fun loadSignedInDriver(): Driver? {
        return preferences.getString(KEY_SIGNED_IN_DRIVER, null)
            ?.let { driverFromJson(JSONObject(it)) }
    }

    fun clearSignedInDriver() {
        preferences.edit()
            .remove(KEY_SIGNED_IN_DRIVER)
            .apply()
    }

    fun saveTicket(ticket: Ticket) {
        val tickets = loadTickets() + ticket
        saveTickets(tickets)
    }

    fun updateTicket(ticket: Ticket) {
        val tickets = loadTickets().map { storedTicket ->
            if (storedTicket.id == ticket.id) ticket else storedTicket
        }
        saveTickets(tickets)
    }

    fun loadTicketsForShift(shiftId: String): List<Ticket> {
        return loadTickets().filter { it.shiftId == shiftId }
    }

    fun saveClosedShift(shift: Shift) {
        require(shift.endedAtMillis != null) { "Only closed shifts can enter sync history" }
        val shifts = loadClosedShifts()
            .filterNot { it.id == shift.id } + shift
        saveClosedShifts(shifts)
    }

    fun pendingClosedShifts(): List<Shift> = loadClosedShifts().filter { !it.synced }

    fun closedShiftsForReporting(): List<Shift> = loadClosedShifts()

    fun ticketsForReporting(): List<Ticket> = loadTickets()

    fun pendingTicketsForSync(activeShiftId: String?): List<Ticket> {
        return loadTickets().filter { ticket ->
            !ticket.synced && ticket.shiftId != activeShiftId
        }
    }

    fun markSyncAcknowledged(shiftIds: Set<String>, ticketIds: Set<String>) {
        saveClosedShifts(acknowledgeShifts(loadClosedShifts(), shiftIds))
        saveTickets(acknowledgeTickets(loadTickets(), ticketIds))
    }

    fun clearActiveShift() {
        preferences.edit()
            .remove(KEY_ACTIVE_SHIFT)
            .remove(KEY_ROUTE_PROGRESS)
            .remove(KEY_STOP_REQUEST)
            .apply()
    }

    fun saveRouteProgress(progress: RouteProgress) {
        preferences.edit()
            .putString(KEY_ROUTE_PROGRESS, progress.toJson().toString())
            .apply()
    }

    fun loadRouteProgress(shiftId: String): RouteProgress? {
        return preferences.getString(KEY_ROUTE_PROGRESS, null)
            ?.let(::JSONObject)
            ?.let(::routeProgressFromJson)
            ?.takeIf { it.shiftId == shiftId }
    }

    fun saveStopRequest(request: StopRequest) {
        preferences.edit()
            .putString(KEY_STOP_REQUEST, request.toJson().toString())
            .apply()
    }

    fun loadStopRequest(shiftId: String): StopRequest? {
        return preferences.getString(KEY_STOP_REQUEST, null)
            ?.let(::JSONObject)
            ?.let(::stopRequestFromJson)
            ?.takeIf { it.shiftId == shiftId }
    }

    fun clearStopRequest() {
        preferences.edit()
            .remove(KEY_STOP_REQUEST)
            .apply()
    }

    fun pendingTicketCount(): Int = loadTickets().count { !it.synced }

    fun pendingShiftCount(): Int = loadClosedShifts().count { !it.synced }

    fun savePrinter(printer: PrinterDevice) {
        preferences.edit()
            .putString(KEY_PRINTER_ADDRESS, printer.address)
            .putString(KEY_PRINTER_NAME, printer.name)
            .apply()
    }

    fun loadPrinter(): PrinterDevice? {
        val address = preferences.getString(KEY_PRINTER_ADDRESS, null) ?: return null
        val name = preferences.getString(KEY_PRINTER_NAME, null) ?: "Label printer"
        return PrinterDevice(name = name, address = address)
    }

    fun saveManagedCatalog(catalog: ManagedCatalog) {
        val value = JSONObject()
            .put("revision", catalog.revision)
            .put("updatedAtMillis", catalog.updatedAtMillis)
            .put("drivers", JSONArray().also { values ->
                catalog.drivers.forEach { values.put(it.toJson()) }
            })
            .put("buses", JSONArray().also { values ->
                catalog.buses.forEach { bus ->
                    values.put(JSONObject().put("id", bus.id).put("plateNumber", bus.plateNumber))
                }
            })
            .put("routes", JSONArray().also { values ->
                catalog.routes.forEach { route ->
                    values.put(
                        JSONObject()
                            .put("id", route.id)
                            .put("name", route.name)
                            .put("stops", JSONArray().also { stops ->
                                route.stops.forEach { stop ->
                                    stops.put(
                                        JSONObject()
                                            .put("id", stop.id)
                                            .put("name", stop.name)
                                            .put("latitude", stop.latitude)
                                            .put("longitude", stop.longitude)
                                            .put("order", stop.order)
                                    )
                                }
                            })
                    )
                }
            })
            .put("fares", JSONArray().also { values ->
                catalog.fareTypes.forEach { fare ->
                    values.put(
                        JSONObject()
                            .put("id", fare.id)
                            .put("name", fare.name)
                            .put("priceCents", fare.priceCents)
                            .put("eligibility", fare.eligibility)
                    )
                }
            })
        preferences.edit().putString(KEY_MANAGED_CATALOG, value.toString()).apply()
    }

    fun loadManagedCatalog(): ManagedCatalog? = preferences
        .getString(KEY_MANAGED_CATALOG, null)
        ?.let { raw -> runCatching { managedCatalogFromJson(JSONObject(raw)) }.getOrNull() }

    private fun loadTickets(): List<Ticket> {
        val rawTickets = preferences.getString(KEY_TICKETS, null) ?: return emptyList()
        val ticketArray = JSONArray(rawTickets)

        return buildList {
            for (index in 0 until ticketArray.length()) {
                add(ticketFromJson(ticketArray.getJSONObject(index)))
            }
        }
    }

    private fun saveTickets(tickets: List<Ticket>) {
        val ticketArray = JSONArray()
        tickets.forEach { ticketArray.put(it.toJson()) }

        preferences.edit()
            .putString(KEY_TICKETS, ticketArray.toString())
            .apply()
    }

    private fun loadClosedShifts(): List<Shift> {
        val rawShifts = preferences.getString(KEY_CLOSED_SHIFTS, null) ?: return emptyList()
        val shiftArray = JSONArray(rawShifts)

        return buildList {
            for (index in 0 until shiftArray.length()) {
                add(shiftFromJson(shiftArray.getJSONObject(index)))
            }
        }
    }

    private fun saveClosedShifts(shifts: List<Shift>) {
        val shiftArray = JSONArray()
        shifts.forEach { shiftArray.put(it.toJson()) }
        preferences.edit()
            .putString(KEY_CLOSED_SHIFTS, shiftArray.toString())
            .apply()
    }

    private fun Shift.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("driverId", driverId)
            .put("busId", busId)
            .put("routeId", routeId)
            .put("startedAtMillis", startedAtMillis)
            .put("endedAtMillis", endedAtMillis)
            .put("synced", synced)
            .put("expectedCashCents", expectedCashCents)
            .put("declaredCashCents", declaredCashCents)
            .put("reconciledAtMillis", reconciledAtMillis)
    }

    private fun Driver.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
    }

    private fun RouteProgress.toJson(): JSONObject {
        return JSONObject()
            .put("shiftId", shiftId)
            .put("currentStopIndex", currentStopIndex)
            .put("updatedAtMillis", updatedAtMillis)
            .put("source", source.name)
    }

    private fun StopRequest.toJson(): JSONObject {
        return JSONObject()
            .put("shiftId", shiftId)
            .put("requestedStopIndex", requestedStopIndex)
            .put("requestedAtMillis", requestedAtMillis)
    }

    private fun Ticket.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("shiftId", shiftId)
            .put("fareTypeId", fareTypeId)
            .put("priceCents", priceCents)
            .put("soldAtMillis", soldAtMillis)
            .put("synced", synced)
            .put("printStatus", printStatus.name)
            .put("printAttempts", printAttempts)
            .put("lastPrintError", lastPrintError)
    }

    private companion object {
        const val STORAGE_NAME = "offline_first_repository"
        const val KEY_ACTIVE_SHIFT = "active_shift"
        const val KEY_SIGNED_IN_DRIVER = "signed_in_driver"
        const val KEY_TICKETS = "tickets"
        const val KEY_CLOSED_SHIFTS = "closed_shifts"
        const val KEY_ROUTE_PROGRESS = "route_progress"
        const val KEY_STOP_REQUEST = "stop_request"
        const val KEY_PRINTER_ADDRESS = "printer_address"
        const val KEY_PRINTER_NAME = "printer_name"
        const val KEY_MANAGED_CATALOG = "managed_catalog"

        fun driverFromJson(json: JSONObject): Driver {
            return Driver(
                id = json.getString("id"),
                name = json.getString("name")
            )
        }

        fun shiftFromJson(json: JSONObject): Shift {
            return Shift(
                id = json.getString("id"),
                driverId = json.getString("driverId"),
                busId = json.getString("busId"),
                routeId = json.getString("routeId"),
                startedAtMillis = json.getLong("startedAtMillis"),
                endedAtMillis = if (json.isNull("endedAtMillis")) {
                    null
                } else {
                    json.getLong("endedAtMillis")
                },
                synced = json.optBoolean("synced", false),
                expectedCashCents = json.optionalInt("expectedCashCents"),
                declaredCashCents = json.optionalInt("declaredCashCents"),
                reconciledAtMillis = json.optionalLong("reconciledAtMillis")
            )
        }

        fun ticketFromJson(json: JSONObject): Ticket {
            return Ticket(
                id = json.getString("id"),
                shiftId = json.getString("shiftId"),
                fareTypeId = json.optString(
                    "fareTypeId",
                    Ticket.STANDARD_FARE_TYPE_ID
                ),
                priceCents = json.getInt("priceCents"),
                soldAtMillis = json.getLong("soldAtMillis"),
                synced = json.optBoolean("synced", false),
                printStatus = json.optString("printStatus")
                    .takeIf(String::isNotBlank)
                    ?.let { storedStatus ->
                        TicketPrintStatus.entries.firstOrNull { it.name == storedStatus }
                    }
                    ?: TicketPrintStatus.PENDING,
                printAttempts = json.optInt("printAttempts", 0),
                lastPrintError = if (json.isNull("lastPrintError")) {
                    null
                } else {
                    json.optString("lastPrintError").takeIf(String::isNotBlank)
                }
            )
        }

        fun routeProgressFromJson(json: JSONObject): RouteProgress {
            return RouteProgress(
                shiftId = json.getString("shiftId"),
                currentStopIndex = json.optInt("currentStopIndex", 0),
                updatedAtMillis = json.optLong("updatedAtMillis", 0L),
                source = json.optString("source")
                    .takeIf(String::isNotBlank)
                    ?.let { storedSource ->
                        RouteProgressSource.entries.firstOrNull { it.name == storedSource }
                    }
                    ?: RouteProgressSource.SHIFT_START
            )
        }

        fun stopRequestFromJson(json: JSONObject): StopRequest {
            return StopRequest(
                shiftId = json.getString("shiftId"),
                requestedStopIndex = json.optInt("requestedStopIndex", 0),
                requestedAtMillis = json.optLong("requestedAtMillis", 0L)
            )
        }

        fun managedCatalogFromJson(json: JSONObject): ManagedCatalog {
            val drivers = json.getJSONArray("drivers").mapObjects { value ->
                driverFromJson(value)
            }
            val buses = json.getJSONArray("buses").mapObjects { value ->
                Bus(id = value.getString("id"), plateNumber = value.getString("plateNumber"))
            }
            val routes = json.getJSONArray("routes").mapObjects { value ->
                Route(
                    id = value.getString("id"),
                    name = value.getString("name"),
                    stops = value.getJSONArray("stops").mapObjects { stop ->
                        Stop(
                            id = stop.getString("id"),
                            name = stop.getString("name"),
                            latitude = stop.getDouble("latitude"),
                            longitude = stop.getDouble("longitude"),
                            order = stop.getInt("order")
                        )
                    }.sortedBy(Stop::order)
                )
            }
            val fares = json.getJSONArray("fares").mapObjects { value ->
                FareType(
                    id = value.getString("id"),
                    name = value.getString("name"),
                    priceCents = value.getInt("priceCents"),
                    eligibility = if (value.isNull("eligibility")) null else value.getString("eligibility")
                )
            }
            return ManagedCatalog(
                revision = json.getInt("revision"),
                updatedAtMillis = json.getLong("updatedAtMillis"),
                drivers = drivers,
                buses = buses,
                routes = routes,
                fareTypes = fares
            )
        }

        private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
            buildList {
                for (index in 0 until length()) add(transform(getJSONObject(index)))
            }

        private fun JSONObject.optionalInt(name: String): Int? =
            if (has(name) && !isNull(name)) getInt(name) else null

        private fun JSONObject.optionalLong(name: String): Long? =
            if (has(name) && !isNull(name)) getLong(name) else null
    }
}
