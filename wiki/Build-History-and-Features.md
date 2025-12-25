# Build History & Features

Complete history of AutoCat builds with features, fixes, and links to related issues and commits.

## Latest Build

### Build autocat.100+ (2025-12-25) 🎉

**Milestone**: Phase 2 Completion - Adaptive Model Selection

**New Features:**
- ✨ **Model Accuracy Tracking** - Tracks LLM provider performance based on user corrections
  - Database: `ModelAccuracy` entity & `AccuracyDao`
  - Service: `AccuracyTracker` for recording prediction outcomes
  - UI: Performance metrics display with color-coded ratings

- ✨ **Adaptive Model Auto-Selection** - Automatically uses best performing provider
  - Service: `AdaptiveModelSelector` analyzes 30-day accuracy
  - Smart fallback to manual preference when insufficient data
  - Visual indicators: "⚡ ACTIVE" badge on selected model
  - Auto-select toggle in Provider Selection settings

**UI Enhancements:**
- Visual performance metrics in LLM Settings
- Color-coded accuracy ratings (Excellent/Good/Fair)
- Active model highlighting with primary color border
- Auto-selection status display

**Technical Details:**
- Database version: 5 → 6
- New table: `model_accuracy`
- Minimum samples: 10 predictions
- Minimum accuracy threshold: 70%
- Lookback period: 30 days

