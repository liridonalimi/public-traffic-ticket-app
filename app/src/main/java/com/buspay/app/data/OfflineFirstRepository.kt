package com.buspay.app.data

import android.content.Context
import android.content.SharedPreferences
import com.buspay.app.domain.Shift
import com.buspay.app.domain.Ticket
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

    fun saveTicket(ticket: Ticket) {
        val tickets = loadTickets() + ticket
        saveTickets(tickets)
    }

    fun loadTicketsForShift(shiftId: String): List<Ticket> {
        return loadTickets().filter { it.shiftId == shiftId }
    }

    fun clearActiveShift() {
        preferences.edit()
            .remove(KEY_ACTIVE_SHIFT)
            .apply()
    }

    fun pendingTicketCount(): Int = loadTickets().count { !it.synced }

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

    private fun Ticket.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("shiftId", shiftId)
            .put("priceCents", priceCents)
            .put("soldAtMillis", soldAtMillis)
            .put("synced", synced)
    }

    private companion object {
        const val STORAGE_NAME = "offline_first_repository"
        const val KEY_ACTIVE_SHIFT = "active_shift"
        const val KEY_TICKETS = "tickets"

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
                priceCents = json.getInt("priceCents"),
                soldAtMillis = json.getLong("soldAtMillis"),
                synced = json.optBoolean("synced", false)
            )
        }
    }
}
