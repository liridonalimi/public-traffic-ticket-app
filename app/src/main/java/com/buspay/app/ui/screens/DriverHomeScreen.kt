package com.buspay.app.ui.screens

import android.content.ActivityNotFoundException
import android.app.Activity
import android.app.Application
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.annotation.StringRes
import com.buspay.app.R
import com.buspay.app.AppLanguage
import com.buspay.app.AppLanguageManager
import com.buspay.app.device.PdfTicketPrinter
import com.buspay.app.device.PrinterDevice
import com.buspay.app.domain.Bus
import com.buspay.app.domain.AdminReport
import com.buspay.app.domain.CashReconciliationStatus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.DriverDuty
import com.buspay.app.domain.FareType
import com.buspay.app.domain.Route
import com.buspay.app.domain.ReportingSyncStatus
import com.buspay.app.domain.parseEuroAmountToCents
import com.buspay.app.data.SyncRuntimeMode
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DriverHomeScreen(viewModel: DriverShiftViewModel = viewModel()) {
    val state = viewModel.uiState
    val context = LocalContext.current
    var showPassengerDisplay by remember { mutableStateOf(false) }
    var showAdminReport by remember { mutableStateOf(false) }
    var workspace by rememberSaveable { mutableStateOf(PilotWorkspace.DRIVER) }
    var showEndShiftConfirmation by remember { mutableStateOf(false) }
    var cashHandoverDraft by remember { mutableStateOf("") }
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
        if (workspace == PilotWorkspace.OPERATIONS && canOpenOperationsTools(state)) {
            if (showAdminReport) {
                AdminReportScreen(
                    report = state.adminReport,
                    onRefresh = { viewModel.refreshAdminReport() },
                    onBackToDriverConsole = { showAdminReport = false },
                    backLabel = R.string.back_operations_tools
                )
            } else {
                OperationsToolsScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenAdminReport = {
                        viewModel.refreshAdminReport()
                        showAdminReport = true
                    },
                    onBackToDriverConsole = { workspace = PilotWorkspace.DRIVER }
                )
            }
            return@MaterialTheme
        }

        if (showAdminReport) {
            AdminReportScreen(
                report = state.adminReport,
                onRefresh = { viewModel.refreshAdminReport() },
                onBackToDriverConsole = { showAdminReport = false },
                backLabel = R.string.back_driver_console
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
                        text = stringResource(R.string.driver_console),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.native_pilot),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = localizedDriverShiftStatus(state),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (state.isShiftActive) {
                                        stringResource(R.string.stay_on_driver_screen)
                                    } else {
                                        stringResource(R.string.confirm_departure_details)
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (state.isShiftActive) R.string.in_service else R.string.pilot
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = stringResource(R.string.driver), fontWeight = FontWeight.Bold)
                    if (state.signedInDriver == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectorCard(
                            title = stringResource(R.string.sign_in_driver),
                            selectedText = state.selectedDriver?.name
                                ?: stringResource(R.string.select_driver),
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
                            Text(stringResource(R.string.sign_in))
                        }
                    } else {
                        state.signedInDriver?.let { driver ->
                            Text(text = driver.name)
                            Text(text = stringResource(R.string.id_value, driver.id))
                        }

                        if (!state.isShiftActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = viewModel::signOutDriver,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.sign_out))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.isDriverSignedIn) {
                        Text(
                            text = stringResource(R.string.scheduled_duties),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.duties.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_scheduled_duties),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            SelectorCard(
                                title = stringResource(R.string.assigned_trip),
                                selectedText = state.selectedDuty?.let(::driverDutyText)
                                    ?: stringResource(R.string.use_ad_hoc_shift),
                                enabled = !state.isShiftActive,
                                items = state.duties,
                                itemText = ::driverDutyText,
                                onItemSelected = viewModel::selectDuty
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.selectDuty(null) },
                                enabled = !state.isShiftActive && state.selectedDuty != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.use_ad_hoc_shift))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    SelectorCard(
                        title = stringResource(R.string.bus),
                        selectedText = state.selectedBus?.plateNumber
                            ?: stringResource(R.string.select_bus),
                        enabled = !state.isShiftActive && state.selectedDuty == null,
                        items = state.buses,
                        itemText = Bus::plateNumber,
                        onItemSelected = viewModel::selectBus
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SelectorCard(
                        title = stringResource(R.string.route),
                        selectedText = state.selectedRoute?.name
                            ?: stringResource(R.string.select_route),
                        enabled = !state.isShiftActive && state.selectedDuty == null,
                        items = state.routes,
                        itemText = Route::name,
                        onItemSelected = viewModel::selectRoute
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = stringResource(R.string.ticket_printer), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectorCard(
                        title = stringResource(R.string.ticket_output),
                        selectedText = state.selectedPrinter?.let(::printerDisplayName)
                            ?: stringResource(R.string.select_printer),
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
                            Text(stringResource(R.string.refresh_paired_printers))
                        }
                    } else {
                        Text(stringResource(R.string.pdf_without_bluetooth))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                bluetoothPermissionLauncher.launch(BLUETOOTH_CONNECT_PERMISSION)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.allow_bluetooth_printer))
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
                            Text(stringResource(R.string.open_last_ticket_pdf))
                        }
                    }

                    if (state.unprintedTickets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pluralStringResource(
                                R.plurals.tickets_waiting_to_print,
                                state.unprintedTickets.size,
                                state.unprintedTickets.size
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        state.unprintedTickets.last().lastPrintError?.let { error ->
                            Text(text = stringResource(R.string.last_error, error))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::retryLastUnprintedTicket,
                            enabled = state.selectedPrinter != null && !state.isPrinting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.retry_last_print))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = stringResource(R.string.route_progress), fontWeight = FontWeight.Bold)
                    if (state.isShiftActive) {
                        Text(
                            text = stringResource(
                                R.string.current_stop_value,
                                state.routeStopStatus.currentStop?.name
                                    ?: stringResource(R.string.unknown)
                            )
                        )
                        Text(
                            text = state.routeStopStatus.nextStop?.let {
                                stringResource(R.string.next_stop_value, it.name)
                            } ?: stringResource(R.string.final_stop_reached)
                        )
                        state.gpsMessage?.let { Text(text = it) }
                        state.requestedStop?.let { requestedStop ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.stop_requested),
                                        fontWeight = FontWeight.Bold
                                    )
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
                                Text(stringResource(R.string.allow_gps_tracking))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = viewModel::advanceToNextStop,
                            enabled = !state.routeStopStatus.isRouteComplete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.advance_stop_demo))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::triggerDemoStopRequest,
                            enabled = !state.routeStopStatus.isRouteComplete &&
                                state.activeStopRequest == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.press_stop_button_demo))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showPassengerDisplay = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.open_passenger_display))
                        }
                    } else {
                        if (state.canOpenPassengerDisplay) {
                            Text(text = stringResource(R.string.last_shift_ended))
                            Text(
                                text = stringResource(
                                    R.string.last_stop_value,
                                    state.passengerRouteStopStatus.currentStop?.name
                                        ?: stringResource(R.string.unknown)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showPassengerDisplay = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.open_last_passenger_display))
                            }
                        } else {
                            Text(text = stringResource(R.string.start_shift_for_tracking))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.current_shift_tickets),
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = state.ticketCount.toString())

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = stringResource(R.string.cash_total), fontWeight = FontWeight.Bold)
                    Text(text = formatEuroCents(state.cashTotalCents))

                    if (state.isShiftActive) {
                        Spacer(modifier = Modifier.height(20.dp))
                        SelectorCard(
                            title = stringResource(R.string.ticket_fare),
                            selectedText = state.selectedFareType?.let {
                                "${it.name} - ${formatEuroCents(it.priceCents)}"
                            } ?: stringResource(R.string.select_fare),
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
                        Text(text = stringResource(R.string.sales_by_fare), fontWeight = FontWeight.Bold)
                        state.fareTypeSummaries.forEach { summary ->
                            Text(
                                text = stringResource(
                                    R.string.fare_summary,
                                    summary.fareName,
                                    summary.ticketCount,
                                    formatEuroCents(summary.cashTotalCents)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = stringResource(R.string.synchronization), fontWeight = FontWeight.Bold)
                    Text(
                        text = pluralStringResource(
                            R.plurals.closed_shifts_count,
                            state.pendingShiftCount,
                            state.pendingShiftCount
                        )
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.local_tickets_count,
                            state.pendingTicketCount,
                            state.pendingTicketCount
                        )
                    )
                    if (state.isShiftActive &&
                        state.pendingTicketCount > state.syncableTicketCount
                    ) {
                        Text(
                            text = stringResource(R.string.active_shift_tickets_wait),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = localizedDriverSyncStatus(state))
                    state.syncMessage?.let { message ->
                        Text(text = message)
                    }
                    if (!state.isShiftActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.pendingShiftCount > 0) {
                            Button(
                                onClick = viewModel::syncPendingData,
                                enabled = !state.isSyncing,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(
                                        if (state.isSyncing) {
                                            R.string.synchronizing
                                        } else {
                                            R.string.sync_closed_shifts
                                        }
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = { workspace = PilotWorkspace.OPERATIONS },
                            enabled = canOpenOperationsTools(state),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.operations_setup))
                        }
                        Text(
                            text = stringResource(R.string.supervisor_tools_separated),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    state.lastClosedSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = stringResource(R.string.last_closed_shift), fontWeight = FontWeight.Bold)
                        Text(
                            text = stringResource(
                                R.string.tickets_and_cash,
                                summary.ticketCount,
                                formatEuroCents(summary.cashTotalCents)
                            )
                        )
                        summary.fareTypeSummaries.forEach { fareSummary ->
                            Text(
                                text = stringResource(
                                    R.string.fare_summary,
                                    fareSummary.fareName,
                                    fareSummary.ticketCount,
                                    formatEuroCents(fareSummary.cashTotalCents)
                                )
                            )
                        }
                        summary.declaredCashCents?.let { declared ->
                            Text(
                                stringResource(
                                    R.string.cash_handover_summary,
                                    formatEuroCents(summary.cashTotalCents),
                                    formatEuroCents(declared),
                                    formatSignedEuroCents(summary.cashVarianceCents ?: 0),
                                    localizedCashReconciliationStatus(summary.cashReconciliationStatus)
                                )
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
                            onClick = {
                                cashHandoverDraft = ""
                                showEndShiftConfirmation = true
                            },
                            enabled = !state.isPrinting && state.unprintedTickets.isEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.end_shift))
                        }
                    } else {
                        Button(
                            onClick = viewModel::startShift,
                            enabled = state.isDriverSignedIn &&
                                state.selectedBus != null &&
                                state.selectedRoute != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.start_shift))
                        }
                    }

                    Button(
                        onClick = viewModel::sellTicket,
                        enabled = state.isShiftActive &&
                            state.selectedPrinter != null &&
                            !state.isPrinting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(
                                if (state.isPrinting) R.string.printing else R.string.sell_ticket
                            )
                        )
                    }
                }
            }
        }

        if (showEndShiftConfirmation) {
            val declaredCashCents = parseEuroAmountToCents(cashHandoverDraft)
            val varianceCents = declaredCashCents?.minus(state.cashTotalCents)
            AlertDialog(
                onDismissRequest = { showEndShiftConfirmation = false },
                title = { Text(stringResource(R.string.end_shift_question)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.end_shift_explanation))
                        Text(
                            stringResource(
                                R.string.expected_cash_value,
                                formatEuroCents(state.cashTotalCents)
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = cashHandoverDraft,
                            onValueChange = { cashHandoverDraft = it },
                            label = { Text(stringResource(R.string.counted_cash_eur)) },
                            supportingText = {
                                Text(
                                    stringResource(
                                        if (cashHandoverDraft.isNotBlank() && declaredCashCents == null) {
                                            R.string.invalid_cash_amount
                                        } else {
                                            R.string.cash_count_guidance
                                        }
                                    )
                                )
                            },
                            isError = cashHandoverDraft.isNotBlank() && declaredCashCents == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        varianceCents?.let { variance ->
                            Text(
                                stringResource(
                                    R.string.cash_variance_value,
                                    formatSignedEuroCents(variance)
                                ),
                                color = if (variance == 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = declaredCashCents != null,
                        onClick = {
                            showEndShiftConfirmation = false
                            viewModel.endShift(requireNotNull(declaredCashCents))
                        }
                    ) {
                        Text(stringResource(R.string.confirm_cash_handover))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndShiftConfirmation = false }) {
                        Text(stringResource(R.string.keep_shift_open))
                    }
                }
            )
        }
    }
}

