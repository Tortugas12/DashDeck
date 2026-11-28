# DashDeck

[![Android CI](https://github.com/Tortugas12/DashDeck/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Tortugas12/DashDeck/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android API](https://img.shields.io/badge/API-26%2B-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)

**DashDeck** is a customizable Android dashboard and Jetpack Glance home-screen widget app built with Kotlin, Jetpack Compose, Material 3 Adaptive, and Room. It brings together your clock, weather, calendar, notifications, media controls, pinned notes/checklists, and app launcher folders in one cohesive app and widget suite.

## Features

- **6 Configurable Dashboard Modules:**
  - **Clock & Weather:** 12/24-hour clock styles, configurable alignment, and optional weather display via coarse location.
  - **Calendar:** Device calendar event integration and monthly calendar view.
  - **Notifications:** Live notification summaries via `NotificationListenerService` with a privacy toggle to hide notification text.
  - **Media Controls:** Active media playback session tracking and progress updates.
  - **Pinned Notes & Checklists:** Local notes with checklist items plus **Pin to DashDeck** share-sheet support (`ACTION_SEND` `text/plain`, including sharing from Google Keep).
  - **App Launcher & Folders:** Custom app folders supporting both inline expansion and a translucent popup overlay (`FolderPopupActivity`).
- **Two Jetpack Glance Home-Screen Widgets:**
  - **DashDeck Full Dashboard:** All-in-one customizable dashboard widget (`DashboardWidgetReceiver`).
  - **DashDeck Modular Widget:** Compact widget focused on 1–2 modules (`ModularWidgetReceiver`).
  - Per-widget configuration (`WidgetConfigActivity`) for module selection, sizing tiers (`COMPACT`, `STANDARD`, `LARGE`), theme tokens, privacy options, and layout specs.
- **Adaptive UI & Desktop Support:** Material 3 Adaptive navigation (`NavigationBar` / `NavigationRail`) with quick search and keyboard shortcuts.
- **Backup Serialization:** Structured local backup serialization (`BackupSerializer`) for DashDeck configuration and data.

## Screenshots

<!-- Add screenshots of the DashDeck app, Full Dashboard widget, and Modular widget here when ready. -->
_Screenshots coming soon._

## Permissions

DashDeck uses Play-Store-safe `<queries>` declarations (without `QUERY_ALL_PACKAGES`) and requests only the permissions needed for enabled modules:

| Permission / Access | Purpose |
| :--- | :--- |
| `READ_CALENDAR` | Read upcoming device calendar events for the Calendar module |
| `SET_ALARM` | Open or trigger clock/alarm actions from the Clock module |
| `INTERNET` | Fetch weather updates for the Clock & Weather module |
| `ACCESS_COARSE_LOCATION` | Determine approximate location for local weather |
| Notification Listener (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Summarize active notifications and track active media sessions |

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin `2.3.20` (JDK 17) |
| **UI** | Jetpack Compose (BOM `2026.11.01`), Material 3, Material 3 Adaptive `1.3.0` |
| **Widgets** | Jetpack Glance (`glance-appwidget` & `glance-material3` `1.2.0`) |
| **Persistence** | Room 3 (`3.0.3` with KSP `2.3.6`) & DataStore Preferences `1.3.0` |
| **Serialization & Async** | `kotlinx.serialization` `1.11.0`, `kotlinx.coroutines` `1.10.2` |
| **Build & SDK** | Android Gradle Plugin `9.3.3`, `minSdk 26`, `targetSdk 37`, `compileSdk 37.1` |
| **Testing** | JUnit 4, `kotlinx-coroutines-test`, `glance-appwidget-testing` |

## Getting Started

### Prerequisites

- **JDK 17**
- **Android Studio** with Android SDK Platform **37** installed

### Build & Run

```bash
git clone https://github.com/Tortugas12/DashDeck.git
cd DashDeck
./gradlew assembleDebug
```

Install directly to a connected device or emulator:

```bash
./gradlew installDebug
```

### Run Tests & Lint

DashDeck enables strict lint checks (`warningsAsErrors = true` in `app/build.gradle.kts`):

```bash
./gradlew testDebugUnitTest lintDebug
```

## Project Structure

```text
app/src/main/java/com/dashdeck/app/
├── DashDeckApplication.kt      # App initialization & widget sync hooks
├── data/
│   ├── backup/                 # Backup serialization
│   ├── db/                     # Room 3 database, entities, and DAOs
│   ├── prefs/                  # DataStore preferences repository
│   └── repo/                   # Calendar, launcher, media, notes, notifications, weather
├── di/                         # Manual DI container (AppContainer)
├── service/                    # Notification & media listener service
├── theme/                      # Material 3 theme & token models
├── ui/                         # Dashboard, notes, launcher, settings, onboarding, popup, config
└── widget/                     # Jetpack Glance widgets, sync coordinator, and 6 widget modules
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.

## License

This project is licensed under the MIT License—see [LICENSE](LICENSE) for details.
