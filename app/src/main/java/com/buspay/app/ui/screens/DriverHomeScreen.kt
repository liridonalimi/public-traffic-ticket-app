package com.buspay.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.buspay.app.device.PdfTicketPrinter
import com.buspay.app.device.PrinterDevice
import com.buspay.app.domain.Bus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.FareType
import com.buspay.app.domain.Route
import java.io.File

@Composable
fun DriverHomeScreen(viewModel: DriverShiftViewModel = viewModel()) {
    val state = viewModel.uiState
    val context = LocalContext.current
    var hasBluetoothPermission by remember {
        mutableStateOf(hasBluetoothPrinterPermission(context))
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetoothPermission = granted
        if (granted) viewModel.refreshPrinters()
    }

    LaunchedEffect(hasBluetoothPermission) {
        viewModel.refreshPrinters()
    }

    MaterialTheme {
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

                    Text(text = "Next stop", fontWeight = FontWeight.Bold)
                    Text(text = if (state.isShiftActive) state.nextStopName else "Start a shift to begin tracking")

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
                    Text(text = "${state.pendingTicketCount} total tickets saved locally")

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

private fun hasBluetoothPrinterPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(
            context,
            BLUETOOTH_CONNECT_PERMISSION
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
