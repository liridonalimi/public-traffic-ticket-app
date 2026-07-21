# Bluetooth and PDF Ticket Printing

## Purpose

Module 06 connects every ticket sale to immediate output. It supports paired Bluetooth Classic ESC/POS receipt or label printers and includes a built-in PDF test printer for validation when physical printer hardware is unavailable.

## What the driver can do

- Use the PDF test printer without granting Bluetooth permission.
- Grant Bluetooth permission and refresh the list of paired printers.
- Select a paired physical printer or the PDF test printer.
- Sell a ticket and print it automatically.
- See whether printing succeeded or failed.
- Retry the last failed or pending print without recording another sale.
- Open the most recently created ticket PDF in an installed PDF viewer.

## Reliable sale and retry behavior

The ticket is stored before printing begins. Its print state is recorded as pending, printed, or failed, together with the number of attempts and the last error. A retry reuses the existing ticket and does not increase the shift's ticket count or cash total.

The driver cannot end the shift while any ticket is still pending or failed. This prevents a print problem from disappearing when the active shift is closed.

## Ticket information

Both Bluetooth and PDF outputs use the same sale information:

- ticket code
- bus plate number
- route
- driver/operator
- fare type and price
- sale date and time

The PDF uses a narrow 58 mm-style layout with an Albanian ticket heading, fare row, total, cash-payment label, sale metadata, and test QR code.

## Important non-fiscal boundary

The PDF is deliberately marked **KUPON TESTUES - JO FISKAL**. Its QR code contains only local test-ticket data and is not an ATK fiscal-verification code. The PDF validates application content and workflow; it does not represent fiscal approval or compliance.

Fiscal identifiers, tax declarations, official fiscal logos, and digital signatures must only be added through an authorized fiscalization integration.

## Bluetooth printer compatibility

The included physical-printer adapter supports Bluetooth Classic devices that accept ESC/POS commands over the serial-port profile (SPP). It does not support ZPL, TSPL, CPCL, BLE-only printers, USB printers, or proprietary vendor protocols without another adapter.

Physical acceptance testing is still required for every target printer model because paper width, character tables, cutter support, feed behavior, speed, and connection reliability differ between devices.

## Android permissions

Android 12 and newer require nearby-device/Bluetooth permission before the app can inspect paired devices or connect to a printer. Earlier Android versions use legacy Bluetooth permissions. PDF testing remains available when Bluetooth access is denied.

## Testing and validation

1. Deny Bluetooth permission, select the PDF test printer, and sell a ticket. Expected: the sale succeeds and a readable PDF is created.
2. Open the PDF and verify ticket code, bus, route, driver, fare, amount, sale time, QR, and the prominent `JO FISKAL` marking.
3. Simulate or cause a print failure. Expected: the ticket remains stored with failed/pending status and shift closure is blocked.
4. Retry printing. Expected: the same ticket ID is printed, the attempt count increases, and ticket/cash totals do not increase.
5. After successful output, close the shift and confirm its ticket count and cash match the single stored sale.
6. If physical hardware is available, repeat through Bluetooth Classic ESC/POS and verify pairing, reconnect, Albanian characters, paper feed, and failure recovery. PDF success alone does not certify the hardware.

The acceptance result is durable sale-before-print behavior, duplicate-safe retry, and clear separation from fiscal certification.

## Current limitations

- The PDF test output cannot validate Bluetooth transport or physical paper handling.
- The ticket is non-fiscal until an authorized fiscal service is integrated.
- Only the most recent PDF is directly exposed from the current console.
- Printer compatibility beyond ESC/POS over Bluetooth Classic requires additional implementations.

## Next module

Module 07 added GPS route progress and a synchronized passenger display showing current and next stops.
