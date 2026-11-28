# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-11-27

### Added
- Initial open-source release of the DashDeck Android application (`com.dashdeck.app`).
- Six customizable dashboard modules: Clock & Weather, Calendar, Notifications (with privacy text-hiding), Media Controls, Pinned Notes & Checklists (with share-to-pin support), and App Launcher with inline and popup folders.
- Two Jetpack Glance home-screen widgets (`DashDeck Full Dashboard` and `DashDeck Modular Widget`) with per-widget configuration (`WidgetConfigActivity`) and live sync (`WidgetSyncCoordinator`).
- Adaptive Material 3 navigation (`Dashboard`, `Notes`, `Launcher`, `Settings`), quick search dialog, permission checklist onboarding, and keyboard shortcuts.
- Room 3 local persistence, DataStore preferences, and structured backup serialization (`BackupSerializer`).
- Unit test suite covering backup serialization, calendar mapping, Keep share parsing, media progress, notification filtering, weather, theme models, folder popup positioning, and Glance widgets.
- MIT License, project `README.md`, hardened `.gitignore`, and GitHub Actions Android CI workflow.

[Unreleased]: https://github.com/Tortugas12/DashDeck/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Tortugas12/DashDeck/releases/tag/v0.1.0
