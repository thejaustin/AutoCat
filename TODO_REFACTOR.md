# Internal Code Refactor Tasks (Branding)

This document tracks the internal code refactors completed to finalize the transition from Lawnchair to AutoCat branding.

## Completed Tasks

- [x] **Class Renaming**
  - [x] Rename `LawnchairApp` to `AutoCatApp`.
  - [x] Rename `LawnchairLauncher` to `AutoCatLauncher`.
  - [x] Rename `LawnchairAccessibilityService` to `AutoCatAccessibilityService`.
  - [x] Rename `LawnchairBugReporter` to `AutoCatBugReporter`.
  - [x] Rename `LawnchairUtils` to `AutoCatUtils`.
  - [x] Rename `LawnchairBackup` to `AutoCatBackup`.
  - [x] Rename Search algorithms and providers to `AutoCat*`.
  - [x] Rename other utility classes prefixed with `Lawnchair`.

- [ ] **Package/Directory Structure**
  - Consider migrating `app.lawnchair` package to `app.autocat` (High risk, requires extensive manifest and resource updates).

- [x] **Theme Renaming**
  - [x] Rename `@style/Theme.Lawnchair` to `@style/Theme.AutoCat`.

- [x] **Manifest Cleanup**
  - [x] Update all `android:name` references in `AndroidManifest.xml` after class renames.

- [x] **Log Tags**
  - [x] Systematically update all `TAG` constants from `Lawnchair*` to `AutoCat*`.

- [x] **Resources & Protos**
  - [x] Rename `lawnchair.proto` to `autocat.proto` and outer class to `AutoCatProto`.
  - [x] Updated localized strings and copyright headers.

---
*Last Updated: February 12, 2026*
