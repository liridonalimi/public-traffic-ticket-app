"use strict";

const elements = {
  authForm: document.querySelector("#auth-form"),
  authPanel: document.querySelector("#auth-panel"),
  authStatus: document.querySelector("#auth-status"),
  auditStatus: document.querySelector("#audit-status"),
  auditTableBody: document.querySelector("#audit-table-body"),
  catalogSummary: document.querySelector("#catalog-summary"),
  catalogStatus: document.querySelector("#catalog-status"),
  catalogDrivers: document.querySelector("#catalog-drivers"),
  catalogBuses: document.querySelector("#catalog-buses"),
  catalogRoutes: document.querySelector("#catalog-routes"),
  catalogStops: document.querySelector("#catalog-stops"),
  catalogFares: document.querySelector("#catalog-fares"),
  catalogServiceCalendars: document.querySelector("#catalog-service-calendars"),
  catalogScheduledTrips: document.querySelector("#catalog-scheduled-trips"),
  catalogTripAssignments: document.querySelector("#catalog-trip-assignments"),
  reloadCatalogButton: document.querySelector("#reload-catalog-button"),
  saveCatalogButton: document.querySelector("#save-catalog-button"),
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
  heroCopy: document.querySelector("#hero-copy"),
  heroEyebrow: document.querySelector("#hero-eyebrow"),
  metricDrivers: document.querySelector("#metric-drivers"),
  metricExpectedCash: document.querySelector("#metric-expected-cash"),
  metricDeclaredCash: document.querySelector("#metric-declared-cash"),
  metricCashVariance: document.querySelector("#metric-cash-variance"),
  metricReconciledShifts: document.querySelector("#metric-reconciled-shifts"),
  metricRevenue: document.querySelector("#metric-revenue"),
  metricShifts: document.querySelector("#metric-shifts"),
  metricTickets: document.querySelector("#metric-tickets"),
  metricTicketActions: document.querySelector("#metric-ticket-actions"),
  pageTitle: document.querySelector("#page-title"),
  refreshButton: document.querySelector("#refresh-button"),
  refreshAuditButton: document.querySelector("#refresh-audit-button"),
  shiftList: document.querySelector("#shift-list"),
  shiftResultCount: document.querySelector("#shift-result-count"),
  shiftSearch: document.querySelector("#shift-search"),
  signOutButton: document.querySelector("#sign-out-button"),
  tokenInput: document.querySelector("#token-input"),
  workspaceEyebrow: document.querySelector("#workspace-eyebrow"),
  workspaceTitle: document.querySelector("#workspace-title"),
};

let bearerToken = "";
let currentReport = null;
let currentCatalog = null;
let currentAudit = null;
let currentRoles = [];

const money = new Intl.NumberFormat("en", { style: "currency", currency: "EUR" });
const timestamp = new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" });
const weekdayNames = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

function formatMoney(cents) {
  return money.format((Number(cents) || 0) / 100);
}

