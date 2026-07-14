package com.buspay.app.ui.screens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buspay.app.data.OfflineFirstRepository
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
import com.buspay.app.domain.Shift
import com.buspay.app.domain.Stop
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.TicketPrintStatus
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
    val fareTypes: List<FareType> = emptyList(),
    val selectedFareType: FareType? = null,
    val pairedPrinters: List<PrinterDevice> = emptyList(),
    val selectedPrinter: PrinterDevice? = null,
    val isPrinting: Boolean = false,
    val printerMessage: String? = null,
    val lastPdfPath: String? = null,
    val tickets: List<Ticket> = emptyList(),
    val pendingTicketCount: Int = 0,
    val lastClosedSummary: DriverShiftSummary? = null
) {
    val isDriverSignedIn: Boolean = signedInDriver != null
    val isShiftActive: Boolean = activeShift != null
    val nextStopName: String = selectedRoute?.stops?.firstOrNull()?.name ?: "Select a route"
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

        repository.clearActiveShift()

        uiState = uiState.copy(
            activeShift = null,
            tickets = emptyList(),
            pendingTicketCount = repository.pendingTicketCount(),
            lastClosedSummary = DriverShiftSummary(
                ticketCount = uiState.ticketCount,
                cashTotalCents = uiState.cashTotalCents,
                fareTypeSummaries = uiState.fareTypeSummaries
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
            fareTypes = demoFareTypes,
            selectedFareType = demoFareTypes.first(),
            pairedPrinters = pdfTicketPrinter.pairedPrinters(),
            selectedPrinter = repository.loadPrinter() ?: PdfTicketPrinter.TEST_DEVICE,
            tickets = restoredTickets,
            pendingTicketCount = repository.pendingTicketCount()
        )
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

    private companion object {
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

        val demoFareTypes = listOf(
            FareType(
                id = Ticket.STANDARD_FARE_TYPE_ID,
                name = "Standard",
                priceCents = 50
            ),
            FareType(
                id = "student",
                name = "Student",
                priceCents = 30,
                eligibility = "Valid student ID required"
            ),
            FareType(
                id = "senior",
                name = "Senior 65+",
                priceCents = 25,
                eligibility = "For passengers aged 65 or older"
            ),
            FareType(
                id = "child",
                name = "Child",
                priceCents = 20,
                eligibility = "For children aged 6 to 12"
            )
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