@Composable
private fun OperationsToolsScreen(
    state: DriverShiftUiState,
    viewModel: DriverShiftViewModel,
    onOpenAdminReport: () -> Unit,
    onBackToDriverConsole: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedLanguage by remember {
        mutableStateOf(AppLanguageManager.selectedLanguage(context))
    }
    val languageOptions = listOf(
        AppLanguageOption(
            language = AppLanguage.SYSTEM,
            label = stringResource(R.string.language_system_default)
        ),
        AppLanguageOption(
            language = AppLanguage.ALBANIAN,
            label = stringResource(R.string.language_albanian)
        ),
        AppLanguageOption(
            language = AppLanguage.ENGLISH,
            label = stringResource(R.string.language_english)
        )
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.operations_setup),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.local_supervisor_workspace),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            SelectorCard(
                title = stringResource(R.string.app_language),
                selectedText = languageOptions.first { it.language == selectedLanguage }.label,
                enabled = true,
                items = languageOptions,
                itemText = AppLanguageOption::label,
                onItemSelected = { option ->
                    selectedLanguage = option.language
                    coroutineScope.launch {
                        delay(LANGUAGE_MENU_DISMISS_DELAY_MILLIS)
                        AppLanguageManager.selectLanguage(
                            application = context.applicationContext as Application,
                            selection = option.language
                        )
                        (context as? Activity)?.recreate()
                    }
                }
            )
            Text(
                text = stringResource(R.string.language_help),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.role_boundary), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.role_boundary_description))
                    Text(
                        stringResource(R.string.pilot_security_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                stringResource(R.string.managed_catalog),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = state.catalogRevision?.let { revision ->
                            stringResource(R.string.catalog_revision, revision)
                        } ?: stringResource(R.string.catalog_demo_fallback),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(
                            R.string.catalog_counts,
                            state.availableDrivers.size,
                            state.buses.size,
                            state.routes.size,
                            state.routes.sumOf { it.stops.size },
                            state.fareTypes.size
                        )
                    )
                    Text(
                        stringResource(R.string.catalog_offline_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = viewModel::refreshManagedCatalog,
                enabled = !state.isDemoSyncMode && !state.isCatalogRefreshing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (state.isCatalogRefreshing) {
                            R.string.refreshing_catalog
                        } else {
                            R.string.refresh_managed_catalog
                        }
                    )
                )
            }
            state.catalogMessage?.let { Text(it) }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                stringResource(R.string.printer_certification),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.printer_certification_required),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.printer_simulation_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        R.string.cert_pairing,
                        R.string.cert_paper,
                        R.string.cert_text,
                        R.string.cert_qr,
                        R.string.cert_retry,
                        R.string.cert_endurance
                    ).forEach { check ->
                        Text("• ${stringResource(check)}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                stringResource(R.string.sync_service),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (state.syncRuntimeMode) {
                            SyncRuntimeMode.DEMO -> stringResource(R.string.active_mode_demo)
                            SyncRuntimeMode.LOCAL_VALIDATION ->
                                stringResource(R.string.active_mode_local)
                            SyncRuntimeMode.PRODUCTION ->
                                stringResource(R.string.active_mode_production)
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text(stringResource(R.string.https_contract_ready))
                    Text(stringResource(R.string.reference_api_status))
                    Text(stringResource(R.string.deployment_status))
                    Text(
                        text = if (state.canUseLocalValidationServer) {
                            stringResource(R.string.debug_loopback_note)
                        } else {
                            stringResource(R.string.production_activation_note)
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
                Text(
                    if (state.isSyncConfigurationOpen) {
                        stringResource(R.string.cancel_server_setup)
                    } else {
                        stringResource(R.string.configure_sync_server)
                    }
                )
            }
            if (state.isSyncConfigurationOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.authenticated_sync_server),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.syncEndpointDraft,
                            onValueChange = viewModel::updateSyncEndpointDraft,
                            label = { Text(stringResource(R.string.sync_endpoint)) },
                            placeholder = {
                                Text(stringResource(R.string.sync_endpoint_placeholder))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.syncTokenDraft,
                            onValueChange = viewModel::updateSyncTokenDraft,
                            label = { Text(stringResource(R.string.access_token)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(R.string.session_token_note),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::activateConfiguredSyncServer,
                            enabled = state.syncEndpointDraft.isNotBlank() &&
                                state.syncTokenDraft.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.activate_server))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(localizedDriverSyncStatus(state))
            state.syncMessage?.let { Text(it) }
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
                            stringResource(R.string.use_demo_mode)
                        } else if (state.isDemoServerAvailable) {
                            stringResource(R.string.go_offline)
                        } else {
                            stringResource(R.string.go_online)
                        }
                    )
                }
                Button(
                    onClick = viewModel::syncPendingData,
                    enabled = !state.isSyncing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(if (state.isSyncing) R.string.syncing else R.string.sync_now)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(stringResource(R.string.reporting), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = onOpenAdminReport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.open_admin_report))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBackToDriverConsole,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.return_driver_console))
            }
        }
    }
}