function formatSignedMoney(cents) {
  const value = Number(cents) || 0;
  return `${value > 0 ? "+" : value < 0 ? "−" : ""}${formatMoney(Math.abs(value))}`;
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
  elements.metricExpectedCash.textContent = formatMoney(overall.expectedCashTotalCents);
  elements.metricDeclaredCash.textContent = formatMoney(overall.declaredCashTotalCents);
  elements.metricCashVariance.textContent = formatSignedMoney(overall.cashVarianceTotalCents);
  elements.metricReconciledShifts.textContent = `${Number(overall.reconciledShiftCount) || 0} / ${Number(overall.shiftCount) || 0}`;
  elements.metricTicketActions.textContent = `${Number(overall.voidCount) || 0} / ${Number(overall.correctionCount) || 0} / ${Number(overall.reprintCount) || 0}`;
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
  if (ticket.farePolicyRevision) {
    const policy = document.createElement("span");
    const transfer = ticket.transferValidUntilMillis
      ? ` · transfer until ${formatTime(ticket.transferValidUntilMillis)}`
      : "";
    policy.textContent = `Policy revision ${ticket.farePolicyRevision} · ${ticket.originStopId} → ${ticket.destinationStopId} · ${ticket.zoneCount} zone${ticket.zoneCount === 1 ? "" : "s"}${ticket.offPeakApplied ? " · off-peak" : ""}${transfer}`;
    item.append(policy);
  }
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
  const reconciliationStatus = labelFare(shift.cashReconciliationStatus || "NOT_RECORDED");
  summary.append(
    primary,
    shiftStat("Started", formatTime(shift.startedAtMillis)),
    shiftStat("Bus · route", `${shift.busId} · ${shift.routeId}`, true),
    shiftStat("Tickets", String(shift.ticketCount)),
    shiftStat("Revenue", formatMoney(shift.cashTotalCents)),
    shiftStat("Cash", reconciliationStatus, true)
  );

  const body = document.createElement("div");
  body.className = "ticket-detail";
  const heading = document.createElement("h3");
  heading.textContent = `Ticket records · ${formatDuration(shift.durationMillis)}`;
  const handover = document.createElement("p");
  handover.textContent = shift.cashReconciliationStatus === "NOT_RECORDED"
    ? "Cash handover was not recorded for this legacy shift."
    : `Cash handover · expected ${formatMoney(shift.expectedCashCents)} · declared ${formatMoney(shift.declaredCashCents)} · variance ${formatSignedMoney(shift.cashVarianceCents)}`;
  const schedule = document.createElement("p");
  schedule.textContent = shift.assignmentId
    ? `Scheduled operation · ${shift.assignmentId} · ${shift.scheduledTripId}`
    : "Ad-hoc pilot operation";
  const revenue = document.createElement("p");
  revenue.textContent = shift.grossCashTotalCents !== shift.cashTotalCents
    ? `Revenue adjustment · gross ${formatMoney(shift.grossCashTotalCents)} · effective ${formatMoney(shift.cashTotalCents)}`
    : `Revenue · ${formatMoney(shift.cashTotalCents)}`;
  const grid = document.createElement("div");
  grid.className = "ticket-grid";
  (shift.tickets || []).forEach((ticket) => grid.append(ticketItem(ticket)));
  if (!shift.tickets?.length) {
    const empty = document.createElement("span");
    empty.textContent = "No tickets recorded for this shift.";
    grid.append(empty);
  }
  const actionHeading = document.createElement("h3");
  actionHeading.textContent = "Ticket action history";
  const actionGrid = document.createElement("div");
  actionGrid.className = "ticket-grid";
  (shift.ticketActions || []).forEach((action) => {
    const item = document.createElement("div");
    item.className = "ticket-item";
    const title = document.createElement("strong");
    title.textContent = `${labelFare(action.actionType)} · ${action.originalTicketId}`;
    const evidence = document.createElement("span");
    evidence.textContent = `${labelFare(action.reason)} · supervisor ${action.supervisorId} · ${formatTime(action.authorizedAtMillis)}`;
    item.append(title, evidence);
    actionGrid.append(item);
  });
  if (!shift.ticketActions?.length) {
    const empty = document.createElement("span");
    empty.textContent = "No post-sale actions recorded.";
    actionGrid.append(empty);
  }
  body.append(heading, schedule, handover, revenue, grid, actionHeading, actionGrid);
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
    const searchable = [shift.shiftId, shift.driverId, shift.busId, shift.routeId, shift.assignmentId, shift.scheduledTripId].join(" ").toLowerCase();
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

function renderAudit(audit) {
  currentAudit = audit;
  elements.auditTableBody.replaceChildren();
  (audit.events || []).forEach((event) => {
    const row = document.createElement("tr");
    const values = [
      formatTime(event.occurredAtMillis),
      event.outcome,
      `${event.method} ${event.path}`,
      (event.roles || []).join(", ") || "none",
      event.source,
    ];
    values.forEach((value, index) => {
      const cell = document.createElement("td");
      cell.textContent = value;
      if (index === 1) cell.className = `audit-outcome-${event.outcome}`;
      row.append(cell);
    });
    elements.auditTableBody.append(row);
  });
  elements.auditStatus.textContent = `${(audit.events || []).length} authorization events loaded.`;
}

async function loadAudit() {
  if (!bearerToken) return;
  elements.refreshAuditButton.disabled = true;
  elements.auditStatus.textContent = "Loading authorization events…";
  try {
    const response = await fetch("/v1/audit?limit=100", {
      method: "GET",
      cache: "no-store",
      credentials: "omit",
      headers: { Authorization: `Bearer ${bearerToken}` },
    });
    if (response.status === 401) throw new Error("The access token was rejected.");
    if (response.status === 403) throw new Error("This token cannot read the authorization audit.");
    if (!response.ok) throw new Error(`The audit service returned HTTP ${response.status}.`);
    const audit = await response.json();
    if (audit.contractVersion !== 1) throw new Error("Unsupported audit contract version.");
    renderAudit(audit);
  } catch (error) {
    elements.auditStatus.textContent = error instanceof Error ? error.message : "Unable to load the authorization audit.";
  } finally {
    elements.refreshAuditButton.disabled = false;
  }
}

function catalogRow(entity, record, primary, secondary) {
  const row = document.createElement("div");
  row.className = "catalog-row";
  const copy = document.createElement("div");
  const strong = document.createElement("strong");
  strong.textContent = primary;
  const span = document.createElement("span");
  span.textContent = secondary;
  copy.append(strong, span);
  const remove = document.createElement("button");
  remove.type = "button";
  remove.className = "catalog-remove";
  remove.textContent = "Remove";
  remove.addEventListener("click", () => removeCatalogRecord(entity, record.id));
  row.append(copy, remove);
  return row;
}

function renderCatalog() {
  if (!currentCatalog) return;
  currentCatalog.serviceCalendars ||= [];
  currentCatalog.scheduledTrips ||= [];
  currentCatalog.tripAssignments ||= [];
  const counts = [
    `${currentCatalog.drivers.length} drivers`,
    `${currentCatalog.buses.length} buses`,
    `${currentCatalog.routes.length} routes`,
    `${currentCatalog.stops.length} stops`,
    `${currentCatalog.fares.length} fares`,
    `${currentCatalog.serviceCalendars.length} calendars`,
    `${currentCatalog.scheduledTrips.length} trips`,
    `${currentCatalog.tripAssignments.length} assignments`,
  ];
  elements.catalogSummary.textContent = `Revision ${currentCatalog.revision} · ${counts.join(" · ")}`;
  elements.catalogDrivers.replaceChildren(...currentCatalog.drivers.map((record) =>
    catalogRow("drivers", record, record.name, record.id)
  ));
  elements.catalogBuses.replaceChildren(...currentCatalog.buses.map((record) =>
    catalogRow("buses", record, record.plateNumber, record.id)
  ));
  elements.catalogRoutes.replaceChildren(...currentCatalog.routes.map((record) =>
    catalogRow("routes", record, record.name, record.id)
  ));
  elements.catalogStops.replaceChildren(...[...currentCatalog.stops]
    .sort((left, right) => left.routeId.localeCompare(right.routeId) || left.order - right.order)
    .map((record) => catalogRow(
      "stops",
      record,
      `${record.order}. ${record.name}`,
      `${record.routeId} · zone ${record.zoneId || "1"} · ${record.latitude}, ${record.longitude}`
    )));
  elements.catalogFares.replaceChildren(...currentCatalog.fares.map((record) =>
    catalogRow(
      "fares",
      record,
      `${record.name} · ${formatMoney(record.priceCents)}`,
      [record.id, record.eligibility,
        record.routeId ? routeName(record.routeId) : "all routes",
        record.additionalZoneCents ? `${formatMoney(record.additionalZoneCents)} / extra zone` : null,
        record.offPeakDiscountCents ? `${formatMoney(record.offPeakDiscountCents)} off-peak (${minutesLabel(record.offPeakStartMinutes)}–${minutesLabel(record.offPeakEndMinutes)})` : null,
        record.transferWindowMinutes ? `${record.transferWindowMinutes} min transfer` : null,
      ].filter(Boolean).join(" · ")
    )
  ));
  elements.catalogServiceCalendars.replaceChildren(...currentCatalog.serviceCalendars.map((record) =>
    catalogRow("serviceCalendars", record, record.name, `${record.startDate}–${record.endDate} · ${weekdayListLabel(record.activeWeekdays)}`)
  ));
  elements.catalogScheduledTrips.replaceChildren(...currentCatalog.scheduledTrips.map((record) =>
    catalogRow(
      "scheduledTrips",
      record,
      scheduledTripLabel(record, false),
      `${record.id} · ${calendarName(record.serviceCalendarId)}`
    )
  ));
  elements.catalogTripAssignments.replaceChildren(...currentCatalog.tripAssignments.map((record) =>
    catalogRow(
      "tripAssignments",
      record,
      `${record.serviceDate} · ${scheduledTripLabel(currentCatalog.scheduledTrips.find((trip) => trip.id === record.tripId), false)}`,
      `${driverName(record.driverId)} · ${busName(record.busId)} · ${record.id}`
    )
  ));

  document.querySelectorAll('form[data-entity="stops"] select[name="routeId"]').forEach((select) => {
    const selected = select.value;
    select.replaceChildren(option("", "Select route"));
    currentCatalog.routes.forEach((route) => select.append(option(route.id, route.name)));
    if (currentCatalog.routes.some((route) => route.id === selected)) select.value = selected;
  });
  replaceCatalogSelect('form[data-entity="fares"] select[name="routeId"]', currentCatalog.routes, "All routes", (record) => record.name);
  replaceCatalogSelect('form[data-entity="scheduledTrips"] select[name="routeId"]', currentCatalog.routes, "Select route", (record) => record.name);
  replaceCatalogSelect('form[data-entity="scheduledTrips"] select[name="serviceCalendarId"]', currentCatalog.serviceCalendars, "Select calendar", (record) => record.name);
  replaceCatalogSelect('form[data-entity="tripAssignments"] select[name="tripId"]', currentCatalog.scheduledTrips, "Select trip", (record) => scheduledTripLabel(record, true));
  replaceCatalogSelect('form[data-entity="tripAssignments"] select[name="driverId"]', currentCatalog.drivers, "Select driver", (record) => record.name);
  replaceCatalogSelect('form[data-entity="tripAssignments"] select[name="busId"]', currentCatalog.buses, "Select bus", (record) => record.plateNumber);
  renderScheduledStopTimeFields();
}

function minutesLabel(value) {
  const minutes = Number(value) || 0;
  const clockMinutes = ((minutes % 1440) + 1440) % 1440;
  const clock = `${String(Math.floor(clockMinutes / 60)).padStart(2, "0")}:${String(clockMinutes % 60).padStart(2, "0")}`;
  return minutes >= 1440 ? `${clock} (+1 day)` : clock;
}

function minutesToTimeValue(value) {
  const minutes = ((Number(value) % 1440) + 1440) % 1440;
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
}

function timeValueToMinutes(value) {
  const match = /^(\d{2}):(\d{2})$/.exec(String(value || ""));
  if (!match) throw new Error("Choose a valid time.");
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) throw new Error("Choose a valid time.");
  return (hours * 60) + minutes;
}

