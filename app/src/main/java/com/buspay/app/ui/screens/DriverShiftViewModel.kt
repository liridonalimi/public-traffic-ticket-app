package com.buspay.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buspay.app.data.DemoTransitData
import com.buspay.app.data.OfflineFirstRepository
import com.buspay.app.device.AndroidGpsTracker
import com.buspay.app.device.BluetoothEscPosTicketPrinter
import com.buspay.app.device.PrintResult
import com.buspay.app.device.PrintableTicket
import com.buspay.app.device.PrinterDevice
import com.buspay.app.device.PdfTicketPrinter
import com.buspay.app.domain.Bus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.DriverShiftSummary
import com.buspay.app.domain.FareType
import com.buspay.app.domain.Route
import com.buspay.app.domain.RouteProgress
import com.buspay.app.domain.RouteProgressSource
import com.buspay.app.domain.RouteStopStatus
import com.buspay.app.domain.Shift
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.TicketPrintStatus
import com.buspay.app.domain.nearestForwardStopIndex
import com.buspay.app.domain.routeStopStatus
import com.buspay.app.domain.summarizeTicketsByFare
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

data class DriverShiftUiState(
    val availableDrivers: List<Driver> = emptyList(),
    val selectedDriver: Driver? = null,
    val signedInDriver: Driver? = null,
    val buses: List<Bus> = emptyList(),
    val routes: List<Route> = emptyList(),
    val selectedBus: Bus? = null,
    val selectedRoute: Route? = null,
    val activeShift: Shift? = null,
    val routeProgress: RouteProgress? = null,
    val isGpsTracking: Boolean = false,
    val gpsMessage: String? = null,
    val fareTypes: List<FareType> = emptyList(),
    val selectedFareType: FareType? = null,
    val pairedPrinters: List<PrinterDevice> = emptyList(),
    val selectedPrinter: PrinterDevice? = null,
    val isPrinting: Boolean = false,
    val printerMessage: String? = null,
    val lastPdfPath: String? = null,
    val tickets: List<Ticket> = emptyList(),
    val pendingTicketCount: Int = 0,
    val lastClosedSummary: DriverShiftSummary? = null,
    val lastClosedRoute: Route? = null,
    val lastClosedRouteProgress: RouteProgress? = null
) {
    val isDriverSignedIn: Boolean = signedInDriver != null
    val isShiftActive: Boolean = activeShift != null
    val routeStopStatus: RouteStopStatus = routeStopStatus(
        stops = selectedRoute?.stops.orEmpty(),
        currentStopIndex = routeProgress?.currentStopIndex ?: 0
    )
    val passengerRoute: Route? = if (isShiftActive) selectedRoute else lastClosedRoute
    val passengerRouteProgress: RouteProgress? = if (isShiftActive) {
        routeProgress
    } else {
        lastClosedRouteProgress
    }
    val passengerRouteStopStatus: RouteStopStatus = routeStopStatus(
        stops = passengerRoute?.stops.orEmpty(),
        currentStopIndex = passengerRouteProgress?.currentStopIndex ?: 0
    )
    val canOpenPassengerDisplay: Boolean = passengerRoute != null && passengerRouteProgress != null
    val ticketCount: Int = tickets.size
    val cashTotalCents: Int = tickets.sumOf { it.priceCents }
    val fareTypeSummaries = summarizeTicketsByFare(tickets, fareTypes)
    val unprintedTickets: List<Ticket> = tickets.filter {
        it.printStatus != TicketPrintStatus.PRINTED
    }
}

class DriverShiftViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OfflineFirstRepository(application.applicationContext)
    private val bluetoothTicketPrinter = BluetoothEscPosTicketPrinter(application.applicationContext)
    private val pdfTicketPrinter = PdfTicketPrinter(application.applicationContext)
    private val gpsTracker = AndroidGpsTracker(application.applicationContext)

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

    fun selectFareType(fareType: FareType) {
        if (!uiState.isShiftActive || fareType !in uiState.fareTypes) return
        uiState = uiState.copy(selectedFareType = fareType)
    }

    fun refreshPrinters() {
        var bluetoothPermissionMissing = false
        val bluetoothPrinters = try {
            bluetoothTicketPrinter.pairedPrinters()
        } catch (_: SecurityException) {
            bluetoothPermissionMissing = true
            emptyList()
        }
        val printers = pdfTicketPrinter.pairedPrinters() + bluetoothPrinters

        val selectedPrinter = uiState.selectedPrinter?.let { selected ->
            printers.firstOrNull { it.address == selected.address } ?: selected
        } ?: PdfTicketPrinter.TEST_DEVICE
        uiState = uiState.copy(
            pairedPrinters = printers,
            selectedPrinter = selectedPrinter,
            printerMessage = when {
                bluetoothPermissionMissing ->
                    "PDF testing is available. Allow Bluetooth to find physical printers."
                bluetoothPrinters.isEmpty() ->
                    "PDF testing is available. No paired Bluetooth printers were found."
                else -> null
            }
        )
    }

    fun selectPrinter(printer: PrinterDevice) {
        if (uiState.isPrinting || printer !in uiState.pairedPrinters) return
        repository.savePrinter(printer)
        uiState = uiState.copy(selectedPrinter = printer, printerMessage = null)
    }

    fun startShift() {
        val driver = uiState.signedInDriver ?: return
        val bus = uiState.selectedBus ?: return
        val route = uiState.selectedRoute ?: return

        val shift = Shift(
            id = "shift-${System.currentTimeMillis()}",
            driverId = driver.id,
            busId = bus.id,
            routeId = route.id,
            startedAtMillis = System.currentTimeMillis()
        )
        val initialProgress = RouteProgress(
            shiftId = shift.id,
            currentStopIndex = 0,
            updatedAtMillis = shift.startedAtMillis,
            source = RouteProgressSource.SHIFT_START
        )

        uiState = uiState.copy(
            activeShift = shift,
            routeProgress = initialProgress,
            isGpsTracking = false,
            gpsMessage = "Allow location to update stops automatically, or use demo advance.",
            tickets = emptyList(),
            pendingTicketCount = repository.pendingTicketCount(),
            lastClosedSummary = null,
            lastClosedRoute = null,
            lastClosedRouteProgress = null
        )

        repository.saveActiveShift(shift)
        repository.saveRouteProgress(initialProgress)
    }

    fun startGpsTracking() {
        if (!uiState.isShiftActive || uiState.isGpsTracking) return

        val started = try {
            gpsTracker.start(::onLocationUpdate)
        } catch (_: SecurityException) {
            false
        }
        uiState = uiState.copy(
            isGpsTracking = started,
            gpsMessage = if (started) {
                "GPS tracking is active"
            } else {
                "GPS is unavailable. Check location permission and device location settings."
            }
        )
    }

    fun stopGpsTracking() {
        gpsTracker.stop()
        if (uiState.isGpsTracking) {
            uiState = uiState.copy(
                isGpsTracking = false,
                gpsMessage = if (uiState.isShiftActive) "GPS tracking is paused" else null
            )
        }
    }

    fun advanceToNextStop() {
        val shift = uiState.activeShift ?: return
        val route = uiState.selectedRoute ?: return
        if (route.stops.isEmpty()) return
        val currentIndex = uiState.routeProgress?.currentStopIndex ?: 0
        updateRouteProgress(
            RouteProgress(
                shiftId = shift.id,
                currentStopIndex = (currentIndex + 1).coerceAtMost(route.stops.lastIndex),
                updatedAtMillis = System.currentTimeMillis(),
                source = RouteProgressSource.MANUAL
            ),
            "Stop advanced in demo mode"
        )
    }

    fun sellTicket() {
        val shift = uiState.activeShift ?: return
        val fareType = uiState.selectedFareType ?: return
        if (uiState.isPrinting) return
        if (uiState.selectedPrinter == null) {
            uiState = uiState.copy(printerMessage = "Select a printer before selling")
            return
        }
        val ticket = Ticket(
            id = "ticket-${UUID.randomUUID()}",
            shiftId = shift.id,
            fareTypeId = fareType.id,
            priceCents = fareType.priceCents,
            soldAtMillis = System.currentTimeMillis(),
            synced = false
        )

        repository.saveTicket(ticket)

        uiState = uiState.copy(
            tickets = uiState.tickets + ticket,
            pendingTicketCount = repository.pendingTicketCount()
        )

        printTicket(ticket)
    }

    fun retryLastUnprintedTicket() {
        if (uiState.isPrinting) return
        uiState.unprintedTickets.lastOrNull()?.let(::printTicket)
    }

    fun endShift() {
        if (!uiState.isShiftActive || uiState.isPrinting || uiState.unprintedTickets.isNotEmpty()) {
            return
        }

        stopGpsTracking()
        val closedRoute = uiState.selectedRoute
        val closedRouteProgress = uiState.routeProgress
        repository.clearActiveShift()

        uiState = uiState.copy(
            activeShift = null,
            routeProgress = null,
            isGpsTracking = false,
            gpsMessage = null,
            tickets = emptyList(),
            pendingTicketCount = repository.pendingTicketCount(),
            lastClosedSummary = DriverShiftSummary(
                ticketCount = uiState.ticketCount,
                cashTotalCents = uiState.cashTotalCents,
                fareTypeSummaries = uiState.fareTypeSummaries
            ),
            lastClosedRoute = closedRoute,
            lastClosedRouteProgress = closedRouteProgress
        )
    }

    private fun createInitialState(): DriverShiftUiState {
        val restoredShift = repository.loadActiveShift()
        val restoredDriver = restoredShift?.let { shift ->
            DemoTransitData.drivers.firstOrNull { it.id == shift.driverId }
        } ?: repository.loadSignedInDriver()?.let { signedInDriver ->
            DemoTransitData.drivers.firstOrNull { it.id == signedInDriver.id } ?: signedInDriver
        }
        val restoredTickets = restoredShift?.let { shift ->
            repository.loadTicketsForShift(shift.id)
        }.orEmpty()
        val restoredProgress = restoredShift?.let { shift ->
            repository.loadRouteProgress(shift.id)
        }

        return DriverShiftUiState(
            availableDrivers = DemoTransitData.drivers,
            selectedDriver = restoredDriver ?: DemoTransitData.drivers.first(),
            signedInDriver = restoredDriver,
            buses = DemoTransitData.buses,
            routes = DemoTransitData.routes,
            selectedBus = restoredShift?.let { shift ->
                DemoTransitData.buses.firstOrNull { it.id == shift.busId }
            } ?: DemoTransitData.buses.first(),
            selectedRoute = restoredShift?.let { shift ->
                DemoTransitData.routes.firstOrNull { it.id == shift.routeId }
            } ?: DemoTransitData.routes.first(),
            activeShift = restoredShift,
            routeProgress = restoredProgress ?: restoredShift?.let { shift ->
                RouteProgress(
                    shiftId = shift.id,
                    currentStopIndex = 0,
                    updatedAtMillis = shift.startedAtMillis,
                    source = RouteProgressSource.SHIFT_START
                )
            },
            gpsMessage = restoredShift?.let { "Allow location to resume automatic stop tracking." },
            fareTypes = DemoTransitData.fareTypes,
            selectedFareType = DemoTransitData.fareTypes.first(),
            pairedPrinters = pdfTicketPrinter.pairedPrinters(),
            selectedPrinter = repository.loadPrinter() ?: PdfTicketPrinter.TEST_DEVICE,
            tickets = restoredTickets,
            pendingTicketCount = repository.pendingTicketCount()
        )
    }

    private fun onLocationUpdate(location: com.buspay.app.device.GeoLocation) {
        val shift = uiState.activeShift ?: return
        val route = uiState.selectedRoute ?: return
        if (route.stops.isEmpty()) return
        val currentIndex = uiState.routeProgress?.currentStopIndex ?: 0
        val nextIndex = nearestForwardStopIndex(
            stops = route.stops,
            latitude = location.latitude,
            longitude = location.longitude,
            currentStopIndex = currentIndex
        )
        updateRouteProgress(
            RouteProgress(
                shiftId = shift.id,
                currentStopIndex = nextIndex,
                updatedAtMillis = location.capturedAtMillis,
                source = RouteProgressSource.GPS
            ),
            "GPS updated the current stop"
        )
    }

    private fun updateRouteProgress(progress: RouteProgress, message: String) {
        repository.saveRouteProgress(progress)
        uiState = uiState.copy(routeProgress = progress, gpsMessage = message)
    }

    override fun onCleared() {
        gpsTracker.stop()
        super.onCleared()
    }

    private fun printTicket(ticket: Ticket) {
        val printer = uiState.selectedPrinter
        if (printer == null) {
            markPrintFailed(ticket, "Select a printer before printing")
            return
        }

        val fareName = uiState.fareTypes
            .firstOrNull { it.id == ticket.fareTypeId }
            ?.name
            ?: "Unknown fare"
        val printableTicket = PrintableTicket(
            ticketCode = ticket.id.removePrefix("ticket-").take(8).uppercase(),
            busPlateNumber = uiState.selectedBus?.plateNumber ?: "Unknown bus",
            routeName = uiState.selectedRoute?.name ?: "Unknown route",
            fareName = fareName,
            priceCents = ticket.priceCents,
            soldAtText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(ticket.soldAtMillis)),
            operatorName = uiState.signedInDriver?.name ?: "Unknown operator"
        )

        uiState = uiState.copy(isPrinting = true, printerMessage = "Printing ticket…")
        viewModelScope.launch {
            val selectedTicketPrinter = if (printer.address == PdfTicketPrinter.TEST_DEVICE.address) {
                pdfTicketPrinter
            } else {
                bluetoothTicketPrinter
            }
            when (val result = selectedTicketPrinter.printTicket(printer, printableTicket)) {
                is PrintResult.Success -> {
                    val printedTicket = ticket.copy(
                        printStatus = TicketPrintStatus.PRINTED,
                        printAttempts = ticket.printAttempts + 1,
                        lastPrintError = null
                    )
                    repository.updateTicket(printedTicket)
                    replaceTicket(
                        ticket = printedTicket,
                        isPrinting = false,
                        printerMessage = if (result.outputPath != null) {
                            "Ticket PDF created successfully"
                        } else {
                            "Ticket printed successfully"
                        },
                        lastPdfPath = result.outputPath ?: uiState.lastPdfPath
                    )
                }

                is PrintResult.Failure -> {
                    markPrintFailed(ticket, result.message)
                }
            }
        }
    }

    private fun markPrintFailed(ticket: Ticket, message: String) {
        val failedTicket = ticket.copy(
            printStatus = TicketPrintStatus.FAILED,
            printAttempts = ticket.printAttempts + 1,
            lastPrintError = message
        )
        repository.updateTicket(failedTicket)
        replaceTicket(
            ticket = failedTicket,
            isPrinting = false,
            printerMessage = "Print failed: $message"
        )
    }

    private fun replaceTicket(
        ticket: Ticket,
        isPrinting: Boolean,
        printerMessage: String,
        lastPdfPath: String? = uiState.lastPdfPath
    ) {
        uiState = uiState.copy(
            tickets = uiState.tickets.map { current ->
                if (current.id == ticket.id) ticket else current
            },
            isPrinting = isPrinting,
            printerMessage = printerMessage,
            lastPdfPath = lastPdfPath
        )
    }

}
