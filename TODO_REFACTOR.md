# Internal Code Refactor Tasks (Branding)

This document tracks planned internal code refactors to finalize the transition from Lawnchair to AutoCat branding. These are low-priority but important for codebase consistency.

## Tasks

- [ ] **Class Renaming**
  - Rename `LawnchairApp` to `AutoCatApp`.
  - Rename `AutoCatLauncher` to `AutoCatLauncher`.
  - Rename `AutoCatAccessibilityService` to `AutoCatAccessibilityService`.
  - Rename `AutoCatBugReporter` to `AutoCatBugReporter`.
  - Rename other utility classes prefixed with `Lawnchair`.

- [ ] **Package/Directory Structure**
  - Consider migrating `app.lawnchair` package to `app.autocat` (High risk, requires extensive manifest and resource updates).

- [ ] **Theme Renaming**
  - Rename `@style/Theme.AutoCat` to `@style/Theme.AutoCat`.

- [ ] **Manifest Cleanup**
  - Update all `android:name` references in `AndroidManifest.xml` after class renames.

- [ ] **Log Tags**
  - Systematically update all `TAG` constants from `Lawnchair*` to `AutoCat*`.

---
*Created on 2026-01-16*
