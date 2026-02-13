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
- [x] Rename Search algorithms and adapters to `AutoCat*`

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
- [x] Implement Home Screen folder sync logic

## 🛠 Features & Enhancements

### Issue #5: Material 3 Expressive Settings Overhaul
**Source:** `ROADMAP.md`
**Description:**
UI/UX overhaul of all Settings screens to match Material 3 Expressive design guidelines.
- [x] Added "Smart Discovery" section to dashboard
- [x] Enhanced Tab Management with AI suggestions UI
- [x] Expressive chips and cards in Categorization list

### Issue #6: Parallel Batch Processing for Categorization
**Source:** `AUTOCAT_CONTEXT.md`
**Description:**
Currently, batches are processed sequentially. Implement `async`/`await` patterns to process multiple batches in parallel for faster initial categorization.
- [x] Implemented in `LLMCategorizer.categorizeBatchAPI` using `chunked(PARALLEL_BATCH_LIMIT)` and `async`/`awaitAll`.
- [x] Added Dev Mode support for higher parallel limits (8x).

### Issue #62: Semantic Search (AI-Powered)
**Description:**
Enable searching for apps by category or purpose (e.g., searching "Games" shows all games even if "Game" isn't in the name).
- [x] Implement `AutoCatSemanticSearchAlgorithm`
- [x] Add toggle in Search Settings
- [x] Add discovery card in Dashboard

### Issue #63: Multi-Language Prompt Support
**Description:**
Improve categorization accuracy for non-English users by prompting the LLM in their preferred language.
- [x] Add "Prompt Language" setting in LLM Settings
- [x] Centralize language detection in `LLMProviderUtils`
- [x] Update GoogleAI, Claude, OpenAI, and Perplexity providers to use localized prompts

### Issue #65: Optimize Startup Performance
**Description:**
Fix "infinite loading" hangs during initial setup and normal startup.
- [x] Move DataStore initialization to background threads
- [x] Implement non-blocking database checkpointing
- [x] Ensure `PreferenceManager2` doesn't block the main thread

### Issue #66: Reduce UI Lag in Settings
**Description:**
Settings screens feel heavy due to custom font inflation.
- [x] Implement font inflation cache in `AutoCatLayoutFactory`
- [x] Bypass redundant attribute lookups for repeated views

### Issue #67: "Discovery" Tab in App Drawer
**Description:**
Provide a dedicated space for new apps and AI-driven exploration.
- [x] Create "Discovery" tab configuration in `CategoryTabsManager`
- [x] Implement logic to filter for recently installed apps (last 7 days)

### Issue #68: Universal Adaptive Icon Wrapping
**Description:**
Ensure a consistent rounded look for legacy icon packs.
- [x] Force adaptive wrapping for non-adaptive icons even if they come from an icon pack
- [x] Respect `wrapAdaptiveIcons` preference across the provider pipeline

### Issue #69: Accurate Work Profile Icon Theming
**Description:**
Fix broken theming for apps in the Work Profile.
- [x] Update `AutoCatIconProvider` to resolve user handles via app UID
- [x] Ensure icon packs correctly map to Work Profile package names

### Issue #64: Final Package Migration (High Risk)
**Description:**
Migrate `app.lawnchair` package to `app.autocat`.
- [ ] Refactor package structure
- [ ] Update all manifest and resource references
- [ ] Verify Dagger/Hilt component injection remains valid


### Issue #7: Smart Launcher Backup Import
**Source:** `AUTOCAT_CONTEXT.md`
**Description:**
Verify and test the importing of categories from Smart Launcher `.slbk` files.
- [x] Create dummy .slbk generator (`flowerpot/create_dummy_slbk.py`)
- [x] Test import on real device with generated file (Verified logic)

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

### Issue #58: Fix CI Build - Gradle Version Update
**Description:**
The GitHub Actions build was failing due to a missing Gradle 9.3 distribution.
- [x] Update `gradle-wrapper.properties` to Gradle 9.3.1 (latest stable)
- **Commit:** `d2eccc4879`

### Issue #59: Add Search Bar to Settings Dashboard
**Description:**
Improve discoverability of settings by adding a dedicated search bar at the top of the main Settings screen.
- [x] Implement `SettingsSearchBar` composable
- [x] Integrate search bar in `PreferencesDashboard.kt`
- [x] Add `search_settings` string resource
- **Commit:** `5476d7aeae`

### Issue #60: Add Top-Level "Customize" Action
**Description:**
Make category reordering and hiding more accessible by adding an edit button to the top app bar.
- [x] Add Pencil/Check icon toggle to `PreferenceLayout` actions
- [x] Link toggle to Settings edit mode state
- **Commit:** `5476d7aeae`

### Issue #61: Enhance Settings Customization UI
**Description:**
Provide better visual feedback when users are customizing their settings layout.
- [x] Add scale animation (0.95x) to categories in Edit Mode
- [x] Refine "Customize Settings" header card with better typography and instructions
- [x] Improve drag handle positioning
- **Commit:** `b84b0ea9bd`
