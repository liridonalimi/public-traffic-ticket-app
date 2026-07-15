package com.buspay.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.buspay.app.device.PdfTicketPrinter
import com.buspay.app.device.PrinterDevice
import com.buspay.app.domain.Bus
import com.buspay.app.domain.AdminReport
import com.buspay.app.domain.Driver
import com.buspay.app.domain.FareType
import com.buspay.app.domain.Route
import com.buspay.app.data.SyncRuntimeMode
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun DriverHomeScreen(viewModel: DriverShiftViewModel = viewModel()) {
    val state = viewModel.uiState
    val context = LocalContext.current
    var showPassengerDisplay by remember { mutableStateOf(false) }
    var showAdminReport by remember { mutableStateOf(false) }
    var hasBluetoothPermission by remember {
        mutableStateOf(hasBluetoothPrinterPermission(context))
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetoothPermission = granted
        if (granted) viewModel.refreshPrinters()
    }
    var hasLocationPermission by remember {
        mutableStateOf(hasLocationPermission(context))
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
        if (hasLocationPermission) viewModel.startGpsTracking()
    }

    LaunchedEffect(hasBluetoothPermission) {
        viewModel.refreshPrinters()
    }

    LaunchedEffect(state.isShiftActive, hasLocationPermission) {
        if (state.isShiftActive && hasLocationPermission) {
            viewModel.startGpsTracking()
        } else if (!state.isShiftActive) {
            viewModel.stopGpsTracking()
            showPassengerDisplay = false
        }
    }

    MaterialTheme {
        if (showAdminReport) {
            AdminReportScreen(
                report = state.adminReport,
                onRefresh = { viewModel.refreshAdminReport() },
                onBackToDriverConsole = { showAdminReport = false }
            )
            return@MaterialTheme
        }

        if (showPassengerDisplay && state.canOpenPassengerDisplay) {
            PassengerDisplay(
                state = state,
                onDemoStopRequest = viewModel::triggerDemoStopRequest,
                onBackToDriverConsole = { showPassengerDisplay = false }
            )
            return@MaterialTheme
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "Driver Console",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BusPay native pilot",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = "Driver", fontWeight = FontWeight.Bold)
                    if (state.signedInDriver == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectorCard(
                            title = "Sign in driver",
                            selectedText = state.selectedDriver?.name ?: "Select driver",
                            enabled = !state.isShiftActive,
                            items = state.availableDrivers,
                            itemText = Driver::name,
                            onItemSelected = viewModel::selectDriver
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::signInDriver,
                            enabled = state.selectedDriver != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign In")
                        }
                    } else {
                        state.signedInDriver?.let { driver ->
                            Text(text = driver.name)
                            Text(text = "ID: ${driver.id}")
                        }

                        if (!state.isShiftActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = viewModel::signOutDriver,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SelectorCard(
                        title = "Bus",
                        selectedText = state.selectedBus?.plateNumber ?: "Select bus",
                        enabled = !state.isShiftActive,
                        items = state.buses,
                        itemText = Bus::plateNumber,
                        onItemSelected = viewModel::selectBus
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectorCard(
                        title = "Route",
                        selectedText = state.selectedRoute?.name ?: "Select route",
                        enabled = !state.isShiftActive,
                        items = state.routes,
                        itemText = Route::name,
                        onItemSelected = viewModel::selectRoute
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Ticket printer", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectorCard(
                        title = "Ticket output",
                        selectedText = state.selectedPrinter?.let(::printerDisplayName)
                            ?: "Select printer",
                        enabled = !state.isPrinting && state.pairedPrinters.isNotEmpty(),
                        items = state.pairedPrinters,
                        itemText = ::printerDisplayName,
                        onItemSelected = viewModel::selectPrinter
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (hasBluetoothPermission) {
                        OutlinedButton(
                            onClick = viewModel::refreshPrinters,
                            enabled = !state.isPrinting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh Paired Printers")
                        }
                    } else {
                        Text("PDF testing works without Bluetooth. Allow Bluetooth only for a physical printer.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                bluetoothPermissionLauncher.launch(BLUETOOTH_CONNECT_PERMISSION)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Bluetooth Printer")
                        }
                    }

                    state.printerMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = message)
                    }

                    state.lastPdfPath?.let { pdfPath ->
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { openTicketPdf(context, pdfPath) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Last Ticket PDF")
                        }
                    }

                    if (state.unprintedTickets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.unprintedTickets.size} ticket(s) waiting to print",
                            fontWeight = FontWeight.Bold
                        )
                        state.unprintedTickets.last().lastPrintError?.let { error ->
                            Text(text = "Last error: $error")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::retryLastUnprintedTicket,
                            enabled = state.selectedPrinter != null && !state.isPrinting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Last Print")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Route progress", fontWeight = FontWeight.Bold)
                    if (state.isShiftActive) {
                        Text(
                            text = "Current: ${state.routeStopStatus.currentStop?.name ?: "Unknown"}"
                        )
                        Text(
                            text = state.routeStopStatus.nextStop?.let { "Next: ${it.name}" }
                                ?: "Final stop reached"
                        )
                        state.gpsMessage?.let { Text(text = it) }
                        state.requestedStop?.let { requestedStop ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = "STOP REQUESTED", fontWeight = FontWeight.Bold)
                                    Text(text = requestedStop.name)
                                }
                            }
                        }
                        state.stopRequestMessage?.let { message ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = message)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!hasLocationPermission) {
                            Button(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Allow GPS Route Tracking")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = viewModel::advanceToNextStop,
                            enabled = !state.routeStopStatus.isRouteComplete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Advance Stop (Demo)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::triggerDemoStopRequest,
                            enabled = !state.routeStopStatus.isRouteComplete &&
                                state.activeStopRequest == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Press Stop Button (Demo)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showPassengerDisplay = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Passenger Display")
                        }
                    } else {
                        if (state.canOpenPassengerDisplay) {
                            Text(text = "The last shift has ended")
                            Text(
                                text = "Last stop: " +
                                    (state.passengerRouteStopStatus.currentStop?.name ?: "Unknown")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showPassengerDisplay = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Last Passenger Display")
                            }
                        } else {
                            Text(text = "Start a shift to begin tracking")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Current shift tickets", fontWeight = FontWeight.Bold)
                    Text(text = state.ticketCount.toString())

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Cash total", fontWeight = FontWeight.Bold)
                    Text(text = formatEuroCents(state.cashTotalCents))

                    if (state.isShiftActive) {
                        Spacer(modifier = Modifier.height(20.dp))
                        SelectorCard(
                            title = "Ticket fare",
                            selectedText = state.selectedFareType?.let {
                                "${it.name} - ${formatEuroCents(it.priceCents)}"
                            } ?: "Select fare",
                            enabled = true,
                            items = state.fareTypes,
                            itemText = { fare: FareType ->
                                "${fare.name} - ${formatEuroCents(fare.priceCents)}"
                            },
                            onItemSelected = viewModel::selectFareType
                        )
                        state.selectedFareType?.eligibility?.let { eligibility ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = eligibility,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (state.fareTypeSummaries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "Sales by fare", fontWeight = FontWeight.Bold)
                        state.fareTypeSummaries.forEach { summary ->
                            Text(
                                text = "${summary.fareName}: ${summary.ticketCount} / " +
                                    formatEuroCents(summary.cashTotalCents)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Total waiting for sync", fontWeight = FontWeight.Bold)
                    Text(text = "${state.pendingShiftCount} closed shift(s)")
                    Text(text = "${state.pendingTicketCount} ticket(s) saved locally")
                    if (state.isShiftActive &&
                        state.pendingTicketCount > state.syncableTicketCount
                    ) {
                        Text(
                            text = "Current-shift tickets wait until shift closure",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Sync service", fontWeight = FontWeight.Bold)
                            Text(
                                text = when (state.syncRuntimeMode) {
                                    SyncRuntimeMode.DEMO -> "Active mode: Demo validation"
                                    SyncRuntimeMode.LOCAL_VALIDATION ->
                                        "Active mode: Local server validation"
                                    SyncRuntimeMode.PRODUCTION -> "Active mode: Production HTTPS"
                                }
                            )
                            Text(text = "Production HTTPS contract v1: ready")
                            Text(text = "Reference API/database: implemented • deployment pending")
                            Text(text = "Deployment package: ready • infrastructure selection pending")
                            Text(
                                text = if (state.canUseLocalValidationServer) {
                                    "Debug builds can use loopback HTTP through adb reverse. " +
                                        "Production connections still require HTTPS."
                                } else {
                                    "Activation requires an HTTPS server URL and an " +
                                        "authenticated access token."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::toggleSyncConfiguration,
                        enabled = !state.isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isSyncConfigurationOpen) "Cancel Server Setup" else "Configure Sync Server")
                    }
                    if (state.isSyncConfigurationOpen) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Authenticated sync server", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = state.syncEndpointDraft,
                                    onValueChange = viewModel::updateSyncEndpointDraft,
                                    label = { Text("Sync endpoint") },
                                    placeholder = { Text("https://server.example/v1/sync") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = state.syncTokenDraft,
                                    onValueChange = viewModel::updateSyncTokenDraft,
                                    label = { Text("Access token") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "The token stays only in this running app session and " +
                                        "is cleared from this field after activation.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = viewModel::activateConfiguredSyncServer,
                                    enabled = state.syncEndpointDraft.isNotBlank() &&
                                        state.syncTokenDraft.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Activate Server")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (state.syncRuntimeMode) {
                            SyncRuntimeMode.LOCAL_VALIDATION -> "Local validation server: configured"
                            SyncRuntimeMode.PRODUCTION -> "Production service: configured"
                            SyncRuntimeMode.DEMO -> if (state.isDemoServerAvailable) {
                                "Demo server: online"
                            } else {
                                "Demo server: offline"
                            }
                        }
                    )
                    state.syncMessage?.let { message ->
                        Text(text = message)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = if (state.isDemoSyncMode) {
                                viewModel::toggleDemoServerAvailability
                            } else {
                                viewModel::useDemoSyncMode
                            },
                            enabled = !state.isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (!state.isDemoSyncMode) {
                                    "Use Demo Mode"
                                } else if (state.isDemoServerAvailable) {
                                    "Go Offline"
                                } else {
                                    "Go Online"
                                }
                            )
                        }
                        Button(
                            onClick = viewModel::syncPendingData,
                            enabled = !state.isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (state.isSyncing) "Syncing…" else "Sync Now")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.refreshAdminReport()
                            showAdminReport = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Admin Report Preview")
                    }

                    state.lastClosedSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "Last closed shift", fontWeight = FontWeight.Bold)
                        Text(text = "${summary.ticketCount} tickets / ${formatEuroCents(summary.cashTotalCents)}")
                        summary.fareTypeSummaries.forEach { fareSummary ->
                            Text(
                                text = "${fareSummary.fareName}: ${fareSummary.ticketCount} / " +
                                    formatEuroCents(fareSummary.cashTotalCents)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isShiftActive) {
                        OutlinedButton(
                            onClick = viewModel::endShift,
                            enabled = !state.isPrinting && state.unprintedTickets.isEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("End Shift")
                        }
                    } else {
                        Button(
                            onClick = viewModel::startShift,
                            enabled = state.isDriverSignedIn &&
                                state.selectedBus != null &&
                                state.selectedRoute != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Shift")
                        }
                    }

                    Button(
                        onClick = viewModel::sellTicket,
                        enabled = state.isShiftActive &&
                            state.selectedPrinter != null &&
                            !state.isPrinting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.isPrinting) "Printing…" else "Sell Ticket")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReportScreen(
    report: AdminReport?,
    onRefresh: () -> Unit,
    onBackToDriverConsole: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Admin Reporting Preview",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (report == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("No report is available")
            } else {
                Text(
                    "Contract v${report.contractVersion} • " +
                        formatReportDateTime(report.generatedAtMillis)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overall totals", fontWeight = FontWeight.Bold)
                        Text("Drivers: ${report.totals.driverCount}")
                        Text("Closed shifts: ${report.totals.shiftCount}")
                        Text("Tickets: ${report.totals.ticketCount}")
                        Text("Cash: ${formatEuroCents(report.totals.cashTotalCents)}")
                        Text(
                            "Sync: ${report.totals.syncedShiftCount} synced, " +
                                "${report.totals.partiallySyncedShiftCount} partial, " +
                                "${report.totals.pendingShiftCount} pending"
                        )
                        if (report.totals.fareTypeSummaries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Fares", fontWeight = FontWeight.Bold)
                            report.totals.fareTypeSummaries.forEach { fare ->
                                Text(
                                    "${fare.fareName}: ${fare.ticketCount} / " +
                                        formatEuroCents(fare.cashTotalCents)
                                )
                            }
                        }
                    }
                }

                val quality = report.dataQuality
                if (quality.unmatchedTicketCount > 0 ||
                    quality.unknownDriverShiftCount > 0 ||
                    quality.unknownBusShiftCount > 0 ||
                    quality.unknownRouteShiftCount > 0
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Data quality", fontWeight = FontWeight.Bold)
                            Text(
                                "Legacy unmatched tickets: ${quality.unmatchedTicketCount} / " +
                                    formatEuroCents(quality.unmatchedTicketCashCents)
                            )
                            Text("Unknown-driver shifts: ${quality.unknownDriverShiftCount}")
                            Text("Unknown-bus shifts: ${quality.unknownBusShiftCount}")
                            Text("Unknown-route shifts: ${quality.unknownRouteShiftCount}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Drivers", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (report.drivers.isEmpty()) Text("No closed shifts in this report")
                report.drivers.forEach { driver ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(driver.driverName, fontWeight = FontWeight.Bold)
                            Text("ID: ${driver.driverId}")
                            Text("Shifts: ${driver.shiftCount}")
                            Text("Tickets: ${driver.ticketCount}")
                            Text("Cash: ${formatEuroCents(driver.cashTotalCents)}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Shift details", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                report.shifts.forEach { shift ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(shift.driverName, fontWeight = FontWeight.Bold)
                            Text("Shift: ${shift.shiftId}")
                            Text("Bus: ${shift.busPlateNumber}")
                            Text("Route: ${shift.routeName}")
                            Text("Start: ${formatReportDateTime(shift.startedAtMillis)}")
                            Text("End: ${formatReportDateTime(shift.endedAtMillis)}")
                            Text("Duration: ${formatDuration(shift.durationMillis)}")
                            Text("Sync: ${shift.syncStatus.name.replace('_', ' ')}")
                            Text(
                                "Tickets: ${shift.ticketCount} / " +
                                    formatEuroCents(shift.cashTotalCents)
                            )
                            shift.fareTypeSummaries.forEach { fare ->
                                Text(
                                    "${fare.fareName}: ${fare.ticketCount} / " +
                                        formatEuroCents(fare.cashTotalCents)
                                )
                            }
                            if (shift.tickets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ticket records", fontWeight = FontWeight.Bold)
                                shift.tickets.forEach { ticket ->
                                    Text(
                                        "${ticket.ticketId.takeLast(8)} • ${ticket.fareName} • " +
                                            formatEuroCents(ticket.priceCents) +
                                            if (ticket.synced) " • synced" else " • pending"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh Report")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBackToDriverConsole,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Driver Console")
            }
        }
    }
}

@Composable
private fun PassengerDisplay(
    state: DriverShiftUiState,
    onDemoStopRequest: () -> Unit,
    onBackToDriverConsole: () -> Unit
) {
    val route = state.passengerRoute
    val progress = state.passengerRouteProgress
    val status = state.passengerRouteStopStatus

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = route?.name ?: "Active route",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(text = "CURRENT STOP", fontWeight = FontWeight.Bold)
                Text(
                    text = status.currentStop?.name ?: "Unknown stop",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = if (status.isRouteComplete) "ROUTE COMPLETE" else "NEXT STOP",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status.nextStop?.name ?: "Final destination",
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                val currentNumber = (progress?.currentStopIndex ?: 0) + 1
                Text(text = "Stop $currentNumber of ${route?.stops?.size ?: 0}")
                if (!state.isShiftActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "SHIFT ENDED", fontWeight = FontWeight.Bold)
                }
                state.requestedStop?.let { requestedStop ->
                    Spacer(modifier = Modifier.height(28.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "STOP REQUESTED",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = requestedStop.name,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isShiftActive &&
                    !status.isRouteComplete &&
                    state.activeStopRequest == null
                ) {
                    Button(
                        onClick = onDemoStopRequest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request Stop (Demo)")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedButton(onClick = onBackToDriverConsole) {
                    Text("Back to Driver Console")
                }
            }
        }
    }
}

@Composable
private fun <T> SelectorCard(
    title: String,
    selectedText: String,
    enabled: Boolean,
    items: List<T>,
    itemText: (T) -> String,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedText)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(itemText(item)) },
                            onClick = {
                                expanded = false
                                onItemSelected(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatEuroCents(cents: Int): String {
    val euros = cents / 100
    val remainder = cents % 100
    return "EUR $euros.${remainder.toString().padStart(2, '0')}"
}

private fun formatReportDateTime(timestampMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestampMillis))
}

private fun formatDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun hasBluetoothPrinterPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(
            context,
            BLUETOOTH_CONNECT_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun printerDisplayName(printer: PrinterDevice): String {
    return if (printer.address == PdfTicketPrinter.TEST_DEVICE.address) {
        printer.name
    } else {
        "${printer.name} (${printer.address})"
    }
}

private fun openTicketPdf(context: Context, path: String) {
    val file = File(path)
    if (!file.exists()) {
        Toast.makeText(context, "The ticket PDF no longer exists", Toast.LENGTH_LONG).show()
        return
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Install a PDF viewer to open the ticket",
            Toast.LENGTH_LONG
        ).show()
    }
}

private const val BLUETOOTH_CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT"
