# GitHub Issues Backlog

This document serves as a local synchronization point for GitHub issues. It aggregates tasks from `TODO_REFACTOR.md`, `ROADMAP.md`, `AUTOCAT_CONTEXT.md`, and active conversation history.

## 🚀 High Priority (Next Up)

### Issue #1: Rename Application Class to AutoCatApp
**Source:** `TODO_REFACTOR.md`
**Description:**
Rename the main `LawnchairApp` class to `AutoCatApp` to reflect the new branding.
- [x] Rename file and class `AutoCatApp.kt` -> `AutoCatApp.kt`
- [x] Update `AndroidManifest.xml` references
- [x] Update any string/log references

### Issue #2: Branding - Class Renaming Package
**Source:** `TODO_REFACTOR.md`
**Description:**
Continue renaming core classes to match AutoCat branding.
- [x] Rename `LawnchairLauncher` -> `AutoCatLauncher`
- [x] Rename `LawnchairAccessibilityService` -> `AutoCatAccessibilityService`
- [x] Rename `LawnchairBugReporter` -> `AutoCatBugReporter`

### Issue #3: Implement Retry Logic with Exponential Backoff for LLM
**Source:** `AUTOCAT_CONTEXT.md`
**Description:**
Currently, LLM calls fail immediately on network error. Implement a robust retry mechanism.
- [x] Add exponential backoff (e.g., 1s, 2s, 4s)
- [x] Handle `429 Too Many Requests` specifically

### Issue #4: Add Folder Sync Mode Preference UI
**Source:** `AUTOCAT_CONTEXT.md`
**Description:**
The folder sync mode is currently hardcoded or hidden. Add a UI selector in Settings.
- [x] Add UI selector in Categorization Settings
- [x] Use `autoCatFolderSyncMode` preference
- [x] Define mode options: `DRAWER`, `HOME_SCREEN`, `BOTH`

## 🛠 Features & Enhancements

### Issue #5: Material 3 Expressive Settings Overhaul
**Source:** `ROADMAP.md`
**Description:**
UI/UX overhaul of all Settings screens to match Material 3 Expressive design guidelines.

### Issue #6: Parallel Batch Processing for Categorization
**Source:** `AUTOCAT_CONTEXT.md`
**Description:**
Currently, batches are processed sequentially. Implement `async`/`await` patterns to process multiple batches in parallel for faster initial categorization.
- [x] Implemented in `LLMCategorizer.categorizeBatchAPI` using `chunked(PARALLEL_BATCH_LIMIT)` and `async`/`awaitAll`.

### Issue #7: Smart Launcher Backup Import
**Source:** `AUTOCAT_CONTEXT.md`
**Description:**
Verify and test the importing of categories from Smart Launcher `.slbk` files.

### Issue #8: Developer Diagnostics UI Verification
**Source:** `AUTOCAT_CONTEXT.md`
**Description:**
Ensure the new Developer Mode > Diagnostics UI correctly displays internal state, logs, and database stats.
- [x] Created `DiagnosticsPreferences.kt`
- [x] Added database statistics (apps, overrides, tabs)
- [x] Added live LLM log viewer
- [x] Added debug actions (Clear logs, Clear database)
- [x] Integrated with `autoCatDevMode` preference

## 🎨 UI/UX Roadmap

### Issue #9: Proper Icon Swipe Gestures
**Source:** `ROADMAP.md`

### Issue #10: Folder "Cover" Mode
**Source:** `ROADMAP.md`

### Issue #11: Widget Stacking
**Source:** `ROADMAP.md`
**Note:** Complex feature, long term.

## 🧹 Refactoring & Debt

### Issue #12: Theme Renaming
**Source:** `TODO_REFACTOR.md`
**Description:**
Rename `@style/Theme.Lawnchair` to `@style/Theme.AutoCat`.
- [x] Rename XML styles in `themes.xml` (values & values-night)
- [x] Update Manifest `android:theme`
- [x] Update Kotlin `R.style.Theme_AutoCat` and `AutoCatTheme` Composable

### Issue #13: Log Tag Cleanup
**Source:** `TODO_REFACTOR.md`
**Description:**
Systematically update all `TAG` constants from `Lawnchair*` to `AutoCat*`.
- [x] Update string literal tags in `AutoCatShortcut.kt` (formerly `LawnchairShortcut.kt`)
- [x] Update `TAG` constants in `AutoCatAlphabeticalAppsList.kt`, `AutoCatIconProvider.kt`, `AutoCatLockedStateController.kt`

### Issue #14: Package Structure Migration (Long Term)
**Source:** `TODO_REFACTOR.md`
**Description:**
Migrate `app.lawnchair` package to `app.autocat`. **Warning:** High risk.

## 📝 Documentation & Process

### Issue #15: Sync Conversation History
**Source:** Conversation
**Description:**
Ensure that key decisions and context from CLI sessions are persisted into `AUTOCAT_CONTEXT.md` or similar documentation to prevent knowledge loss.

### Issue #16: Update Roadmap
**Source:** Conversation
**Description:**
Regularly update `ROADMAP.md` to reflect completed tasks (like the Android 16 rebase progress or AutoCat fork status).
