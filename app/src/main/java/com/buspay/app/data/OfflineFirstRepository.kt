package com.buspay.app.data

import android.content.Context
import android.content.SharedPreferences
import com.buspay.app.domain.Driver
import com.buspay.app.domain.Shift
import com.buspay.app.domain.RouteProgress
import com.buspay.app.domain.RouteProgressSource
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.TicketPrintStatus
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

    fun clearActiveShift() {
        preferences.edit()
            .remove(KEY_ACTIVE_SHIFT)
            .remove(KEY_ROUTE_PROGRESS)
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

    fun pendingTicketCount(): Int = loadTickets().count { !it.synced }

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

    private fun Shift.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("driverId", driverId)
            .put("busId", busId)
            .put("routeId", routeId)
            .put("startedAtMillis", startedAtMillis)
            .put("endedAtMillis", endedAtMillis)
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
        const val KEY_ROUTE_PROGRESS = "route_progress"
        const val KEY_PRINTER_ADDRESS = "printer_address"
        const val KEY_PRINTER_NAME = "printer_name"

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
                }
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
    }
}
