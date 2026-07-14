# Project Foundation and Android Skeleton

## Purpose

Module 01 established the native Android foundation for the BusPay public-transport ticketing pilot. Its purpose was to create a runnable, maintainable project that later modules could extend with driver shifts, offline ticket sales, printing, route tracking, and passenger information.

## What this module delivered

- A native Android application written in Kotlin.
- A Jetpack Compose user-interface foundation.
- Gradle configuration, wrapper scripts, and an Android application module.
- Android manifest, application theme, resources, and launcher activity.
- An initial driver-console screen.
- Base domain models for drivers, buses, routes, stops, shifts, and tickets.
- Package boundaries for user interface, domain logic, local data, and device integrations.
- Initial interfaces for GPS tracking, ticket printing, and stop-request input.
- An Android development and first-run setup guide.

## Architecture established

The application was divided into four main responsibilities:

- `ui`: Compose screens and driver/passenger presentation.
- `domain`: Core transport and ticketing models and business rules.
- `data`: Offline-first storage and future synchronization responsibilities.
- `device`: Android and vehicle-hardware integration boundaries.

This separation lets later modules replace demo implementations without rewriting the whole application. For example, local demo data can later be replaced by server data, and one printer adapter can be replaced by another implementation of the same boundary.

## Business value

The foundation reduced implementation risk before business workflows were added. It established one installable Android project, a shared transport vocabulary, and clear integration points for tablet hardware and future backend services.

## Current status

The foundation remains the base of the application. Later modules have filled in the original placeholders with persistent shifts and tickets, driver identity, multiple fares, ticket printing, GPS progress, and a passenger display.

## Initial limitations

Module 01 intentionally did not provide a complete operational workflow. Driver authentication, shift handling, permanent local storage, printer communication, live GPS behavior, server synchronization, and reporting were scheduled for later modules.

## Next module

Module 02 added the first complete driver shift flow with bus and route selection, ticket totals, and shift closure.
