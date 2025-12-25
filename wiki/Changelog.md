# Changelog

All notable changes to AutoCat are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [15.0.b1-autocat.100+] - 2025-12-25

### Added
- Model accuracy tracking for LLM providers based on user corrections
- Adaptive model auto-selection that learns which provider performs best
- `ModelAccuracy` database entity and `AccuracyDao` for analytics
- `AccuracyTracker` service for recording prediction outcomes
- `AdaptiveModelSelector` service for intelligent provider selection
- Visual performance metrics in LLM Settings with color-coded ratings
- "⚡ ACTIVE" badge for auto-selected model
- Auto-select toggle in Provider Selection settings
- Highlighted card UI for active model with primary color border

### Changed
- Updated `TabDatabase` from version 5 to 6
- LLMCategorizer now uses auto-selected provider when enabled
- Provider selection falls back to manual preference when insufficient data

### Fixed
- Missing `AdaptiveModelSelector` import in LLMSettingsPreferences
- Code formatting violations in `AdaptiveModelSelector.kt`

**Links**: [#18](https://github.com/thejaustin/AutoCat/issues/18), Commits: 432c961...79cbc88

---

## [15.0.b1-autocat.99] - 2025-12-24

### Changed
- Applied Material 3 Expressive design to LLM Settings screen
- Applied Material 3 Expressive design to Categorization Settings screen
- Enhanced visual hierarchy and spacing

**Links**: Commits: c46a4f1, 71cc812

---

## [15.0.b1-autocat.98] - 2025-12-24

### Changed
- Implemented batch database inserts in BuiltInCategorizer for better performance

**Links**: Commit: 5b76004

---

## [15.0.b1-autocat.97] - 2025-12-23

### Changed
- Complete Material 3 Expressive redesign of category management screens
- Enhanced app categorization UI with Material 3 Expressive design

**Links**: Commits: 5a26db9, fe52c4a

---

## [15.0.b1-autocat.96] - 2025-12-22

### Added
- Merged upstream Lawnchair 15-dev updates

### Fixed
- Added missing Flowerpot import after upstream merge

**Links**: Commits: bd56dfa, e4776fb

---

## [15.0.b1-autocat.95] - 2025-12-21

### Changed
- Optimized app startup by skipping already-categorized apps

### Fixed
- Fixed remaining background() usage in reasoning section
- Corrected background usage in AppCategorizationListPreferences

**Links**: Commits: 71f5747, 709e674, 30fb2e3, 5c66b84

---

## [15.0.b1-autocat.94] - 2025-12-20

### Changed
- Migrated LLM API calls from HttpURLConnection to OkHttpClient
- Improved reliability and performance of network requests

**Links**: Commit: 2e361aa

---

## [15.0.b1-autocat.93] - 2025-12-17 🔴 CRITICAL

### Added
- Environment variable support for all LLM provider API keys
  - `GOOGLE_AI_API_KEY`, `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `PERPLEXITY_API_KEY`

### Fixed
- **CRITICAL**: App categorization screen freeze/crash on open ([#31](https://github.com/thejaustin/AutoCat/issues/31))
  - Moved heavy service initialization off UI thread
  - Fixed 4 preference screens (AppCategorization, CategorizationSettings, CategoryManagement, LLMSettings)
  - Added comprehensive error handling
  - Made all service operations null-safe
- Database schema mismatch error ([#35](https://github.com/thejaustin/AutoCat/issues/35))
  - Incremented TabDatabase version from 4 to 5
  - Added `llm_provider` and `llm_model` fields to AppTab entity
- "Get AI Suggestions" button ignoring Google AI env variable ([#34](https://github.com/thejaustin/AutoCat/issues/34))

**Links**: [#31](https://github.com/thejaustin/AutoCat/issues/31), [#34](https://github.com/thejaustin/AutoCat/issues/34), [#35](https://github.com/thejaustin/AutoCat/issues/35)

---

## [15.0.b1-autocat.92] - 2025-12-16 ⭐ MAJOR

### Added
- Circuit breaker for LLM providers ([#30](https://github.com/thejaustin/AutoCat/issues/30))
  - Automatic failover on provider failures
  - Configurable failure thresholds and timeouts
  - UI controls in LLM Settings
- Confidence score calibration ([#30](https://github.com/thejaustin/AutoCat/issues/30))
  - Provider-specific calibration factors
  - Improved categorization accuracy
  - Model tracking in AppTab entities
- Advanced settings toggles ([#29](https://github.com/thejaustin/AutoCat/issues/29))
  - Option to hide Quickstep settings
  - Option to hide settings warnings

**Links**: [#29](https://github.com/thejaustin/AutoCat/issues/29), [#30](https://github.com/thejaustin/AutoCat/issues/30)

---

## [15.0.b1-autocat.91] - 2025-12-15

### Fixed
- UI blocking issues in LawnchairShortcut (removed runBlocking)
- Cache race condition in AutoCatAppProvider
- Moved blocking database calls to Dispatchers.IO

**Links**: Commits: 048bd16, 0888f1e

---

## [15.0.b1-autocat.90] - 2025-12-14

### Changed
- Moved category tabs to bottom of app drawer for better reachability ([#23](https://github.com/thejaustin/AutoCat/issues/23))

**Links**: [#23](https://github.com/thejaustin/AutoCat/issues/23)

---

## [15.0.b1-autocat.~88] - 2025-12-13

### Added
- Created `CategorizationOverviewPreferences.kt` - New overview screen with categorization stats
- Created `CategorizationSettingsPreferences.kt` - Centralized settings hub
- Filter dropdowns for apps by tab

### Changed
- Reorganized settings navigation for better UX
- Fixed UI lag with async loading in preferences

**Links**: Commits: 6bde253, 3fd9627, 50b6071

---

## [15.0.b1-autocat.~85] - 2025-12-12

### Added
- Smart Launcher .slbk import support ([#28](https://github.com/thejaustin/AutoCat/issues/28))
  - Created `SmartLauncherImporter.kt` for importing .slbk backup files
  - Auto-create categories from Smart Launcher data
- Folder Auto-Sort Service
  - Created `FolderAutoSortService.kt` for automatic folder organization
  - LLM-powered folder suggestions
  - Integration with categorization pipeline

### Changed
- Wired up Smart Launcher import in settings UI

**Links**: [#28](https://github.com/thejaustin/AutoCat/issues/28), [#20](https://github.com/thejaustin/AutoCat/issues/20), Commits: 004772a, ae7e4cc, 41598d4

---

## [15.0.b1-autocat.~80] - 2025-12-10 🔴 SECURITY

### Added
- Prompt injection protection for all LLM providers ([#11](https://github.com/thejaustin/AutoCat/issues/11))
- Socket timeout configuration for all providers ([#10](https://github.com/thejaustin/AutoCat/issues/10))

### Changed
- Fixed N+1 query problem with 80-90% speedup ([#2](https://github.com/thejaustin/AutoCat/issues/2))
- Made CategoryFolderSyncService lazy to prevent memory leaks ([#9](https://github.com/thejaustin/AutoCat/issues/9))
- Optimized object allocations in app drawer hot path ([#6](https://github.com/thejaustin/AutoCat/issues/6))
- All categorization properties now lazy-loaded for faster startup

### Fixed
- Exponential backoff calculation bug ([#7](https://github.com/thejaustin/AutoCat/issues/7))
- JSON parsing error recovery ([#14](https://github.com/thejaustin/AutoCat/issues/14))
- Cache race conditions in AutoCatAppProvider ([#5](https://github.com/thejaustin/AutoCat/issues/5))

### Security
- Input sanitization to prevent prompt injection attacks
- Escaped special characters in category names
- LLM response validation before database insertion

**Links**: [#2](https://github.com/thejaustin/AutoCat/issues/2), [#5](https://github.com/thejaustin/AutoCat/issues/5), [#6](https://github.com/thejaustin/AutoCat/issues/6), [#7](https://github.com/thejaustin/AutoCat/issues/7), [#9](https://github.com/thejaustin/AutoCat/issues/9), [#10](https://github.com/thejaustin/AutoCat/issues/10), [#11](https://github.com/thejaustin/AutoCat/issues/11), [#14](https://github.com/thejaustin/AutoCat/issues/14)

---

## [15.0.b1-autocat.~75] - 2025-12-08 ⭐ BREAKING

### Changed
- **BREAKING**: Renamed all "categories" to "tabs" throughout codebase ([#21](https://github.com/thejaustin/AutoCat/issues/21))
  - Database entities: `CustomCategory` → `CustomTab`
  - Database entities: `AppCategory` → `AppTab`
  - Database tables: `custom_categories` → `custom_tabs`, `app_categories` → `app_tabs`
  - All UI terminology updated
  - Navigation routes renamed
  - Preferences updated
  - DAO methods renamed
  - Automatic database migration for existing users

**Rationale**: "Tabs" more intuitive for users and better reflects visual UI

**Links**: [#21](https://github.com/thejaustin/AutoCat/issues/21), Commits: 2742d3d, bc34a19, 97efba4

---

## [15.0.b1-autocat.~10] - 2025-11-27 ⭐ MAJOR

### Added
- Complete LLM logging system
  - Created `LLMLogger.kt` with structured logging (DEBUG/INFO/WARNING/ERROR)
  - In-memory buffer (200 entries)
  - Request/response tracking
  - Statistics and export capability
- Model registry system
  - Created `ModelConfig.kt` with 11+ model definitions
  - Model metadata: context windows, cost tiers, speed, quality
  - Automatic fallback selection
  - Deprecation tracking (Gemini 1.5 → 2.0 migration)
- True batch processing for LLMs
  - Created `BatchCalculator.kt` for optimal batch sizing
  - Auto-calculates batch size based on model context windows
  - Token estimation and savings calculation
  - 20x performance improvement (100 apps: 6.7min → 20sec)
  - 68% token savings
- Dual folder sync system
  - Created `CategoryFolderSyncService.kt` for folder management
  - Drawer folders (caddy implementation)
  - Home screen folders (Launcher3 workspace)
  - Three sync modes: DRAWER/HOME_SCREEN/BOTH
  - Automatic sync after categorization

### Changed
- Updated all 4 LLM providers with `getCurrentModel()` method
- Added batch API support to all providers
- Enhanced connection testing
- Comprehensive logging integration

**Links**: Source: AUTOCAT_COMPLETE_HISTORY.md

---

## [15.0.b1-autocat.6] - 2025-11-23

### Added
- AutoCat versioning system (format: `15.0.b1-autocat.{BUILD_NUMBER}`)
- Permanent release strategy (no deletions)
- Automatic APK builds via GitHub Actions

**Links**: Commit: 7ecca46

---

## [15.0.b1-autocat.5] - 2025-11-23

### Added
- Development logs system (`/dev-logs/` directory)
- Session-based conversation logging for full transparency

**Links**: Commit: b8aae91

---

## [15.0.b1-autocat.4] - 2025-11-23

### Added
- Automatic APK releases via GitHub Actions
- `dev-latest` tag for continuous deployment

**Links**: Commit: d59a1a0

---

## [15.0.b1-autocat.3] - 2025-11-23

### Changed
- Updated version format to include beta number (15.0.b1, 15.0.b2, etc.)
- Proper semantic versioning alignment with Lawnchair

**Links**: Commit: 3fd9692

---

## [15.0.b1-autocat.2] - 2025-11-23

### Added
- Rebranded to AutoCat (maintaining Lawnchair attribution)
- Updated README with project documentation

**Links**: Commit: f5d06d9

---

## [15.0.b1-autocat.1] - 2025-11-23 🎬 INITIAL RELEASE

### Added
- Room database foundation for app categorization
  - Created `CategoryDatabase.kt` - Room database singleton
  - Created `CategoryDao.kt` - Database access object with 30+ operations
  - Created `AppCategory.kt` entity - App→category mappings with confidence scoring
  - Created `CustomCategory.kt` entity - User-defined categories
  - 7 default categories (Games, Social, Productivity, Tools, Entertainment, Photography, Communication)
  - Confidence scoring system (0.0-1.0)
  - User override support to prevent auto-recategorization
  - Source tracking (built-in/rule/ML/user)

**Database Schema:**
```kotlin
@Entity CustomCategory(id, name, colorHex, isVisible, sortOrder)
@Entity AppCategory(packageName, category, confidence, source, isUserOverride)
```

**Links**: Session 1 log (dev-logs/2025-11-23-session-1.md), Commit: 6eff9b0

---

## Categories

### Added
New features, functionality, or capabilities.

### Changed
Changes to existing functionality or behavior.

### Deprecated
Soon-to-be removed features (none currently).

### Removed
Removed features or functionality (none currently).

### Fixed
Bug fixes and error corrections.

### Security
Security-related changes (none currently).

---

*Last Updated: 2025-12-25*
