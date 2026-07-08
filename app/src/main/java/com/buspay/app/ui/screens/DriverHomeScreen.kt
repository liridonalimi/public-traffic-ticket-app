package com.buspay.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buspay.app.domain.Bus
import com.buspay.app.domain.Driver
import com.buspay.app.domain.Route

@Composable
fun DriverHomeScreen(viewModel: DriverShiftViewModel = viewModel()) {
    val state = viewModel.uiState

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
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

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Next stop", fontWeight = FontWeight.Bold)
                    Text(text = if (state.isShiftActive) state.nextStopName else "Start a shift to begin tracking")

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Current shift tickets", fontWeight = FontWeight.Bold)
                    Text(text = state.ticketCount.toString())

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Cash total", fontWeight = FontWeight.Bold)
                    Text(text = formatEuroCents(state.cashTotalCents))

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(text = "Total waiting for sync", fontWeight = FontWeight.Bold)
                    Text(text = "${state.pendingTicketCount} total tickets saved locally")

                    state.lastClosedSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "Last closed shift", fontWeight = FontWeight.Bold)
                        Text(text = "${summary.ticketCount} tickets / ${formatEuroCents(summary.cashTotalCents)}")
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
                        enabled = state.isShiftActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sell Ticket")
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