function weekdayListLabel(values) {
  const days = [...new Set((values || []).map(Number))].sort((left, right) => left - right);
  if (days.join(",") === "1,2,3,4,5") return "Monday–Friday";
  if (days.join(",") === "6,7") return "Saturday–Sunday";
  return days.map((day) => weekdayNames[day - 1] || `Day ${day}`).join(", ");
}

function routeName(id) {
  return currentCatalog?.routes.find((record) => record.id === id)?.name || id || "Unknown route";
}

function calendarName(id) {
  return currentCatalog?.serviceCalendars.find((record) => record.id === id)?.name || id || "Unknown calendar";
}

function driverName(id) {
  return currentCatalog?.drivers.find((record) => record.id === id)?.name || id || "Unknown driver";
}

function busName(id) {
  return currentCatalog?.buses.find((record) => record.id === id)?.plateNumber || id || "Unknown bus";
}

function scheduledTripLabel(record, includeId) {
  if (!record) return "Unknown trip";
  const finalStop = (record.stopTimes || []).at(-1);
  const endMinutes = finalStop?.departureMinutes ?? finalStop?.arrivalMinutes;
  const timeRange = Number.isInteger(Number(endMinutes))
    ? `${minutesLabel(record.departureMinutes)}–${minutesLabel(endMinutes)}`
    : minutesLabel(record.departureMinutes);
  const direction = String(record.direction || "").toLowerCase();
  const base = `${timeRange} · ${routeName(record.routeId)} · ${direction}`;
  return includeId ? `${base} · ${record.id}` : base;
}

