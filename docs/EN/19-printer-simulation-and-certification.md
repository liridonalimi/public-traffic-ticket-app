# Module 19 - Printer Simulation and Certification

## Outcome

Module 19 makes receipt validation possible before selecting production printer hardware. The Android pilot exposes separate 58 mm and 80 mm PDF simulators, generates width-aware ESC/POS output, improves the non-fiscal receipt layout, and places the hardware certification gate in Operations and Setup.

## Receipt simulators

The printer selector now includes:

- `PDF Simulator 58 mm`, representing compact mobile thermal printers with 32 text columns;
- `PDF Simulator 80 mm`, representing wider counter or vehicle printers with 48 text columns.

Each simulator creates a separate PDF filename containing the selected paper width. Both layouts include fare, total, cash method, ticket code, bus, line, operator, sale time, a local-data QR code, and prominent non-fiscal markings.

## ESC/POS quality

- Receipt rows align the price to the right edge for the selected character width.
- Long line, operator, and metadata values wrap at word boundaries.
- The default physical Bluetooth profile remains the conservative 58 mm/32-column format until a specific printer is certified.
- Initialization, alignment, bold control, feed, and full-cut commands remain present.
- Failed printing remains durable and retryable without allowing shift closure while tickets are unprinted.

## Tablet validation checklist

1. Between shifts, open the ticket-printer selector and confirm both PDF simulators appear.
2. Select the 58 mm simulator, start a shift, and sell one ticket from every fare category.
3. Open the last PDF and check wrapping, alignment, amount, identifiers, QR, and `JO FISKAL` markings.
4. End the shift, select the 80 mm simulator, start another shift, and repeat with the longest line and fare names.
5. Confirm the 80 mm PDF is wider and its filename ends with `-80mm.pdf`.
6. Restart the app and confirm the selected simulator is restored.
7. If a physical printer is available, complete every certification check below before approving it.

## Hardware certification gate

A physical model is not production-certified until all of these pass:

- pairing, reconnection, app restart, printer restart, and vehicle power-cycle recovery;
- correct 58 mm or 80 mm roll, feed, margins, darkness, and cutting or tear-bar behavior;
- readable Albanian `ë` and `ç`, long line names, each fare, totals, and timestamps;
- QR scanning under normal vehicle lighting and an unmistakable `NOT FISCAL` designation;
- paper-out, cover-open, Bluetooth-off, out-of-range, and mid-print failure recovery;
- retry prints the same durable ticket and does not create a second sale;
- shift closure stays blocked until the ticket is successfully printed;
- 50 consecutive tickets without disconnect, truncation, overheating, or unacceptable battery drain.

Record the manufacturer, model, firmware, paper width, character/code-page behavior, test date, tester, and evidence before certification.

## Acceptance criteria

- Both simulator widths produce readable, explicitly non-fiscal receipts.
- ESC/POS text never exceeds the selected profile width.
- Long values wrap without losing ticket data.
- Existing printing failure and retry safeguards remain intact.
- No physical printer is described as certified until the hardware checklist passes.

## Validation status

Software and simulator validation was completed on 17 July 2026. The 58 mm and 80 mm PDFs were confirmed to have visibly different widths, readable content, contained QR codes, and no clipping or overlap. The selected simulator was also confirmed to persist after the app was fully closed and reopened.

Module 19 is complete for the app and simulator scope. Physical-printer certification remains pending because no printer hardware is currently available; a model must pass the hardware certification gate before production approval.
