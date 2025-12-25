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

## [15.0.b1-autocat.93] - 2025-12-17

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

## [15.0.b1-autocat.92] - 2025-12-16

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