function renderScheduledStopTimeFields() {
  const form = document.querySelector('form[data-entity="scheduledTrips"]');
  if (!(form instanceof HTMLFormElement) || !currentCatalog) return;
  const routeSelect = form.querySelector('select[name="routeId"]');
  const departureInput = form.querySelector('input[name="departureTime"]');
  const fieldContainer = form.querySelector("[data-stop-time-fields]");
  const hint = form.querySelector(".stop-time-hint");
  if (!(routeSelect instanceof HTMLSelectElement) || !(fieldContainer instanceof HTMLElement)) return;

  const routeStops = [...currentCatalog.stops]
    .filter((stop) => stop.routeId === routeSelect.value)
    .sort((left, right) => left.order - right.order);
  fieldContainer.replaceChildren();
  if (!routeStops.length) {
    if (hint) hint.textContent = routeSelect.value
      ? "This route has no stops yet. Add its stops before scheduling a trip."
      : "Choose a route to create one normal time field for every stop.";
    return;
  }

  let departure = 480;
  try {
    departure = timeValueToMinutes(departureInput?.value || "08:00");
  } catch (_) {
    departure = 480;
  }
  if (departureInput instanceof HTMLInputElement && !departureInput.value) {
    departureInput.value = minutesToTimeValue(departure);
  }
  if (hint) hint.textContent = `${routeName(routeSelect.value)} has ${routeStops.length} stops. Confirm each time in route order.`;

  routeStops.forEach((stop, index) => {
    const minutes = departure + (index * 10);
    const field = document.createElement("div");
    field.className = "stop-time-field";
    const timeLabel = document.createElement("label");
    const title = document.createElement("span");
    title.textContent = `${index + 1}. ${stop.name}`;
    const time = document.createElement("input");
    time.type = "time";
    time.step = "60";
    time.required = true;
    time.value = minutesToTimeValue(minutes);
    time.dataset.stopTime = "true";
    time.dataset.stopId = stop.id;
    timeLabel.append(title, time);

    const nextDay = document.createElement("label");
    nextDay.className = "next-day-toggle";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.dataset.nextDay = "true";
    checkbox.checked = minutes >= 1440;
    const nextDayText = document.createElement("span");
    nextDayText.textContent = "Next day";
    nextDay.append(checkbox, nextDayText);
    field.append(timeLabel, nextDay);
    fieldContainer.append(field);
  });
}

