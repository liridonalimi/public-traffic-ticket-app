# Native Android Environment Setup

This project is prepared as a native Android app using Kotlin and Jetpack Compose.

## Required tools

Install these on the Mac:

1. Android Studio
2. JDK 17 or newer
3. Android SDK Platform 35
4. Android SDK Build Tools

The easiest path is to install Android Studio, then open this folder:

```text
/Users/ljiridon.aljimi/Developer/public_traffic_ticket_app
```

Android Studio should detect the Gradle project and download the Android Gradle Plugin and Kotlin plugin automatically.

## First run

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Create an Android emulator or connect a real Android tablet.
4. Run the `app` configuration.

## Bluetooth label printer setup

The current hardware adapter supports Bluetooth Classic printers that accept ESC/POS commands over the serial-port profile (SPP).

1. On the Android tablet, open Bluetooth settings and pair the printer.
2. Start BusPay and allow the nearby-device/Bluetooth permission when requested.
3. In **Ticket printer**, tap **Refresh Paired Printers**.
4. Select the paired printer before starting ticket sales.
5. Run a low-value test sale and verify the label width, text, feed, and cutter behavior on the target hardware.

Printing failures are stored with the ticket. The driver can retry the last unprinted ticket without recording another sale, and cannot close the shift while a ticket remains unprinted.

Printers using ZPL, TSPL, CPCL, BLE-only transport, USB, or a vendor SDK require a matching adapter and ticket encoder; they are not compatible with the included ESC/POS SPP adapter.

## PDF test printer

Use the built-in PDF output when no physical printer is available:

1. Start BusPay; Bluetooth permission is not required for PDF testing.
2. Under **Ticket output**, select **PDF Test Printer**.
3. Start a shift, select a fare, and sell a ticket.
4. After the success message appears, tap **Open Last Ticket PDF**.
5. Confirm the PDF contains the Kosovo-style test layout: fare row, total, cash payment, ticket code, bus, route, driver/operator, sale time, test QR, and the visible `JO FISKAL` warning.

The PDF uses a narrow 58 mm ticket-style page and is stored in the app's private documents area. Android shares it with the selected PDF viewer through a read-only `FileProvider` URI. Its QR stores local test-ticket data only; it is not an ATK fiscal-verification QR. This validates ticket content and the application's sale-to-print workflow, but it does not validate fiscal compliance, Bluetooth, ESC/POS compatibility, paper handling, or physical print quality.

## Product direction

The first milestone is one driver tablet that can:

1. Start a shift.
2. Select a bus and route.
3. Track GPS.
4. Sell cash tickets offline.
5. Sync tickets to the server later.
6. Show next stop and stop-request state.

The code is already split into:

- `ui`: driver and passenger screens
- `domain`: bus, route, stop, shift, and ticket models
- `data`: local storage and sync layer
- `device`: GPS, printer, and stop-button integrations
