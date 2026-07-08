package com.buspay.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.buspay.app.data.OfflineFirstRepository
import com.buspay.app.domain.Bus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.DriverShiftSummary
import com.buspay.app.domain.Route
import com.buspay.app.domain.Shift
import com.buspay.app.domain.Stop
import com.buspay.app.domain.Ticket

data class DriverShiftUiState(
    val availableDrivers: List<Driver> = emptyList(),
    val selectedDriver: Driver? = null,
    val signedInDriver: Driver? = null,
    val buses: List<Bus> = emptyList(),
    val routes: List<Route> = emptyList(),
    val selectedBus: Bus? = null,
    val selectedRoute: Route? = null,
    val activeShift: Shift? = null,
    val tickets: List<Ticket> = emptyList(),
    val pendingTicketCount: Int = 0,
    val lastClosedSummary: DriverShiftSummary? = null
) {
    val isDriverSignedIn: Boolean = signedInDriver != null
    val isShiftActive: Boolean = activeShift != null
    val nextStopName: String = selectedRoute?.stops?.firstOrNull()?.name ?: "Select a route"
    val ticketCount: Int = tickets.size
    val cashTotalCents: Int = tickets.sumOf { it.priceCents }
}

class DriverShiftViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OfflineFirstRepository(application.applicationContext)

    var uiState by mutableStateOf(createInitialState())
        private set

    fun selectDriver(driver: Driver) {
        if (uiState.isDriverSignedIn || uiState.isShiftActive) return
        uiState = uiState.copy(selectedDriver = driver, lastClosedSummary = null)
    }

    fun signInDriver() {
        val driver = uiState.selectedDriver ?: return
        repository.saveSignedInDriver(driver)
        uiState = uiState.copy(signedInDriver = driver, lastClosedSummary = null)
    }

    fun signOutDriver() {
        if (uiState.isShiftActive) return
        repository.clearSignedInDriver()
        uiState = uiState.copy(signedInDriver = null, lastClosedSummary = null)
    }

    fun selectBus(bus: Bus) {
        if (uiState.isShiftActive) return
        uiState = uiState.copy(selectedBus = bus, lastClosedSummary = null)
    }

    fun selectRoute(route: Route) {
        if (uiState.isShiftActive) return
        uiState = uiState.copy(selectedRoute = route, lastClosedSummary = null)
    }

    fun startShift() {
        val driver = uiState.signedInDriver ?: return
        val bus = uiState.selectedBus ?: return
        val route = uiState.selectedRoute ?: return

        uiState = uiState.copy(
            activeShift = Shift(
                id = "shift-${System.currentTimeMillis()}",
                driverId = driver.id,
                busId = bus.id,
                routeId = route.id,
                startedAtMillis = System.currentTimeMillis()
            ),
            tickets = emptyList(),
            pendingTicketCount = repository.pendingTicketCount(),
            lastClosedSummary = null
        )

        uiState.activeShift?.let(repository::saveActiveShift)
    }

    fun sellTicket() {
        val shift = uiState.activeShift ?: return
        val ticket = Ticket(
            id = "ticket-${System.currentTimeMillis()}",
            shiftId = shift.id,
            priceCents = STANDARD_TICKET_PRICE_CENTS,
            soldAtMillis = System.currentTimeMillis(),
            synced = false
        )

        repository.saveTicket(ticket)

        uiState = uiState.copy(
            tickets = uiState.tickets + ticket,
            pendingTicketCount = repository.pendingTicketCount()
        )
    }

    fun endShift() {
        if (!uiState.isShiftActive) return

        repository.clearActiveShift()

        uiState = uiState.copy(
            activeShift = null,
            tickets = emptyList(),
            pendingTicketCount = repository.pendingTicketCount(),
            lastClosedSummary = DriverShiftSummary(
                ticketCount = uiState.ticketCount,
                cashTotalCents = uiState.cashTotalCents
            )
        )
    }

    private fun createInitialState(): DriverShiftUiState {
        val restoredShift = repository.loadActiveShift()
        val restoredDriver = restoredShift?.let { shift ->
            demoDrivers.firstOrNull { it.id == shift.driverId }
        } ?: repository.loadSignedInDriver()?.let { signedInDriver ->
            demoDrivers.firstOrNull { it.id == signedInDriver.id } ?: signedInDriver
        }
        val restoredTickets = restoredShift?.let { shift ->
            repository.loadTicketsForShift(shift.id)
        }.orEmpty()

        return DriverShiftUiState(
            availableDrivers = demoDrivers,
            selectedDriver = restoredDriver ?: demoDrivers.first(),
            signedInDriver = restoredDriver,
            buses = demoBuses,
            routes = demoRoutes,
            selectedBus = restoredShift?.let { shift ->
                demoBuses.firstOrNull { it.id == shift.busId }
            } ?: demoBuses.first(),
            selectedRoute = restoredShift?.let { shift ->
                demoRoutes.firstOrNull { it.id == shift.routeId }
            } ?: demoRoutes.first(),
            activeShift = restoredShift,
            tickets = restoredTickets,
            pendingTicketCount = repository.pendingTicketCount()
        )
    }

    private companion object {
        const val STANDARD_TICKET_PRICE_CENTS = 50

        val demoDrivers = listOf(
            Driver(id = "driver-001", name = "Arben Krasniqi"),
            Driver(id = "driver-002", name = "Drita Berisha"),
            Driver(id = "driver-003", name = "Ilir Gashi")
        )

        val demoBuses = listOf(
            Bus(id = "bus-101", plateNumber = "01-101-KS"),
            Bus(id = "bus-205", plateNumber = "01-205-KS"),
            Bus(id = "bus-318", plateNumber = "01-318-KS")
        )

        val demoRoutes = listOf(
            Route(
                id = "route-1",
                name = "Line 1 - Center to Hospital",
                stops = listOf(
                    Stop("stop-1", "Central Station", 42.6629, 21.1655, 1),
                    Stop("stop-2", "Mother Teresa Boulevard", 42.6608, 21.1622, 2),
                    Stop("stop-3", "University Hospital", 42.6488, 21.1612, 3)
                )
            ),
            Route(
                id = "route-2",
                name = "Line 2 - Center to Sunny Hill",
                stops = listOf(
                    Stop("stop-4", "Central Station", 42.6629, 21.1655, 1),
                    Stop("stop-5", "City Park", 42.6551, 21.1713, 2),
                    Stop("stop-6", "Sunny Hill", 42.6468, 21.1781, 3)
                )
            )
        )
    }
}