function replaceCatalogSelect(selector, records, firstLabel, formatter) {
  document.querySelectorAll(selector).forEach((select) => {
    const selected = select.value;
    select.replaceChildren(option("", firstLabel));
    records.forEach((record) => select.append(option(record.id, formatter(record))));
    if (records.some((record) => record.id === selected)) select.value = selected;
  });
}

function removeCatalogRecord(entity, id) {
  if (!currentCatalog) return;
  currentCatalog[entity] = currentCatalog[entity].filter((record) => record.id !== id);
  if (entity === "routes") {
    currentCatalog.stops = currentCatalog.stops.filter((stop) => stop.routeId !== id);
    const tripIds = currentCatalog.scheduledTrips.filter((trip) => trip.routeId === id).map((trip) => trip.id);
    currentCatalog.scheduledTrips = currentCatalog.scheduledTrips.filter((trip) => trip.routeId !== id);
    currentCatalog.tripAssignments = currentCatalog.tripAssignments.filter((assignment) => !tripIds.includes(assignment.tripId));
  }
  if (entity === "drivers") currentCatalog.tripAssignments = currentCatalog.tripAssignments.filter((record) => record.driverId !== id);
  if (entity === "buses") currentCatalog.tripAssignments = currentCatalog.tripAssignments.filter((record) => record.busId !== id);
  if (entity === "serviceCalendars") {
    const tripIds = currentCatalog.scheduledTrips.filter((trip) => trip.serviceCalendarId === id).map((trip) => trip.id);
    currentCatalog.scheduledTrips = currentCatalog.scheduledTrips.filter((trip) => trip.serviceCalendarId !== id);
    currentCatalog.tripAssignments = currentCatalog.tripAssignments.filter((assignment) => !tripIds.includes(assignment.tripId));
  }
  if (entity === "scheduledTrips") currentCatalog.tripAssignments = currentCatalog.tripAssignments.filter((record) => record.tripId !== id);
  elements.catalogStatus.textContent = "Draft changed. Publish to make it available to tablets.";
  renderCatalog();
}

