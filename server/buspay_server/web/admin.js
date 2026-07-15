"use strict";

const elements = {
  authForm: document.querySelector("#auth-form"),
  authPanel: document.querySelector("#auth-panel"),
  authStatus: document.querySelector("#auth-status"),
  connectButton: document.querySelector("#connect-button"),
  connectionState: document.querySelector("#connection-state"),
  connectionLabel: document.querySelector("#connection-label"),
  dashboard: document.querySelector("#dashboard"),
  dashboardStatus: document.querySelector("#dashboard-status"),
  driverFilter: document.querySelector("#driver-filter"),
  driverTableBody: document.querySelector("#driver-table-body"),
  emptyState: document.querySelector("#empty-state"),
  fareFilter: document.querySelector("#fare-filter"),
  fareList: document.querySelector("#fare-list"),
  generatedTime: document.querySelector("#generated-time"),
  metricDrivers: document.querySelector("#metric-drivers"),
  metricRevenue: document.querySelector("#metric-revenue"),
  metricShifts: document.querySelector("#metric-shifts"),
  metricTickets: document.querySelector("#metric-tickets"),
  refreshButton: document.querySelector("#refresh-button"),
  shiftList: document.querySelector("#shift-list"),
  shiftResultCount: document.querySelector("#shift-result-count"),
  shiftSearch: document.querySelector("#shift-search"),
  signOutButton: document.querySelector("#sign-out-button"),
  tokenInput: document.querySelector("#token-input"),
};

let bearerToken = "";
let currentReport = null;

const money = new Intl.NumberFormat("en", { style: "currency", currency: "EUR" });
const timestamp = new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" });

function formatMoney(cents) {
  return money.format((Number(cents) || 0) / 100);
}

function formatTime(millis) {
  return timestamp.format(new Date(Number(millis)));
}

function formatDuration(millis) {
  const minutes = Math.max(0, Math.round((Number(millis) || 0) / 60000));
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return hours ? `${hours}h ${remainder}m` : `${remainder}m`;
}

function labelFare(value) {
  return String(value || "unknown").replaceAll("_", " ");
}

function setConnection(state, label) {
  elements.connectionState.dataset.state = state;
  elements.connectionLabel.textContent = label;
}

function option(value, label) {
  const node = document.createElement("option");
  node.value = value;
  node.textContent = label;
  return node;
}

function replaceFilterOptions(select, values, firstLabel, formatter = (value) => value) {
  const selected = select.value;
  select.replaceChildren(option("", firstLabel));
  values.forEach((value) => select.append(option(value, formatter(value))));
  if (values.includes(selected)) select.value = selected;
}

function renderMetrics(report) {
  const overall = report.overall || {};
  elements.metricRevenue.textContent = formatMoney(overall.cashTotalCents);
  elements.metricTickets.textContent = Number(overall.ticketCount) || 0;
  elements.metricShifts.textContent = Number(overall.shiftCount) || 0;
  elements.metricDrivers.textContent = Number(overall.driverCount) || 0;
}

function renderFares(report) {
  const fares = report.fares || [];
  const maximum = Math.max(1, ...fares.map((fare) => Number(fare.ticketCount) || 0));
  elements.fareList.replaceChildren();
  if (!fares.length) {
    const empty = document.createElement("p");
    empty.textContent = "No synchronized fare sales yet.";
    elements.fareList.append(empty);
    return;
  }
  fares.forEach((fare) => {
    const row = document.createElement("div");
    row.className = "fare-row";
    const name = document.createElement("strong");
    name.textContent = labelFare(fare.fareTypeId);
    const total = document.createElement("span");
    total.textContent = `${fare.ticketCount} tickets · ${formatMoney(fare.cashTotalCents)}`;
    const progress = document.createElement("progress");
    progress.max = maximum;
    progress.value = Number(fare.ticketCount) || 0;
    progress.setAttribute("aria-label", `${name.textContent} ticket share`);
    row.append(name, total, progress);
    elements.fareList.append(row);
  });
}

function renderDrivers(report) {
  elements.driverTableBody.replaceChildren();
  (report.drivers || []).forEach((driver) => {
    const row = document.createElement("tr");
    [driver.driverId, driver.shiftCount, driver.ticketCount, formatMoney(driver.cashTotalCents)].forEach((value) => {
      const cell = document.createElement("td");
      cell.textContent = value;
      row.append(cell);
    });
    elements.driverTableBody.append(row);
  });
}

function ticketItem(ticket) {
  const item = document.createElement("div");
  item.className = "ticket-item";
  const heading = document.createElement("strong");
  heading.textContent = `${labelFare(ticket.fareTypeId)} · ${formatMoney(ticket.priceCents)}`;
  const metadata = document.createElement("span");
  metadata.textContent = `${ticket.ticketId} · ${formatTime(ticket.soldAtMillis)}`;
  item.append(heading, metadata);
  return item;
}

function shiftStat(label, value, optional = false) {
  const block = document.createElement("div");
  block.className = `shift-stat${optional ? " optional-stat" : ""}`;
  const strong = document.createElement("strong");
  strong.textContent = value;
  const span = document.createElement("span");
  span.textContent = label;
  block.append(strong, span);
  return block;
}

