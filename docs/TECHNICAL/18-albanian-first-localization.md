# Module 18 Technical Notes: Albanian-First Localization

## Scope

Module 18 moves Android user-facing text from Kotlin literals into locale resources and adds a persistent application-language override. Albanian is the unqualified base resource set and English is the qualified `values-en` set.

## Resource boundary

- `res/values/strings.xml` contains the Albanian default.
- `res/values-en/strings.xml` contains the English translation.
- Plural resources handle ticket and shift counts.
- Formatted strings handle runtime values without concatenating grammar in Compose.
- `DemoTransitData` resolves localized fare, route, stop, and eligibility labels from `Context` while stable IDs remain unchanged.
- `AppLanguageManager` persists `SYSTEM`, `ALBANIAN`, or `ENGLISH` and applies the resolved locale before application and activity creation.
- System selection maps Albanian device locales to Albanian and all other currently unsupported locales to the English fallback.
- App Bundle language splitting is disabled so both locale catalogs remain installed for runtime switching.

## State and policy boundary

`PilotRolePolicy` now returns typed `DriverShiftStatus` and `DriverSyncStatus` values rather than English strings. Compose maps those values to the active locale. This keeps workflow rules pure and independently testable while preventing policy code from becoming a hidden English source.

`DriverShiftViewModel` resolves operational messages through application resources. Printer discovery and failure fallbacks also use localized resources. Stored IDs, endpoint values, access tokens, and protocol values remain language-neutral.

## Locale switching

`BusPayApplication` and `MainActivity` wrap their base contexts before UI or view-model creation. Selecting a language updates the application resources and recreates the activity, which rebuilds localized demo catalog labels and operational messages. Persistent shift, ticket, printer, route, and fare identifiers remain stable across recreation.

The language callback waits briefly for the Material dropdown popup to dismiss before recreating the activity. The selected pilot workspace uses saveable Compose state, so the user returns to Operations and Setup after the language change instead of seeing a stale popup over the driver console.

## Automated validation

Run:

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Resource compilation validates format placeholders and locale catalogs. JVM tests validate the typed role and synchronization policies and all existing domain, printing, route, reporting, persistence-adjacent, and synchronization behavior.

## Regression boundaries

Module 18 does not change database schemas, ticket prices or IDs, sync payloads, acknowledgement rules, printer durability, role access, route progress, stop-request state, or reporting calculations.

## Validation status

Complete after automated validation and a successful device double-test, including a focused retest of the persisted language selection and dropdown-to-activity-recreation transition.