function catalogRecordFromForm(entity, form) {
  const formData = new FormData(form);
  const values = Object.fromEntries(formData.entries());
  const trimmed = Object.fromEntries(
    Object.entries(values).map(([key, value]) => [key, String(value).trim()])
  );
  if (entity === "stops") {
    return {
      ...trimmed,
      latitude: Number(trimmed.latitude),
      longitude: Number(trimmed.longitude),
      order: Number(trimmed.order),
    };
  }
  if (entity === "fares") {
    const startTime = trimmed.offPeakStartTime;
    const endTime = trimmed.offPeakEndTime;
    if (Boolean(startTime) !== Boolean(endTime)) throw new Error("Choose both off-peak start and end times, or leave both empty.");
    if (startTime && startTime === endTime) throw new Error("Off-peak start and end times must be different.");
    return {
      id: trimmed.id,
      name: trimmed.name,
      priceCents: Number(trimmed.priceCents),
      eligibility: trimmed.eligibility || null,
      routeId: trimmed.routeId || null,
      additionalZoneCents: Number(trimmed.additionalZoneCents) || 0,
      offPeakDiscountCents: Number(trimmed.offPeakDiscountCents) || 0,
      offPeakStartMinutes: startTime ? timeValueToMinutes(startTime) : null,
      offPeakEndMinutes: endTime ? timeValueToMinutes(endTime) : null,
      transferWindowMinutes: Number(trimmed.transferWindowMinutes) || 0,
    };
  }
  if (entity === "serviceCalendars") {
    const activeWeekdays = formData.getAll("activeWeekdays").map((value) => Number(value));
    if (!activeWeekdays.length) throw new Error("Select at least one service day.");
    return {
      id: trimmed.id,
      name: trimmed.name,
      startDate: trimmed.startDate,
      endDate: trimmed.endDate,
      activeWeekdays,
    };
  }
  if (entity === "scheduledTrips") {
    const routeStops = [...currentCatalog.stops]
      .filter((stop) => stop.routeId === trimmed.routeId)
      .sort((left, right) => left.order - right.order);
    const departureMinutes = timeValueToMinutes(trimmed.departureTime);
    const timeInputs = [...form.querySelectorAll("input[data-stop-time]")];
    if (timeInputs.length !== routeStops.length) throw new Error("Choose the route again so its stop-time fields can be created.");
    const minutes = timeInputs.map((input) => {
      const nextDay = input.closest(".stop-time-field")?.querySelector("input[data-next-day]")?.checked;
      return timeValueToMinutes(input.value) + (nextDay ? 1440 : 0);
    });
    if (minutes[0] < departureMinutes) throw new Error("The first stop time cannot be earlier than the trip departure time.");
    if (minutes.some((value, index) => index > 0 && value < minutes[index - 1])) {
      throw new Error("Stop times must stay in chronological route order.");
    }
    return {
      id: trimmed.id,
      routeId: trimmed.routeId,
      serviceCalendarId: trimmed.serviceCalendarId,
      departureMinutes,
      direction: trimmed.direction,
      stopTimes: routeStops.map((stop, index) => ({
        stopId: stop.id,
        arrivalMinutes: minutes[index],
        departureMinutes: minutes[index],
      })),
    };
  }
  return trimmed;
}

function setCatalogFormStatus(form, message, isError = false) {
  let status = form.nextElementSibling;
  if (!(status instanceof HTMLElement) || !status.classList.contains("catalog-form-status")) {
    status = document.createElement("p");
    status.className = "catalog-form-status";
    status.setAttribute("role", "status");
    status.setAttribute("aria-live", "polite");
    form.insertAdjacentElement("afterend", status);
  }
  status.textContent = message;
  status.classList.toggle("is-error", isError);
}

