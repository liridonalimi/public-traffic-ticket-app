# Module 19 Technical Notes: Printer Simulation and Certification

## Scope

Module 19 adds paper-profile-aware receipt rendering and a certification boundary without changing ticket persistence or print-state durability.

## Paper profiles

`ReceiptPaperProfile` defines:

- `MM58`: 58 mm, 32 text columns, 164 PDF points;
- `MM80`: 80 mm, 48 text columns, 226 PDF points.

`PdfTicketPrinter` exposes one virtual device per profile. The existing `pdf://ticket-preview` address remains the 58 mm address for backward compatibility with saved printer preferences. The 80 mm simulator uses `pdf://ticket-preview/80mm`.

Generated filenames include `-58mm.pdf` or `-80mm.pdf`. PDF column positions and available wrap width derive from the selected profile rather than fixed page coordinates.

## ESC/POS formatting

`escPosTicketText` produces the printable body independently from transport commands. `alignedReceiptRow` reserves the right edge for prices, and `wrapReceiptText` keeps every line within the profile column count. `buildEscPosTicketBytes` surrounds that body with initialization, alignment, bold, feed, and cut commands.

Physical Bluetooth devices currently use `MM58` by default. A future managed hardware profile may associate a certified physical address/model with `MM80`; Module 19 does not guess capabilities from a Bluetooth name.

## UI boundary

The driver printer selector lists both simulators alongside paired Bluetooth devices. Operations and Setup displays the required hardware checks but does not claim certification or persist a certification result. Production approval requires external evidence and an accountable tester.

## Automated validation

Run:

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Tests cover both paper-profile mappings, exact right-edge alignment, long-value wrapping, maximum ESC/POS line width, ticket fields, euro amounts, QR non-fiscal payloads, and cut commands. Existing domain and workflow tests remain part of the gate.

## Regression boundaries

Module 19 does not change ticket IDs, prices, database schemas, print status, retry attempts, shift-closing guards, synchronization payloads, route state, or reporting calculations.

## Validation record

On 17 July 2026, tablet validation confirmed simulator selection and persistence across a full app restart. The supplied 58 mm PDF measured 164 x 420 points and the 80 mm PDF measured 226 x 420 points. Rendered inspection confirmed that text, separators, totals, non-fiscal markings, and QR codes remain within the page with no clipping or overlap.

The software scope is complete. No physical printer is certified: hardware-dependent pairing, recovery, paper handling, Albanian code-page behavior, QR scanning, failure injection, and endurance checks remain pending until a printer is available.