**Links:**
- Issue: [#18](https://github.com/thejaustin/AutoCat/issues/18) ✅ CLOSED
- Commits: [432c961](https://github.com/thejaustin/AutoCat/commit/432c961264), [661a418](https://github.com/thejaustin/AutoCat/commit/661a418408), [ac636fa](https://github.com/thejaustin/AutoCat/commit/ac636faaf3), [e15734e](https://github.com/thejaustin/AutoCat/commit/e15734e5e8), [0bfbc5a](https://github.com/thejaustin/AutoCat/commit/0bfbc5ab8c)
- CI Build: [20510081504](https://github.com/thejaustin/AutoCat/actions/runs/20510081504) ✅

---

## Recent Builds

### Build autocat.99 (2025-12-24)

**Features:**
- 🎨 **Material 3 Expressive Design** - Applied to LLM Settings and Categorization Settings
  - Enhanced visual hierarchy
  - Improved spacing and animations
  - Better user experience

**Links:**
- Commits: [c46a4f1](https://github.com/thejaustin/AutoCat/commit/c46a4f1abd), [71cc812](https://github.com/thejaustin/AutoCat/commit/71cc812fd1)

---

### Build autocat.98 (2025-12-24)

**Optimizations:**
- ⚡ **Batch Database Inserts** in BuiltInCategorizer - Improved performance

**Links:**
- Commit: [5b76004](https://github.com/thejaustin/AutoCat/commit/5b7600496b)

---

### Build autocat.97 (2025-12-23)

**Features:**
- 🎨 **Complete Material 3 Expressive Redesign**
  - Category Management screens
  - App Categorization UI
  - Enhanced visual feedback

**Links:**
- Commits: [5a26db9](https://github.com/thejaustin/AutoCat/commit/5a26db9d0b), [fe52c4a](https://github.com/thejaustin/AutoCat/commit/fe52c4ae12)

---

### Build autocat.96 (2025-12-22)

**Infrastructure:**
- 🔄 **Upstream Merge** - Merged latest Lawnchair 15-dev updates
- 🐛 **Fix**: Added missing Flowerpot import after merge

**Links:**
- Commits: [bd56dfa](https://github.com/thejaustin/AutoCat/commit/bd56dfab00), [e4776fb](https://github.com/thejaustin/AutoCat/commit/e4776fbbbf)

---

### Build autocat.95 (2025-12-21)

**Performance:**
- ⚡ **Startup Optimization** - Skip already-categorized apps for faster startup

**Bug Fixes:**
- 🐛 Fixed background usage in reasoning section

**Links:**
- Commits: [71f5747](https://github.com/thejaustin/AutoCat/commit/71f5747fd9), [709e674](https://github.com/thejaustin/AutoCat/commit/709e674fd9)

---

### Build autocat.94 (2025-12-20)

**Refactoring:**
- 🔧 **Migration to OkHttpClient** - Replaced HttpURLConnection with OkHttpClient for LLM API calls

**Links:**
- Commit: [2e361aa](https://github.com/thejaustin/AutoCat/commit/2e361aa27d)
- Related Issue: Improved reliability and performance

---

### Build autocat.93 (2025-12-17)

**Critical Bug Fixes:**
- 🐛 **Fix App Categorization Screen Freeze/Crash** (#31)
  - Moved heavy service initialization off UI thread
  - Fixed 4 preference screens
  - Added comprehensive error handling
  - Made all service operations null-safe

- 🐛 **Fix Database Schema Mismatch** (#35)
  - Incremented TabDatabase version (4 → 5)
  - Added llm_provider and llm_model fields

- 🐛 **Fix Environment Variable Support** (#34)
  - All LLM providers now support env vars
  - Priority: Constructor → Preference → Environment Variable

**Links:**
- Issues: [#31](https://github.com/thejaustin/AutoCat/issues/31), [#34](https://github.com/thejaustin/AutoCat/issues/34), [#35](https://github.com/thejaustin/AutoCat/issues/35) ✅ CLOSED
- Commits: [303db41](https://github.com/thejaustin/AutoCat/commit/303db413be), [36b128a](https://github.com/thejaustin/AutoCat/commit/36b128af87), [546714d](https://github.com/thejaustin/AutoCat/commit/546714dcb6), [53a1564](https://github.com/thejaustin/AutoCat/commit/53a1564cff)

---

### Build autocat.92 (2025-12-16)

**Features:**
- 🛡️ **Circuit Breaker for LLM Providers** (#30)
  - Automatic failover on provider failures
  - Configurable thresholds and timeouts
  - UI controls in LLM Settings

- 📊 **Confidence Score Calibration** (#30)
  - Improved categorization accuracy
  - Provider-specific calibration factors
  - Model tracking in AppTab entities

- ⚙️ **Advanced Settings Toggles** (#29)
  - Hide Quickstep settings option
  - Hide settings warnings option

**Links:**
- Issues: [#29](https://github.com/thejaustin/AutoCat/issues/29), [#30](https://github.com/thejaustin/AutoCat/issues/30) ✅ CLOSED
- Commits: [952d644](https://github.com/thejaustin/AutoCat/commit/952d644dcd), [1ef1bd5](https://github.com/thejaustin/AutoCat/commit/1ef1bd5b13), [bfe2661](https://github.com/thejaustin/AutoCat/commit/bfe2661b9c), [6962f70](https://github.com/thejaustin/AutoCat/commit/6962f70ec5)

---

### Build autocat.91 (2025-12-15)

**Performance Fixes:**
- 🐛 **Fix UI Blocking Issues**
  - Removed runBlocking from LawnchairShortcut
  - Fixed cache race condition in AutoCatAppProvider
  - Moved blocking calls to Dispatchers.IO

**Links:**
- Commits: [048bd16](https://github.com/thejaustin/AutoCat/commit/048bd16daa), [0888f1e](https://github.com/thejaustin/AutoCat/commit/0888f1e0d3)

---

### Build autocat.90 (2025-12-14)

**UI Improvements:**
- 📱 **Category Tabs at Bottom** (#23)
  - Moved category tabs to bottom of app drawer
  - Better thumb reachability
  - Improved UX on large screens

**Links:**
- Issue: [#23](https://github.com/thejaustin/AutoCat/issues/23) ✅ CLOSED
- Commit: [53d4528](https://github.com/thejaustin/AutoCat/commit/53d452f892)

---

## Feature Categories

### 🤖 LLM & AI Features
- Multi-provider support (Google AI, Claude, OpenAI, Perplexity)
- Batch processing (20x faster)
- Confidence calibration
- Circuit breaker
- Accuracy tracking
- Adaptive model selection

### 📊 Database & Storage
- Room database foundation
- AppTab entity with provider/model tracking
- ModelAccuracy tracking table
- Custom categories/tabs
- Folder assignments

### 🎨 UI/UX
- Material 3 Expressive design
- Category tabs at bottom
- Performance metrics display
- Progress tracking
- Error handling & logging

### ⚡ Performance
- Batch database inserts
- Startup optimization
- Async service initialization
- Parallel processing support

### 🐛 Bug Fixes
- Screen freeze/crash fixes
- Database schema migrations
- Environment variable support
- Icon loading issues
- Startup crashes

---

## Build Numbering

AutoCat uses the versioning scheme: `15.0.b1-autocat.{BUILD_NUMBER}`

- `15.0.b1` = Lawnchair 15.0 Beta 1 (base version)
- `autocat.X` = AutoCat build number

Each push to `15-dev` creates a new build number.

---

*Last Updated: 2025-12-25*