private data class AppLanguageOption(
    val language: AppLanguage,
    val label: String
)

@Composable
private fun AdminReportScreen(
    report: AdminReport?,
    onRefresh: () -> Unit,
    onBackToDriverConsole: () -> Unit,
    @StringRes backLabel: Int
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_reporting_preview),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (report == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.no_report_available))
            } else {
                Text(
                    stringResource(
                        R.string.contract_generated,
                        report.contractVersion,
                        formatReportDateTime(report.generatedAtMillis)
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.overall_totals), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.drivers_count, report.totals.driverCount))
                        Text(stringResource(R.string.closed_shifts_value, report.totals.shiftCount))
                        Text(stringResource(R.string.tickets_value, report.totals.ticketCount))
                        Text(
                            stringResource(
                                R.string.cash_value,
                                formatEuroCents(report.totals.cashTotalCents)
                            )
                        )
                        Text(
                            stringResource(
                                R.string.reconciliation_totals,
                                formatEuroCents(report.totals.expectedCashTotalCents),
                                formatEuroCents(report.totals.declaredCashTotalCents),
                                formatSignedEuroCents(report.totals.cashVarianceTotalCents)
                            )
                        )
                        Text(
                            stringResource(
                                R.string.reconciliation_shift_counts,
                                report.totals.reconciledShiftCount,
                                report.totals.unreconciledShiftCount
                            )
                        )
                        Text(
                            stringResource(
                                R.string.sync_totals,
                                report.totals.syncedShiftCount,
                                report.totals.partiallySyncedShiftCount,
                                report.totals.pendingShiftCount
                            )
                        )
                        if (report.totals.fareTypeSummaries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.fares), fontWeight = FontWeight.Bold)
                            report.totals.fareTypeSummaries.forEach { fare ->
                                Text(
                                    stringResource(
                                        R.string.fare_summary,
                                        fare.fareName,
                                        fare.ticketCount,
                                        formatEuroCents(fare.cashTotalCents)
                                    )
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
                            Text(stringResource(R.string.data_quality), fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(
                                    R.string.legacy_unmatched_tickets,
                                    quality.unmatchedTicketCount,
                                    formatEuroCents(quality.unmatchedTicketCashCents)
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.unknown_driver_shifts,
                                    quality.unknownDriverShiftCount
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.unknown_bus_shifts,
                                    quality.unknownBusShiftCount
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.unknown_route_shifts,
                                    quality.unknownRouteShiftCount
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(stringResource(R.string.drivers), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (report.drivers.isEmpty()) Text(stringResource(R.string.no_closed_shifts_report))
                report.drivers.forEach { driver ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(driver.driverName, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.id_value, driver.driverId))
                            Text(stringResource(R.string.shifts_value, driver.shiftCount))
                            Text(stringResource(R.string.tickets_value, driver.ticketCount))
                            Text(
                                stringResource(
                                    R.string.cash_value,
                                    formatEuroCents(driver.cashTotalCents)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    stringResource(R.string.shift_details),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                report.shifts.forEach { shift ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(shift.driverName, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.shift_value, shift.shiftId))
                            Text(stringResource(R.string.bus_value, shift.busPlateNumber))
                            Text(stringResource(R.string.route_value, shift.routeName))
                            Text(
                                stringResource(
                                    R.string.start_value,
                                    formatReportDateTime(shift.startedAtMillis)
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.end_value,
                                    formatReportDateTime(shift.endedAtMillis)
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.duration_value,
                                    formatDuration(shift.durationMillis)
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.sync_value,
                                    localizedReportingSyncStatus(shift.syncStatus)
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.tickets_cash_value,
                                    shift.ticketCount,
                                    formatEuroCents(shift.cashTotalCents)
                                )
                            )
                            Text(
                                stringResource(
                                    R.string.cash_reconciliation_value,
                                    shift.expectedCashCents?.let(::formatEuroCents)
                                        ?: stringResource(R.string.not_available),
                                    shift.declaredCashCents?.let(::formatEuroCents)
                                        ?: stringResource(R.string.not_available),
                                    shift.cashVarianceCents?.let(::formatSignedEuroCents)
                                        ?: stringResource(R.string.not_available),
                                    localizedCashReconciliationStatus(shift.cashReconciliationStatus)
                                )
                            )
                            shift.fareTypeSummaries.forEach { fare ->
                                Text(
                                    stringResource(
                                        R.string.fare_summary,
                                        fare.fareName,
                                        fare.ticketCount,
                                        formatEuroCents(fare.cashTotalCents)
                                    )
                                )
                            }
                            if (shift.tickets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.ticket_records), fontWeight = FontWeight.Bold)
                                shift.tickets.forEach { ticket ->
                                    Text(
                                        stringResource(
                                            R.string.ticket_record,
                                            ticket.ticketId.takeLast(8),
                                            ticket.fareName,
                                            formatEuroCents(ticket.priceCents),
                                            stringResource(
                                                if (ticket.synced) R.string.synced else R.string.pending
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.refresh_report))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBackToDriverConsole,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(backLabel))
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
                    text = route?.name ?: stringResource(R.string.active_route),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(text = stringResource(R.string.current_stop), fontWeight = FontWeight.Bold)
                Text(
                    text = status.currentStop?.name ?: stringResource(R.string.unknown_stop),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = stringResource(
                        if (status.isRouteComplete) R.string.route_complete else R.string.next_stop
                    ),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status.nextStop?.name ?: stringResource(R.string.final_destination),
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                val currentNumber = (progress?.currentStopIndex ?: 0) + 1
                Text(
                    text = stringResource(
                        R.string.stop_progress,
                        currentNumber,
                        route?.stops?.size ?: 0
                    )
                )
                if (!state.isShiftActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.shift_ended), fontWeight = FontWeight.Bold)
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
                                text = stringResource(R.string.stop_requested),
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
                        Text(stringResource(R.string.request_stop_demo))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedButton(onClick = onBackToDriverConsole) {
                    Text(stringResource(R.string.back_driver_console))
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

private fun formatSignedEuroCents(cents: Int): String {
    val sign = when {
        cents > 0 -> "+"
        cents < 0 -> "−"
        else -> ""
    }
    return sign + formatEuroCents(kotlin.math.abs(cents))
}

private fun formatReportDateTime(timestampMillis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestampMillis))
}

@Composable
private fun formatDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

@Composable
private fun localizedDriverShiftStatus(state: DriverShiftUiState): String {
    val resource = when (driverShiftStatus(state)) {
        DriverShiftStatus.ACTIVE -> R.string.shift_status_active
        DriverShiftStatus.READY_TO_START -> R.string.shift_status_ready
        DriverShiftStatus.SIGN_IN_REQUIRED -> R.string.shift_status_sign_in
    }
    return stringResource(resource)
}

@Composable
private fun localizedDriverSyncStatus(state: DriverShiftUiState): String {
    return when (val status = driverSyncStatus(state)) {
        DriverSyncStatus.Synchronizing -> stringResource(R.string.sync_status_synchronizing)
        is DriverSyncStatus.Waiting -> pluralStringResource(
            R.plurals.sync_status_waiting,
            status.closedShiftCount,
            status.closedShiftCount
        )
        DriverSyncStatus.DemoReady -> stringResource(R.string.sync_status_demo_ready)
        DriverSyncStatus.LocalSynchronized -> stringResource(R.string.sync_status_local_complete)
        DriverSyncStatus.ProductionSynchronized -> stringResource(R.string.sync_status_complete)
    }
}

@Composable
private fun localizedReportingSyncStatus(status: ReportingSyncStatus): String {
    return stringResource(
        when (status) {
            ReportingSyncStatus.SYNCED -> R.string.report_sync_synced
            ReportingSyncStatus.PARTIALLY_SYNCED -> R.string.report_sync_partial
            ReportingSyncStatus.PENDING -> R.string.report_sync_pending
        }
    )
}

@Composable
private fun localizedCashReconciliationStatus(status: CashReconciliationStatus): String {
    return stringResource(
        when (status) {
            CashReconciliationStatus.NOT_RECORDED -> R.string.cash_status_not_recorded
            CashReconciliationStatus.MATCHED -> R.string.cash_status_matched
            CashReconciliationStatus.SHORTAGE -> R.string.cash_status_shortage
            CashReconciliationStatus.SURPLUS -> R.string.cash_status_surplus
        }
    )
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
    return if (PdfTicketPrinter.isPdfTestDevice(printer)) {
        printer.name
    } else {
        "${printer.name} (${printer.address})"
    }
}

private fun driverDutyText(duty: DriverDuty): String {
    val hours = duty.trip.departureMinutes / 60
    val minutes = duty.trip.departureMinutes % 60
    return "%s · %02d:%02d · %s · %s".format(
        duty.assignment.serviceDate,
        hours,
        minutes,
        duty.route.name,
        duty.bus.plateNumber
    )
}

private fun openTicketPdf(context: Context, path: String) {
    val file = File(path)
    if (!file.exists()) {
        Toast.makeText(context, context.getString(R.string.ticket_pdf_missing), Toast.LENGTH_LONG)
            .show()
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
            context.getString(R.string.install_pdf_viewer),
            Toast.LENGTH_LONG
        ).show()
    }
}

private const val BLUETOOTH_CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT"
private const val LANGUAGE_MENU_DISMISS_DELAY_MILLIS = 200L
