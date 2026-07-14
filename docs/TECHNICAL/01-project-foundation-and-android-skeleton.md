# Technical: Project Foundation and Android Skeleton

## Scope

Module 01 created the initial native Android project and the architectural boundaries used by every later BusPay module. It established a runnable Kotlin and Jetpack Compose application, core transport models, placeholder integration interfaces, and the Gradle/Android configuration required for development.

## Project configuration

The foundation includes:

- a single Android application module named `app`
- Kotlin Android and Jetpack Compose support
- Gradle wrapper scripts for reproducible builds
- Android manifest, resources, application theme, and launcher activity
- Java 17-compatible source and target configuration
- minimum and target Android SDK configuration
- a repository-level Android setup guide

`MainActivity` is the application entry point and installs the Compose content tree through `setContent`.

## Package structure

The initial source layout separated the application into four responsibilities:

- `com.buspay.app.ui`: Compose presentation and screen state.
- `com.buspay.app.domain`: Transport and ticketing entities and business rules.
- `com.buspay.app.data`: Local persistence and future synchronization boundaries.
- `com.buspay.app.device`: Android and vehicle-device integrations.

This structure keeps platform-specific code away from the core transport model and allows later modules to replace demo or placeholder implementations incrementally.

## Initial domain model

The foundation introduced the main entities used by later workflows:

- `Driver`
- `Bus`
- `Route`
- `Stop`
- `Shift`
- `Ticket`

Identifiers are represented as strings so locally generated demo identifiers and future server identifiers can use the same model boundary. Times are represented as epoch milliseconds for simple persistence and transport compatibility.

The initial `Route` model owns an ordered list of stops. Each `Stop` contains an ID, name, latitude, longitude, and order value, preparing the model for later GPS route progress.

## Integration boundaries

Module 01 defined interfaces for responsibilities expected to depend on Android or external hardware:

- `GpsTracker` for starting and stopping location tracking.
- `TicketPrinter` for ticket output.
- `StopRequestInput` for receiving passenger stop requests.

These were intentionally minimal placeholders. Later modules expanded or implemented them without moving hardware concerns into the domain model.

## Offline-first boundary

The initial data package reserved `OfflineFirstRepository` as the application-owned persistence boundary. Module 01 did not yet persist operational data; later modules implemented active-shift, driver, ticket, printer, and route-progress storage behind this boundary.

## Initial user interface

The first Compose screen provided the driver-console shell and demonstrated that the application could render as a native Android app. Operational state handling and business actions were deliberately left to subsequent modules.

## Design decisions

- **Native Android:** chosen for direct access to Bluetooth, GPS, local storage, and vehicle-connected hardware.
- **Kotlin:** provides the primary Android language and concise immutable data models.
- **Jetpack Compose:** enables state-driven UI without XML layout files.
- **Layered packages:** separate presentation, business data, persistence, and hardware concerns.
- **Offline-first direction:** allows ticketing workflows to continue before server connectivity is available.

## Verification boundary

Module 01 acceptance consisted of Gradle synchronization, successful Android compilation, application installation, and rendering the initial driver console on an emulator or physical device. Business-rule unit tests were added when later modules introduced testable operational behavior.

## Initial limitations

- No complete driver shift workflow.
- No persistent ticket or shift storage.
- No driver authentication.
- No working printer, GPS, or stop-request adapter.
- No backend synchronization or reporting contract.
- Demo-only presentation and data.

These limitations represented planned module boundaries rather than unfinished foundation work.

## Next technical step

Module 02 implemented the first driver shift flow, including bus and route selection, shift start/end actions, ticket counting, and cash totals.
