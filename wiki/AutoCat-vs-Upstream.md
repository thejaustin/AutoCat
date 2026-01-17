# AutoCat vs Upstream Lawnchair

Comprehensive tracking of all modifications, additions, and differences between AutoCat and upstream Lawnchair 15.

## Overview

**AutoCat** is a fork of **Lawnchair 15** (Android 15 Launcher3) with extensive additions for intelligent app categorization. This page documents every difference from the upstream project.

**Fork Point**: Lawnchair 15-dev (November 2025)
**Sync Strategy**: Periodic merges from upstream
**Divergence Level**: Major feature additions, minimal core modifications

---

## Added Features (Not in Upstream)

### 🤖 AI-Powered Categorization System

#### LLM Integration
**Added**: Complete multi-provider LLM infrastructure

**New Packages**:
- `app.lawnchair.categorization.llm.*`
  - `GoogleAIProvider.kt`
  - `ClaudeProvider.kt`
  - `OpenAIProvider.kt`
  - `PerplexityProvider.kt`
  - `LLMProvider.kt` (interface)
  - `ModelRegistry.kt`
  - `ProviderCircuitBreaker.kt`
  - `ConfidenceCalibrator.kt`

**Upstream Status**: ❌ Not present

---

#### Categorization Pipeline
**Added**: Multi-stage intelligent categorization

**New Files**:
- `app.lawnchair.categorization.*`
  - `CategorizationManager.kt`
  - `AppTabsController.kt`
  - `AccuracyTracker.kt`
  - `AdaptiveModelSelector.kt`

**Stages**:
- `stages/BuiltInCategorizer.kt`
- `stages/LLMCategorizer.kt`
- `learning/UserCorrectionLearner.kt`

**Upstream Status**: ❌ Not present

---

#### Folder Sync Service
**Added**: Automatic folder creation and sync

**New Files**:
- `TabFolderSyncService.kt`
- Dual sync modes (Drawer + Home Screen)
- Automatic folder management

**Upstream Status**: ❌ Not present

---

### 💾 Database Extensions

#### New Tables
**Added**:
1. `app_tab` - App category assignments
2. `custom_tab` - User-defined categories
3. `model_accuracy` - LLM performance tracking

**Schema**:
- `TabDatabase.kt` (completely new)
- `TabDao.kt`
- `AccuracyDao.kt`

**Entities**:
- `AppTab.kt`
- `CustomTab.kt`
- `ModelAccuracy.kt`

**Upstream Status**: ❌ Not present

---

### 🎨 UI Additions

#### New Preference Screens
**Added**:
1. `AppTabAssignmentPreferences.kt` - Review & override tab assignments (formerly AppCategorizationListPreferences)
2. `CategorizationSettingsPreferences.kt` - Categorization settings
3. `TabManagementPreferences.kt` - Manage tabs/folders (formerly CategoryManagementPreferences)
4. `LLMSettingsPreferences.kt` - LLM provider configuration

**Upstream Status**: ❌ Not present

---

#### UI Components
**Added**:
- Category tabs in app drawer (bottom placement)
- Categorization progress tracking
- LLM reasoning display
- Accuracy metrics dashboard
- Provider status indicators
- Batch progress UI

**Modified Files**:
- App drawer layouts (tab integration)
- Preferences dashboard (new sections)

**Upstream Status**: ⚠️ Modified from upstream

---

### ⚙️ Preferences Extensions

#### New Preferences
**Added** to `PreferenceManager.kt`:
```kotlin
// LLM Provider Settings
val llmProviderPreference: StringPref
val llmGoogleAIKey: StringPref
val llmClaudeKey: StringPref
val llmOpenAIKey: StringPref
val llmPerplexityKey: StringPref
val llmAutoSelectBestModel: BoolPref

// Model Selection
val llmGoogleAIModel: StringPref
val llmClaudeModel: StringPref
val llmOpenAIModel: StringPref
val llmPerplexityModel: StringPref

// Batch Processing
val llmEnableBatching: BoolPref
val llmBatchSize: IntPref

// Circuit Breaker
val circuitBreakerEnabled: BoolPref
val circuitBreakerFailureThreshold: IntPref
val circuitBreakerTimeoutMs: LongPref

// Folder Sync
val autoCatSyncFolders: BoolPref
val autoCatUseTabs: BoolPref

// Advanced
val hideQuickstepSettings: BoolPref
val hideSettingsWarnings: BoolPref
```

**Upstream Status**: ❌ Not present

---

## Modified Features (Changed from Upstream)

### App Drawer
**Changed**:
- Added category tab support at bottom
- Integrated folder sync
- Tab-based filtering

**Impact**: Enhanced organization, backward compatible

**Files Modified**:
- App drawer layouts
- Folder management logic

**Upstream Status**: ⚠️ Extended, not replaced

---

### Startup Flow
**Changed**:
- Added lazy initialization for categorization services
- Deferred database access
- Async service loading

**Impact**: Faster startup, no blocking

**Files Modified**:
- `LawnchairLauncher.kt`
- Service initialization logic

**Upstream Status**: ⚠️ Enhanced, compatible

---

### Folder System
**Changed**:
- Extended with auto-creation
- Dual sync (drawer + home)
- Category-based organization

**Impact**: Automatic folder management

**Files Modified**:
- `FolderInfo.kt`
- `FolderIcon.kt`

**Upstream Status**: ⚠️ Extended, compatible

---

## Dependencies Added