function updateCatalogDraft(entity, record) {
  if (!currentCatalog) return;
  const existingIndex = currentCatalog[entity].findIndex((value) => value.id === record.id);
  if (existingIndex === -1) currentCatalog[entity].push(record);
  else currentCatalog[entity][existingIndex] = record;
  elements.catalogStatus.textContent = "Draft changed. Publish to make it available to tablets.";
  renderCatalog();
}

async function loadCatalog() {
  if (!bearerToken) return;
  elements.reloadCatalogButton.disabled = true;
  elements.catalogStatus.textContent = "Loading the published catalog…";
  try {
    const response = await fetch("/v1/catalog", {
      method: "GET",
      cache: "no-store",
      credentials: "omit",
      headers: { Authorization: `Bearer ${bearerToken}` },
    });
    if (response.status === 401) throw new Error("The access token was rejected.");
    if (!response.ok) throw new Error(`The catalog service returned HTTP ${response.status}.`);
    const catalog = await response.json();
    if (catalog.contractVersion !== 1) throw new Error("Unsupported catalog contract version.");
    currentCatalog = catalog;
    renderCatalog();
    elements.catalogStatus.textContent = "Published catalog loaded.";
  } finally {
    elements.reloadCatalogButton.disabled = false;
  }
}

async function saveCatalog() {
  if (!bearerToken || !currentCatalog) return;
  elements.saveCatalogButton.disabled = true;
  elements.catalogStatus.textContent = "Publishing the complete catalog…";
  try {
    const response = await fetch("/v1/catalog", {
      method: "PUT",
      cache: "no-store",
      credentials: "omit",
      headers: {
        Authorization: `Bearer ${bearerToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        contractVersion: 1,
        expectedRevision: currentCatalog.revision,
        drivers: currentCatalog.drivers,
        buses: currentCatalog.buses,
        routes: currentCatalog.routes,
        stops: currentCatalog.stops,
        fares: currentCatalog.fares,
        serviceCalendars: currentCatalog.serviceCalendars,
        scheduledTrips: currentCatalog.scheduledTrips,
        tripAssignments: currentCatalog.tripAssignments,
      }),
    });
    const payload = await response.json().catch(() => ({}));
    if (response.status === 401) throw new Error("The access token was rejected.");
    if (!response.ok) throw new Error(payload.error || `Catalog publication returned HTTP ${response.status}.`);
    currentCatalog = payload;
    renderCatalog();
    elements.catalogStatus.textContent = `Catalog revision ${payload.revision} published. Tablets can refresh now.`;
  } catch (error) {
    elements.catalogStatus.textContent = error instanceof Error ? error.message : "Unable to publish the catalog.";
  } finally {
    elements.saveCatalogButton.disabled = false;
  }
}

async function loadReport() {
  if (!bearerToken) return;
  elements.refreshButton.disabled = true;
  elements.dashboardStatus.textContent = "Loading the latest synchronized report…";
  try {
    const accessResponse = await fetch("/v1/access", {
      method: "GET",
      cache: "no-store",
      credentials: "omit",
      headers: { Authorization: `Bearer ${bearerToken}` },
    });
    if (accessResponse.status === 401) throw new Error("The access token was rejected.");
    if (!accessResponse.ok) throw new Error(`Access verification returned HTTP ${accessResponse.status}.`);
    const access = await accessResponse.json();
    currentRoles = Array.isArray(access.roles) ? access.roles : [];
    const canReport = currentRoles.includes("report_read");
    const canManageCatalog = currentRoles.includes("catalog_write");
    const canReadAudit = currentRoles.includes("audit_read");
    if (!canReport && !canManageCatalog && !canReadAudit) {
      throw new Error("This token does not grant access to an administrative workspace.");
    }
    document.querySelectorAll("[data-requires]").forEach((node) => {
      node.hidden = !currentRoles.includes(node.dataset.requires);
    });

    if (canReport) {
      const response = await fetch("/v1/reports/admin", {
        method: "GET",
        cache: "no-store",
        credentials: "omit",
        headers: { Authorization: `Bearer ${bearerToken}` },
      });
      if (response.status === 401) throw new Error("The access token was rejected.");
      if (response.status === 403) throw new Error("This token cannot read reports.");
      if (!response.ok) throw new Error(`The reporting service returned HTTP ${response.status}.`);
      const report = await response.json();
      if (report.contractVersion !== 1) throw new Error("Unsupported reporting contract version.");
      renderReport(report);
    }
    if (canManageCatalog) await loadCatalog();
    if (canReadAudit) await loadAudit();
    elements.tokenInput.value = "";
    elements.authPanel.hidden = true;
    elements.dashboard.hidden = false;
    elements.dashboardStatus.textContent = canReadAudit
      ? "Security audit authorized."
      : canManageCatalog
        ? "Catalog administration authorized."
        : "Read-only report authorized.";
    if (canReport) {
      elements.heroEyebrow.textContent = "Contract v1 reporting";
      elements.pageTitle.textContent = "The network, reconciled.";
      elements.heroCopy.textContent = "Review synchronized shifts, ticket revenue, drivers, and fares from one operational view.";
      elements.workspaceEyebrow.textContent = "Live reporting";
      elements.workspaceTitle.textContent = "Operations overview";
    } else if (canManageCatalog) {
      elements.heroEyebrow.textContent = "Managed reference data";
      elements.pageTitle.textContent = "The network, configured.";
      elements.heroCopy.textContent = "Publish drivers, buses, routes, stops, and fares as one controlled revision.";
      elements.workspaceEyebrow.textContent = "Catalog administration";
      elements.workspaceTitle.textContent = "Managed operations data";
      elements.generatedTime.textContent = "Catalog administration session.";
    } else {
      elements.heroEyebrow.textContent = "Authorization accountability";
      elements.pageTitle.textContent = "Access, accounted for.";
      elements.heroCopy.textContent = "Review protected API access decisions without exposing credential values.";
      elements.workspaceEyebrow.textContent = "Security audit";
      elements.workspaceTitle.textContent = "Authorization events";
      elements.generatedTime.textContent = "Security audit session.";
    }
    setConnection("live", canReadAudit ? "Security auditor" : canManageCatalog ? "Catalog admin" : "Report reader");
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
  currentCatalog = null;
  currentAudit = null;
  currentRoles = [];
  elements.tokenInput.value = "";
  elements.dashboard.hidden = true;
  elements.authPanel.hidden = false;
  elements.heroEyebrow.textContent = "Protected operations";
  elements.pageTitle.textContent = "The network, controlled.";
  elements.heroCopy.textContent = "Use a role-scoped credential to open reporting, catalog, or security audit.";
  elements.generatedTime.textContent = "Connect to open an authorized workspace.";
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
elements.refreshAuditButton.addEventListener("click", loadAudit);
elements.signOutButton.addEventListener("click", () => signOut());
elements.reloadCatalogButton.addEventListener("click", () => {
  loadCatalog().catch((error) => {
    elements.catalogStatus.textContent = error instanceof Error ? error.message : "Unable to load the catalog.";
  });
});
elements.saveCatalogButton.addEventListener("click", saveCatalog);
document.querySelectorAll(".catalog-form").forEach((form) => {
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const entity = form.dataset.entity;
    if (!entity || !currentCatalog) return;
    try {
      updateCatalogDraft(entity, catalogRecordFromForm(entity, form));
      setCatalogFormStatus(form, "Added to the draft. Publish the catalog to send this change to tablets.");
      form.reset();
      renderCatalog();
    } catch (error) {
      const message = error instanceof Error ? error.message : "Invalid catalog record.";
      elements.catalogStatus.textContent = message;
      setCatalogFormStatus(form, message, true);
    }
  });
});
document.querySelector('form[data-entity="scheduledTrips"] select[name="routeId"]')?.addEventListener("change", renderScheduledStopTimeFields);
document.querySelector('form[data-entity="scheduledTrips"] input[name="departureTime"]')?.addEventListener("change", renderScheduledStopTimeFields);
elements.driverFilter.addEventListener("change", renderShifts);
elements.fareFilter.addEventListener("change", renderShifts);
elements.shiftSearch.addEventListener("input", renderShifts);
window.addEventListener("pageshow", (event) => {
  if (event.persisted) signOut(false);
});
