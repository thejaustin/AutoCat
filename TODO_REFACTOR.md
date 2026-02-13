# Internal Code Refactor Tasks (Branding)

This document tracks planned internal code refactors to finalize the transition from Lawnchair to AutoCat branding. These are low-priority but important for codebase consistency.

## Tasks

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

---
*Created on 2026-01-16*