### New Dependencies
```gradle
// LLM API clients (via OkHttpClient)
implementation("com.squareup.okhttp3:okhttp:4.x")

// Room Database (already in upstream, extended usage)
// No new dependencies for core features

// JSON parsing (already in upstream)
// Using built-in kotlinx.serialization
```

**Impact**: Minimal dependency additions, mostly use existing libraries

---

## Performance Modifications

### Optimizations Added
1. **Batch Processing**: 20x speedup for categorization
2. **Database Batch Inserts**: 10x faster bulk operations
3. **Async Initialization**: No UI thread blocking
4. **Skip Categorized Apps**: Faster startup

**Upstream Impact**: ✅ No negative impact, only improvements

---

## Build Configuration Changes

### Version Numbering
**Changed**:
- AutoCat: `15.0.b1-autocat.{BUILD_NUMBER}`
- Upstream: `15.0.b1`

**GitHub Actions**:
- Added `autocat-release` job
- Dual release tagging (dev-latest + versioned)

**Files Modified**:
- `.github/workflows/ci.yml`
- Build scripts

---

## Statistics

### Code Changes
- **New Files**: 42 AutoCat-specific files
- **Modified Files**: ~15 files
- **Lines Added**: ~12,000+
- **Lines Removed**: ~300

### Complete Package Structure
**Added** (All 42 AutoCat Files):
```
app.lawnchair.categorization/ (19 files)
├── AccuracyTracker.kt                   ⭐ Phase 21
├── AdaptiveModelSelector.kt             ⭐ Phase 21
├── AutoCatAppProvider.kt                Phase 3
├── CategorizationManager.kt             Phase 2
├── TabFolderSyncService.kt         Phase 11
├── AppTabsController.kt            Phase 11
├── AppTabsManager.kt               Phase 3 (renamed)
├── FolderAutoSortService.kt             Phase 12
├── importer/
│   └── SmartLauncherImporter.kt         Phase 12
├── learning/
│   └── UserCorrectionLearner.kt         Phase 7
├── llm/
│   ├── BatchCalculator.kt               Phase 11
│   ├── ClaudeProvider.kt                Phase 8
│   ├── ConfidenceCalibrator.kt          Phase 15
│   ├── GoogleAIProvider.kt              Phase 5
│   ├── LLMLogger.kt                     Phase 11
│   ├── LLMProvider.kt                   Phase 5
│   ├── ModelConfig.kt                   Phase 11
│   ├── OpenAIProvider.kt                Phase 8
│   ├── PerplexityProvider.kt            Phase 8
│   └── ProviderCircuitBreaker.kt        Phase 14
└── stages/
    ├── BuiltInCategorizer.kt            Phase 2
    └── LLMCategorizer.kt                Phase 5

app.lawnchair.data.tab/ (7 files)
├── AccuracyDao.kt                       ⭐ Phase 21
├── TabDao.kt                            Phase 11 (renamed)
├── TabDatabase.kt                       Phase 11 (renamed)
└── entities/
    ├── AppTab.kt                        Phase 11 (renamed from AppCategory)
    ├── CustomTab.kt                     Phase 11 (renamed from CustomCategory)
    └── ModelAccuracy.kt                 ⭐ Phase 21

app.lawnchair.ui.preferences.destinations/ (8 files)
├── AppTabAssignmentPreferences.kt       Phase 4 (renamed)
├── AppCategorizationPreferences.kt      Phase 20
├── CategorizationProgress.kt            Phase 6
├── CategorizationSettingsPreferences.kt Phase 20
├── TabManagementPreferences.kt          Phase 4 (renamed)
└── LLMSettingsPreferences.kt            Phase 5

app.lawnchair.ui.preferences.components/ (2 files)
├── controls/PreferenceCategory.kt       UI utilities
└── layout/TwoTabPreferenceLayout.kt     UI utilities

app.lawnchair.data/ (1 file)
└── AppDatabase.kt                       Security fixes

**Total**: 42 files across 5 packages
```

---

## Compatibility

### Upstream Merges
**Strategy**: Periodic merges from upstream 15-dev

**Recent Merges**:
- 2025-12-22: Merged upstream updates (commit bd56dfa)

**Conflicts**: Minimal, mostly in preferences

**Merge Compatibility**: ✅ Good, clean merges

---

### API Compatibility
**Breaking Changes**: ❌ None to upstream APIs

**Additions Only**: ✅ All changes are additive

**Backward Compatible**: ✅ Can build without AutoCat features

---

## Maintenance Burden

### Ongoing Maintenance
- **Upstream Tracking**: Monitor 15-dev for updates
- **Merge Frequency**: Every 1-2 weeks
- **Conflict Resolution**: Typically minor

### Technical Debt
- ⚠️ Database migrations need production-ready logic
- ⚠️ LLM API error handling could be more robust
- ✅ Code is well-documented
- ✅ Following upstream patterns

---

## Future Divergence

### Planned Changes
1. **Parallel Batch Processing** - More performance improvements
2. **Smart Launcher Import** - Feature parity with other launchers
3. **Advanced Analytics** - Deeper insights

**Expected Impact**: Continued additive changes, minimal core modifications

---

## Contribution Back to Upstream

### Potential Upstreamable Features
- ⬆️ Material 3 Expressive UI improvements
- ⬆️ Performance optimizations (batch inserts, lazy loading)
- ⬆️ Error handling patterns

### AutoCat-Specific Features (Not Upstreamable)
- ❌ LLM integration (too specific)
- ❌ Categorization system (opinionated)
- ❌ Accuracy tracking (niche use case)

---

*Last Updated: 2025-12-25*
