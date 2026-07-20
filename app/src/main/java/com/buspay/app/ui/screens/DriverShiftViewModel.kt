package com.buspay.app.ui.screens

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buspay.app.R
import com.buspay.app.data.DemoTransitData
import com.buspay.app.data.DemoTransitSyncClient
import com.buspay.app.data.CatalogRefreshResult
import com.buspay.app.data.ManagedCatalogClient
import com.buspay.app.data.OfflineFirstRepository
import com.buspay.app.data.SyncRuntimeConfig
import com.buspay.app.data.SyncRuntimeMode
import com.buspay.app.data.createTransitSyncClient
import com.buspay.app.data.createManagedCatalogClient
import com.buspay.app.device.AndroidGpsTracker
import com.buspay.app.device.BluetoothEscPosTicketPrinter
import com.buspay.app.device.DemoStopRequestInput
import com.buspay.app.device.PrintResult
import com.buspay.app.device.PrintableTicket
import com.buspay.app.device.PrinterDevice
import com.buspay.app.device.PdfTicketPrinter
import com.buspay.app.domain.Bus
import com.buspay.app.domain.AdminReport
import com.buspay.app.domain.AdminReportFilter
import com.buspay.app.domain.Driver
import com.buspay.app.domain.DriverShiftSummary
import com.buspay.app.domain.FareType
import com.buspay.app.domain.ManagedCatalog
import com.buspay.app.domain.Route
import com.buspay.app.domain.RouteProgress
import com.buspay.app.domain.RouteProgressSource
import com.buspay.app.domain.RouteStopStatus
import com.buspay.app.domain.Shift
import com.buspay.app.domain.StopRequest
import com.buspay.app.domain.SyncResult
import com.buspay.app.domain.Ticket
import com.buspay.app.domain.TicketPrintStatus
import com.buspay.app.domain.createSyncBatch
import com.buspay.app.domain.buildAdminReport
import com.buspay.app.domain.isStopRequestReached
import com.buspay.app.domain.nearestForwardStopIndex
import com.buspay.app.domain.routeStopStatus
import com.buspay.app.domain.summarizeTicketsByFare
import com.buspay.app.domain.isOperationallyValid
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
    val activeStopRequest: StopRequest? = null,
    val stopRequestMessage: String? = null,
    val fareTypes: List<FareType> = emptyList(),
    val selectedFareType: FareType? = null,
    val pairedPrinters: List<PrinterDevice> = emptyList(),
    val selectedPrinter: PrinterDevice? = null,
    val isPrinting: Boolean = false,
    val printerMessage: String? = null,
    val lastPdfPath: String? = null,
    val tickets: List<Ticket> = emptyList(),
    val pendingTicketCount: Int = 0,
    val pendingShiftCount: Int = 0,
    val syncableTicketCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncRuntimeMode: SyncRuntimeMode = SyncRuntimeMode.DEMO,
    val isDemoSyncMode: Boolean = true,
    val isDemoServerAvailable: Boolean = true,
    val canUseLocalValidationServer: Boolean = false,
    val isSyncConfigurationOpen: Boolean = false,
    val syncEndpointDraft: String = "http://127.0.0.1:8080/v1/sync",
    val syncTokenDraft: String = "",
    val syncMessage: String? = null,
    val isCatalogRefreshing: Boolean = false,
    val catalogRevision: Int? = null,
    val catalogUpdatedAtMillis: Long? = null,
    val catalogMessage: String? = null,
    val adminReport: AdminReport? = null,
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
    val requestedStop = activeStopRequest?.let { request ->
        selectedRoute?.stops?.getOrNull(request.requestedStopIndex)
    }
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
    private val stopRequestInput = DemoStopRequestInput()
    private val demoRoutes = DemoTransitData.routes(application)
    private val demoFareTypes = DemoTransitData.fareTypes(application)
    private val isDebuggable = application.applicationInfo.flags and
        ApplicationInfo.FLAG_DEBUGGABLE != 0
    private var syncRuntimeConfig = SyncRuntimeConfig.demo()
    private var syncClient = createTransitSyncClient(syncRuntimeConfig)
    private var demoSyncClient = syncClient as? DemoTransitSyncClient
    private var managedCatalogClient: ManagedCatalogClient? = null

    var uiState by mutableStateOf(createInitialState())
        private set

    init {
        if (uiState.isShiftActive) {
            stopRequestInput.start(::onStopRequested)
        }
    }

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
        } ?: pdfTicketPrinter.pairedPrinters().first()
        uiState = uiState.copy(
            pairedPrinters = printers,
            selectedPrinter = selectedPrinter,
            printerMessage = when {
                bluetoothPermissionMissing ->
                    text(R.string.vm_pdf_bluetooth_permission)
                bluetoothPrinters.isEmpty() ->
                    text(R.string.vm_pdf_no_printers)
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
            gpsMessage = text(R.string.vm_allow_location),
            activeStopRequest = null,
            stopRequestMessage = null,
            tickets = emptyList(),
            pendingTicketCount = repository.pendingTicketCount(),
            pendingShiftCount = repository.pendingShiftCount(),
            syncableTicketCount = repository.pendingTicketsForSync(shift.id).size,
            syncMessage = null,
            lastClosedSummary = null,
            lastClosedRoute = null,
            lastClosedRouteProgress = null
        )

        repository.saveActiveShift(shift)
        repository.saveRouteProgress(initialProgress)
        repository.clearStopRequest()
        stopRequestInput.start(::onStopRequested)
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
                text(R.string.vm_gps_active)
            } else {
                text(R.string.vm_gps_unavailable)
            }
        )
    }

    fun stopGpsTracking() {
        gpsTracker.stop()
        if (uiState.isGpsTracking) {
            uiState = uiState.copy(
                isGpsTracking = false,
                gpsMessage = if (uiState.isShiftActive) text(R.string.vm_gps_paused) else null
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
            text(R.string.vm_stop_advanced)
        )
    }

    fun triggerDemoStopRequest() {
        stopRequestInput.trigger()
    }

    fun toggleDemoServerAvailability() {
        if (uiState.isSyncing) return
        val demoClient = demoSyncClient ?: return
        demoClient.isAvailable = !demoClient.isAvailable
        uiState = uiState.copy(
            isDemoServerAvailable = demoClient.isAvailable,
            syncMessage = if (demoClient.isAvailable) {
                text(R.string.vm_demo_online)
            } else {
                text(R.string.vm_demo_offline)
            }
        )
    }

    fun toggleSyncConfiguration() {
        if (uiState.isSyncing) return
        uiState = uiState.copy(
            isSyncConfigurationOpen = !uiState.isSyncConfigurationOpen,
            syncTokenDraft = "",
            syncMessage = null
        )
    }

    fun updateSyncEndpointDraft(value: String) {
        if (!uiState.isSyncConfigurationOpen) return
        uiState = uiState.copy(syncEndpointDraft = value, syncMessage = null)
    }

    fun updateSyncTokenDraft(value: String) {
        if (!uiState.isSyncConfigurationOpen) return
        uiState = uiState.copy(syncTokenDraft = value, syncMessage = null)
    }

    fun activateConfiguredSyncServer() {
        if (uiState.isSyncing || !uiState.isSyncConfigurationOpen) return
        val endpoint = uiState.syncEndpointDraft.trim()
        val token = uiState.syncTokenDraft
        val config = runCatching {
            val candidate = if (endpoint.startsWith("https://", ignoreCase = true)) {
                SyncRuntimeConfig.production(endpoint, token)
            } else {
                check(isDebuggable) { text(R.string.vm_loopback_debug_only) }
                SyncRuntimeConfig.localValidation(endpoint, token)
            }
            createTransitSyncClient(candidate)
            createManagedCatalogClient(candidate)
            candidate
        }.getOrElse { failure ->
            uiState = uiState.copy(
                syncTokenDraft = "",
                syncMessage = text(
                    R.string.vm_config_rejected,
                    failure.message ?: text(R.string.vm_invalid_endpoint)
                )
            )
            return
        }

        syncRuntimeConfig = config
        syncClient = createTransitSyncClient(config)
        managedCatalogClient = createManagedCatalogClient(config)
        demoSyncClient = null
        uiState = uiState.copy(
            syncRuntimeMode = config.mode,
            isDemoSyncMode = false,
            isDemoServerAvailable = false,
            isSyncConfigurationOpen = false,
            syncTokenDraft = "",
            syncMessage = if (config.mode == SyncRuntimeMode.LOCAL_VALIDATION) {
                text(R.string.vm_local_server_configured)
            } else {
                text(R.string.vm_production_server_configured)
            }
        )
    }

    fun useDemoSyncMode() {
        if (uiState.isSyncing) return
        syncRuntimeConfig = SyncRuntimeConfig.demo()
        syncClient = createTransitSyncClient(syncRuntimeConfig)
        demoSyncClient = syncClient as DemoTransitSyncClient
        managedCatalogClient = null
        uiState = uiState.copy(
            syncRuntimeMode = SyncRuntimeMode.DEMO,
            isDemoSyncMode = true,
            isDemoServerAvailable = demoSyncClient?.isAvailable == true,
            isSyncConfigurationOpen = false,
            syncTokenDraft = "",
            syncMessage = text(R.string.vm_demo_restored)
        )
    }

    fun syncPendingData() {
        if (uiState.isSyncing) return
        val shifts = repository.pendingClosedShifts()
        val tickets = repository.pendingTicketsForSync(uiState.activeShift?.id)
        if (shifts.isEmpty() && tickets.isEmpty()) {
            uiState = uiState.copy(
                syncMessage = if (uiState.isShiftActive && uiState.pendingTicketCount > 0) {
                    text(R.string.vm_active_tickets_wait)
                } else {
                    text(R.string.vm_no_sync_data)
                }
            )
            return
        }

        val batch = createSyncBatch(shifts, tickets)
        uiState = uiState.copy(
            isSyncing = true,
            syncMessage = text(R.string.vm_sending_sync, shifts.size, tickets.size)
        )
        viewModelScope.launch {
            when (val result = syncClient.sync(batch)) {
                is SyncResult.Success -> {
                    repository.markSyncAcknowledged(
                        shiftIds = result.acknowledgement.acknowledgedShiftIds,
                        ticketIds = result.acknowledgement.acknowledgedTicketIds
                    )
                    refreshSyncState(
                        message = text(
                            R.string.vm_sync_complete,
                            result.acknowledgement.acknowledgedShiftIds.size,
                            result.acknowledgement.acknowledgedTicketIds.size
                        )
                    )
                }

                is SyncResult.Failure -> {
                    refreshSyncState(message = text(R.string.vm_sync_failed, result.message))
                }
            }
        }
    }

    fun refreshManagedCatalog() {
        if (uiState.isShiftActive || uiState.isCatalogRefreshing) return
        val client = managedCatalogClient
        if (client == null) {
            uiState = uiState.copy(catalogMessage = text(R.string.vm_catalog_requires_server))
            return
        }
        uiState = uiState.copy(
            isCatalogRefreshing = true,
            catalogMessage = text(R.string.vm_catalog_loading)
        )
        viewModelScope.launch {
            when (val result = client.fetch()) {
                is CatalogRefreshResult.Success -> applyManagedCatalog(result.catalog)
                is CatalogRefreshResult.Failure -> {
                    uiState = uiState.copy(
                        isCatalogRefreshing = false,
                        catalogMessage = text(R.string.vm_catalog_failed, result.message)
                    )
                }
            }
        }
    }

    fun refreshAdminReport(filter: AdminReportFilter = AdminReportFilter()) {
        uiState = uiState.copy(adminReport = createAdminReport(filter = filter))
    }

    fun sellTicket() {
        val shift = uiState.activeShift ?: return
        val fareType = uiState.selectedFareType ?: return
        if (uiState.isPrinting) return
        if (uiState.selectedPrinter == null) {
            uiState = uiState.copy(printerMessage = text(R.string.vm_select_printer_selling))
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

    fun endShift(declaredCashCents: Int) {
        if (!uiState.isShiftActive || uiState.isPrinting || uiState.unprintedTickets.isNotEmpty()) {
            return
        }
        if (declaredCashCents !in 0..100_000_000) return

        stopGpsTracking()
        stopRequestInput.stop()
        val closedRoute = uiState.selectedRoute
        val closedRouteProgress = uiState.routeProgress
        val reconciledAtMillis = System.currentTimeMillis()
        val closedShift = uiState.activeShift?.copy(
            endedAtMillis = reconciledAtMillis,
            synced = false,
            expectedCashCents = uiState.cashTotalCents,
            declaredCashCents = declaredCashCents,
            reconciledAtMillis = reconciledAtMillis
        )
        closedShift?.let(repository::saveClosedShift)
        repository.clearActiveShift()

        uiState = uiState.copy(
            activeShift = null,
            routeProgress = null,
            isGpsTracking = false,
            gpsMessage = null,
            activeStopRequest = null,
            stopRequestMessage = null,
            tickets = emptyList(),
            pendingTicketCount = repository.pendingTicketCount(),
            pendingShiftCount = repository.pendingShiftCount(),
            syncableTicketCount = repository.pendingTicketsForSync(activeShiftId = null).size,
            syncMessage = text(R.string.vm_shift_closed, uiState.ticketCount),
            adminReport = createAdminReport(activeShiftId = null),
            lastClosedSummary = DriverShiftSummary(
                ticketCount = uiState.ticketCount,
                cashTotalCents = uiState.cashTotalCents,
                fareTypeSummaries = uiState.fareTypeSummaries,
                declaredCashCents = declaredCashCents,
                cashVarianceCents = declaredCashCents - uiState.cashTotalCents,
                cashReconciliationStatus = requireNotNull(closedShift).cashReconciliationStatus
            ),
            lastClosedRoute = closedRoute,
            lastClosedRouteProgress = closedRouteProgress
        )
    }

    private fun createInitialState(): DriverShiftUiState {
        val managedCatalog = repository.loadManagedCatalog()?.takeIf(ManagedCatalog::isOperationallyValid)
        val availableDrivers = managedCatalog?.drivers ?: DemoTransitData.drivers
        val availableBuses = managedCatalog?.buses ?: DemoTransitData.buses
        val availableRoutes = managedCatalog?.routes ?: demoRoutes
        val availableFares = managedCatalog?.fareTypes ?: demoFareTypes
        val restoredShift = repository.loadActiveShift()
        val restoredDriver = restoredShift?.let { shift ->
            availableDrivers.firstOrNull { it.id == shift.driverId }
        } ?: repository.loadSignedInDriver()?.let { signedInDriver ->
            availableDrivers.firstOrNull { it.id == signedInDriver.id } ?: signedInDriver
        }
        val restoredTickets = restoredShift?.let { shift ->
            repository.loadTicketsForShift(shift.id)
        }.orEmpty()
        val restoredProgress = restoredShift?.let { shift ->
            repository.loadRouteProgress(shift.id)
        }
        val storedStopRequest = restoredShift?.let { shift ->
            repository.loadStopRequest(shift.id)
        }
        val restoredStopRequest = storedStopRequest?.takeUnless { request ->
            restoredProgress?.let { progress ->
                isStopRequestReached(request, progress)
            } == true
        }
        if (storedStopRequest != null && restoredStopRequest == null) {
            repository.clearStopRequest()
        }

        return DriverShiftUiState(
            availableDrivers = availableDrivers,
            selectedDriver = restoredDriver ?: availableDrivers.first(),
            signedInDriver = restoredDriver,
            buses = availableBuses,
            routes = availableRoutes,
            selectedBus = restoredShift?.let { shift ->
                availableBuses.firstOrNull { it.id == shift.busId }
            } ?: availableBuses.first(),
            selectedRoute = restoredShift?.let { shift ->
                availableRoutes.firstOrNull { it.id == shift.routeId }
            } ?: availableRoutes.first(),
            activeShift = restoredShift,
            routeProgress = restoredProgress ?: restoredShift?.let { shift ->
                RouteProgress(
                    shiftId = shift.id,
                    currentStopIndex = 0,
                    updatedAtMillis = shift.startedAtMillis,
                    source = RouteProgressSource.SHIFT_START
                )
            },
            gpsMessage = restoredShift?.let { text(R.string.vm_resume_location) },
            activeStopRequest = restoredStopRequest,
            stopRequestMessage = restoredStopRequest?.let {
                text(R.string.vm_stop_request_restored)
            },
            fareTypes = availableFares,
            selectedFareType = availableFares.first(),
            pairedPrinters = pdfTicketPrinter.pairedPrinters(),
            selectedPrinter = repository.loadPrinter() ?: pdfTicketPrinter.pairedPrinters().first(),
            tickets = restoredTickets,
            pendingTicketCount = repository.pendingTicketCount(),
            pendingShiftCount = repository.pendingShiftCount(),
            syncableTicketCount = repository.pendingTicketsForSync(restoredShift?.id).size,
            syncRuntimeMode = syncRuntimeConfig.mode,
            isDemoSyncMode = syncRuntimeConfig.mode == SyncRuntimeMode.DEMO,
            isDemoServerAvailable = demoSyncClient?.isAvailable == true,
            canUseLocalValidationServer = isDebuggable,
            catalogRevision = managedCatalog?.revision,
            catalogUpdatedAtMillis = managedCatalog?.updatedAtMillis,
            adminReport = buildAdminReport(
                closedShifts = repository.closedShiftsForReporting(),
                tickets = repository.ticketsForReporting().filterNot { it.shiftId == restoredShift?.id },
                drivers = availableDrivers,
                buses = availableBuses,
                routes = availableRoutes,
                fareTypes = availableFares
            )
        )
    }

    private fun applyManagedCatalog(catalog: ManagedCatalog) {
        if (!catalog.isOperationallyValid() || uiState.isShiftActive) {
            uiState = uiState.copy(
                isCatalogRefreshing = false,
                catalogMessage = text(R.string.vm_catalog_invalid)
            )
            return
        }
        repository.saveManagedCatalog(catalog)
        val signedInDriver = uiState.signedInDriver?.let { current ->
            catalog.drivers.firstOrNull { it.id == current.id }
        }
        if (signedInDriver == null && uiState.signedInDriver != null) {
            repository.clearSignedInDriver()
        } else if (signedInDriver != null) {
            repository.saveSignedInDriver(signedInDriver)
        }
        uiState = uiState.copy(
            availableDrivers = catalog.drivers,
            selectedDriver = catalog.drivers.firstOrNull { it.id == uiState.selectedDriver?.id }
                ?: catalog.drivers.first(),
            signedInDriver = signedInDriver,
            buses = catalog.buses,
            selectedBus = catalog.buses.firstOrNull { it.id == uiState.selectedBus?.id }
                ?: catalog.buses.first(),
            routes = catalog.routes,
            selectedRoute = catalog.routes.firstOrNull { it.id == uiState.selectedRoute?.id }
                ?: catalog.routes.first(),
            fareTypes = catalog.fareTypes,
            selectedFareType = catalog.fareTypes.firstOrNull { it.id == uiState.selectedFareType?.id }
                ?: catalog.fareTypes.first(),
            isCatalogRefreshing = false,
            catalogRevision = catalog.revision,
            catalogUpdatedAtMillis = catalog.updatedAtMillis,
            catalogMessage = if (signedInDriver == null && uiState.signedInDriver != null) {
                text(R.string.vm_catalog_refreshed_signed_out, catalog.revision)
            } else {
                text(R.string.vm_catalog_refreshed, catalog.revision)
            }
        )
        uiState = uiState.copy(adminReport = createAdminReport())
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
            text(R.string.vm_gps_updated_stop)
        )
    }

    private fun onStopRequested() {
        val shift = uiState.activeShift ?: return
        val route = uiState.selectedRoute ?: return
        val progress = uiState.routeProgress ?: return
        if (uiState.activeStopRequest != null) {
            uiState = uiState.copy(stopRequestMessage = text(R.string.vm_stop_request_active))
            return
        }
        val requestedStopIndex = progress.currentStopIndex + 1
        val requestedStop = route.stops.getOrNull(requestedStopIndex)
        if (requestedStop == null) {
            uiState = uiState.copy(stopRequestMessage = text(R.string.vm_no_next_stop))
            return
        }

        val request = StopRequest(
            shiftId = shift.id,
            requestedStopIndex = requestedStopIndex,
            requestedAtMillis = System.currentTimeMillis()
        )
        repository.saveStopRequest(request)
        uiState = uiState.copy(
            activeStopRequest = request,
            stopRequestMessage = text(R.string.vm_stop_requested, requestedStop.name)
        )
    }

    private fun updateRouteProgress(progress: RouteProgress, message: String) {
        repository.saveRouteProgress(progress)
        val activeRequest = uiState.activeStopRequest
        val requestReached = activeRequest?.let { request ->
            isStopRequestReached(request, progress)
        } == true
        val requestedStopName = if (requestReached) {
            activeRequest?.let { request ->
                uiState.selectedRoute?.stops?.getOrNull(request.requestedStopIndex)?.name
            }
        } else {
            null
        }
        if (requestReached) repository.clearStopRequest()

        uiState = uiState.copy(
            routeProgress = progress,
            gpsMessage = message,
            activeStopRequest = if (requestReached) null else activeRequest,
            stopRequestMessage = if (requestReached) {
                text(
                    R.string.vm_stop_request_cleared,
                    requestedStopName ?: text(R.string.vm_requested_stop)
                )
            } else {
                uiState.stopRequestMessage
            }
        )
    }

    private fun refreshSyncState(message: String) {
        uiState = uiState.copy(
            pendingTicketCount = repository.pendingTicketCount(),
            pendingShiftCount = repository.pendingShiftCount(),
            syncableTicketCount = repository.pendingTicketsForSync(uiState.activeShift?.id).size,
            isSyncing = false,
            isDemoServerAvailable = demoSyncClient?.isAvailable == true,
            syncMessage = message,
            adminReport = createAdminReport()
        )
    }

    private fun createAdminReport(
        filter: AdminReportFilter = AdminReportFilter(),
        activeShiftId: String? = uiState.activeShift?.id
    ): AdminReport {
        val reportingTickets = repository.ticketsForReporting().filterNot { ticket ->
            ticket.shiftId == activeShiftId
        }
        return buildAdminReport(
            closedShifts = repository.closedShiftsForReporting(),
            tickets = reportingTickets,
            drivers = uiState.availableDrivers,
            buses = uiState.buses,
            routes = uiState.routes,
            fareTypes = uiState.fareTypes,
            filter = filter
        )
    }

    override fun onCleared() {
        gpsTracker.stop()
        stopRequestInput.stop()
        super.onCleared()
    }

    private fun printTicket(ticket: Ticket) {
        val printer = uiState.selectedPrinter
        if (printer == null) {
            markPrintFailed(ticket, text(R.string.vm_select_printer_printing))
            return
        }

        val fareName = uiState.fareTypes
            .firstOrNull { it.id == ticket.fareTypeId }
            ?.name
            ?: text(R.string.vm_unknown_fare)
        val printableTicket = PrintableTicket(
            ticketCode = ticket.id.removePrefix("ticket-").take(8).uppercase(),
            busPlateNumber = uiState.selectedBus?.plateNumber ?: text(R.string.vm_unknown_bus),
            routeName = uiState.selectedRoute?.name ?: text(R.string.vm_unknown_route),
            fareName = fareName,
            priceCents = ticket.priceCents,
            soldAtText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(ticket.soldAtMillis)),
            operatorName = uiState.signedInDriver?.name ?: text(R.string.vm_unknown_operator)
        )

        uiState = uiState.copy(isPrinting = true, printerMessage = text(R.string.vm_printing_ticket))
        viewModelScope.launch {
            val selectedTicketPrinter = if (PdfTicketPrinter.isPdfTestDevice(printer)) {
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
                            text(R.string.vm_pdf_created)
                        } else {
                            text(R.string.vm_ticket_printed)
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
            printerMessage = text(R.string.vm_print_failed, message)
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

    private fun text(resourceId: Int, vararg arguments: Any): String {
        return getApplication<Application>().getString(resourceId, *arguments)
    }

}