function shiftCard(shift) {
  const details = document.createElement("details");
  details.className = "shift-card";
  const summary = document.createElement("summary");
  const primary = document.createElement("div");
  primary.className = "shift-primary";
  const driver = document.createElement("strong");
  driver.textContent = shift.driverId;
  const identity = document.createElement("span");
  identity.textContent = shift.shiftId;
  primary.append(driver, identity);
  const status = shiftStat("Status", shift.syncStatus, true);
  status.querySelector("strong").className = "status-pill";
  summary.append(
    primary,
    shiftStat("Started", formatTime(shift.startedAtMillis)),
    shiftStat("Bus · route", `${shift.busId} · ${shift.routeId}`, true),
    shiftStat("Tickets", String(shift.ticketCount)),
    shiftStat("Revenue", formatMoney(shift.cashTotalCents)),
    document.createElement("span")
  );

  const body = document.createElement("div");
  body.className = "ticket-detail";
  const heading = document.createElement("h3");
  heading.textContent = `Ticket records · ${formatDuration(shift.durationMillis)}`;
  const grid = document.createElement("div");
  grid.className = "ticket-grid";
  (shift.tickets || []).forEach((ticket) => grid.append(ticketItem(ticket)));
  if (!shift.tickets?.length) {
    const empty = document.createElement("span");
    empty.textContent = "No tickets recorded for this shift.";
    grid.append(empty);
  }
  body.append(heading, grid);
  details.append(summary, body);
  return details;
}

function renderShifts() {
  if (!currentReport) return;
  const driver = elements.driverFilter.value;
  const fare = elements.fareFilter.value;
  const search = elements.shiftSearch.value.trim().toLowerCase();
  const shifts = (currentReport.shifts || []).filter((shift) => {
    const matchesDriver = !driver || shift.driverId === driver;
    const matchesFare = !fare || (shift.tickets || []).some((ticket) => ticket.fareTypeId === fare);
    const searchable = [shift.shiftId, shift.driverId, shift.busId, shift.routeId].join(" ").toLowerCase();
    return matchesDriver && matchesFare && (!search || searchable.includes(search));
  });
  elements.shiftList.replaceChildren(...shifts.map(shiftCard));
  elements.shiftResultCount.textContent = `${shifts.length} of ${(currentReport.shifts || []).length} shifts`;
  elements.emptyState.hidden = shifts.length !== 0;
}

function renderReport(report) {
  currentReport = report;
  renderMetrics(report);
  renderFares(report);
  renderDrivers(report);
  const drivers = [...new Set((report.shifts || []).map((shift) => shift.driverId))].sort();
  const fares = [...new Set((report.shifts || []).flatMap((shift) => (shift.tickets || []).map((ticket) => ticket.fareTypeId)))].sort();
  replaceFilterOptions(elements.driverFilter, drivers, "All drivers");
  replaceFilterOptions(elements.fareFilter, fares, "All fares", labelFare);
  renderShifts();
  elements.generatedTime.textContent = `Report generated ${formatTime(report.generatedAtMillis)}`;
}

async function loadReport() {
  if (!bearerToken) return;
  elements.refreshButton.disabled = true;
  elements.dashboardStatus.textContent = "Loading the latest synchronized report…";
  try {
    const response = await fetch("/v1/reports/admin", {
      method: "GET",
      cache: "no-store",
      credentials: "omit",
      headers: { Authorization: `Bearer ${bearerToken}` },
    });
    if (response.status === 401) throw new Error("The access token was rejected.");
    if (!response.ok) throw new Error(`The reporting service returned HTTP ${response.status}.`);
    const report = await response.json();
    if (report.contractVersion !== 1) throw new Error("Unsupported reporting contract version.");
    renderReport(report);
    elements.tokenInput.value = "";
    elements.authPanel.hidden = true;
    elements.dashboard.hidden = false;
    elements.dashboardStatus.textContent = "Report reconciled successfully.";
    setConnection("live", "Live report");
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unable to load the report.";
    elements.authStatus.textContent = message;
    elements.dashboardStatus.textContent = message;
    setConnection("error", "Connection failed");
    if (message.includes("token")) signOut(false);
  } finally {
    elements.refreshButton.disabled = false;
    elements.connectButton.disabled = false;
  }
}

function signOut(focus = true) {
  bearerToken = "";
  currentReport = null;
  elements.tokenInput.value = "";
  elements.dashboard.hidden = true;
  elements.authPanel.hidden = false;
  elements.generatedTime.textContent = "Connect to load the latest report.";
  setConnection("locked", "Locked");
  if (focus) elements.tokenInput.focus();
}

elements.authForm.addEventListener("submit", (event) => {
  event.preventDefault();
  elements.authStatus.textContent = "";
  bearerToken = elements.tokenInput.value.trim();
  if (!bearerToken) return;
  elements.connectButton.disabled = true;
  loadReport();
});
elements.refreshButton.addEventListener("click", loadReport);
elements.signOutButton.addEventListener("click", () => signOut());
elements.driverFilter.addEventListener("change", renderShifts);
elements.fareFilter.addEventListener("change", renderShifts);
elements.shiftSearch.addEventListener("input", renderShifts);
window.addEventListener("pageshow", (event) => {
  if (event.persisted) signOut(false);
});
