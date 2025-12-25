# AutoCat Roadmap

Future plans and development priorities for AutoCat.

## Vision

AutoCat aims to be:

- **Intelligent**: AI-powered app organization that learns from your preferences
- **Automatic**: Minimal manual configuration, maximum convenience
- **Accurate**: Continuous learning and optimization
- **Compatible**: Seamless integration with Lawnchair features

---

## Recently Completed

### Phase 21: Intelligence & Analytics (2025-12-25) ✅

**Delivered**:
- Model accuracy tracking based on user corrections
- Adaptive model auto-selection (learns which provider works best)
- Visual performance metrics in LLM Settings
- Data-driven provider optimization

**Impact**: AutoCat now learns and improves automatically

**Links**: [Issue #18](https://github.com/thejaustin/AutoCat/issues/18), [Build 100+](Build-History-and-Features.md#build-100-phase-21-accuracy-tracking--adaptive-model-selection)

### Phase 20: Material 3 Expressive UX (2025-12-23-24) ✅

**Delivered**:
- Complete Material 3 Expressive redesign
- Enhanced visual hierarchy
- Better spacing and typography
- Modern card-based layouts

**Impact**: Beautiful, modern UI across all settings screens

### Phase 19: OkHttp Migration (2025-12-20) ✅

**Delivered**:
- Migrated from HttpURLConnection to OkHttpClient
- Connection pooling
- Better timeout handling
- Improved reliability

**Impact**: More reliable API calls, better error handling

---

## Current Focus

### Stability & Polish

**Priority**: High

**Goals**:
- Fix remaining edge cases
- Improve error messages
- Optimize performance
- Enhance documentation

**Status**: Ongoing

---

## Up Next (Q1 2026)

### Parallel Batch Processing

**Priority**: Medium

**Goal**: Further performance improvements

**Features**:
- Parallel API calls to different providers
- Smart batching across multiple models
- Reduced categorization time to <10 seconds for 100 apps

**Estimated Impact**: 2-3x faster categorization

**Links**: [Issue #19](https://github.com/thejaustin/AutoCat/issues/19)

### Per-Category Accuracy Analysis

**Priority**: Medium

**Goal**: Deeper insights into categorization quality

**Features**:
- Accuracy metrics per category (e.g., "Games" accuracy vs "Productivity")
- Identify which categories need improvement
- Category-specific model selection

**Estimated Impact**: Better categorization for difficult categories

### Smart Launcher Import Enhancements

**Priority**: Low

**Goal**: Better migration from Smart Launcher

**Current Status**: Basic .slbk import working ([Issue #28](https://github.com/thejaustin/AutoCat/issues/28))

**Planned Enhancements**:
- Import folder layouts
- Import custom categories
- Preserve icon positions
- Better conflict resolution

---

## Long-Term Goals (2026+)

### Advanced Analytics Dashboard

**Features**:
- Historical accuracy trends
- Token usage analytics
- Cost tracking per provider
- Category distribution charts
- User correction patterns

### Multi-Device Sync

**Features**:
- Sync categories across devices
- Cloud backup of categorizations
- Shared category configurations
- Family/team category sharing

### Custom Categorization Rules

**Features**:
- User-defined rules (e.g., "All Google apps → Productivity")
- Pattern matching (e.g., "*.game.* → Games")
- Domain-based rules (e.g., "social media apps → Social")
- Priority chains (rules → LLM → built-in)

### Plugin System

**Features**:
- Third-party categorization plugins
- Custom LLM provider support
- Category export/import formats
- API for external tools

---

## Upstream Compatibility

### Lawnchair 15 Tracking

**Status**: Active

**Strategy**: Periodic merges from upstream 15-dev

**Recent Merges**:
- 2025-12-22: Merged upstream updates ([Commit bd56dfa](https://github.com/thejaustin/AutoCat/commit/bd56dfab00))

**Compatibility**: ✅ Good - Clean merges, minimal conflicts

### Lawnchair 16 Migration

**Status**: Monitoring

**Timeline**: TBD (when Lawnchair 16 stable)

**Challenges**:
- Major upstream rebase to Android 16
- Potential breaking changes
- Database migration considerations

**Approach**: Wait for upstream Lawnchair 16 stabilization, then plan migration

---

## Feature Requests

### Open Issues Tracking

**Total Open Issues**: 18 ([View all](https://github.com/thejaustin/AutoCat/issues))

**High Priority**:
- [Issue #37](https://github.com/thejaustin/AutoCat/issues/37): Comprehensive Wiki & Documentation System (In Progress)

**Medium Priority**:
- Performance optimizations
- UI/UX enhancements
- Edge case fixes

**Low Priority**:
- Nice-to-have features
- Future enhancements

### Community Requests

**Most Requested**:
1. ✅ Accuracy tracking (Completed in Phase 21)
2. ✅ Auto-select best model (Completed in Phase 21)
3. 🔄 Parallel batch processing (Planned)
4. 🔄 Per-category analytics (Planned)
5. 📅 Multi-device sync (Long-term)

---

## Not Planned

### Features We Won't Implement

**Why?**: Outside AutoCat's scope or better handled by upstream Lawnchair

- **Widget Stacking**: Highly complex, Lawnchair upstream priority
- **QuickSwitch Improvements**: Upstream Lawnchair responsibility
- **Custom Gesture Handlers**: Core launcher functionality, not categorization
- **Theme Engine**: Upstream Lawnchair handles this

---

## Roadmap Principles

### 1. Accuracy First

Every feature should improve categorization accuracy or help users achieve better organization.

### 2. Performance Matters

AutoCat should be fast. No feature should degrade startup time or responsiveness.

### 3. Upstream Compatibility

Maintain clean separation from upstream code. Minimize merge conflicts.

### 4. User Privacy

No telemetry, no data collection, no cloud requirements (unless opt-in).

### 5. Open Development

All development happens in public. Community input shapes priorities.

---

## Success Metrics

### Key Performance Indicators

**Accuracy**:
- Target: >85% categorization accuracy (across all providers)
- Current: ~80-90% (varies by provider)

**Performance**:
- Target: <10 seconds for 100 apps
- Current: ~20 seconds with batch processing

**User Satisfaction**:
- Target: <5% of apps manually recategorized
- Current: ~10-15% manual corrections

**Stability**:
- Target: Zero critical bugs in latest build
- Current: ✅ Achieved (no critical bugs in build 100+)

---

## Timeline Visualization

```
2025-11 │ 🎬 Project Start
        │ └─ Fork from Lawnchair 15
        │
2025-12 │ 🏗️ Foundation Phase
   Week 1-2│ ├─ Database, Built-in categorization
   Week 2│ ├─ LLM Integration (4 providers)
   Week 3│ ├─ Performance (batch processing)
   Week 3│ ├─ UX (Material 3 Expressive)
   Week 4│ └─ Stability (bug fixes)
        │
2025-12-25│ 📊 Phase 21: Accuracy Tracking ✅
        │
2026-Q1 │ 🚀 Performance Optimizations
        │ ├─ Parallel batch processing
        │ ├─ Per-category analytics
        │ └─ Smart Launcher import enhancements
        │
2026-Q2 │ 📈 Advanced Features
        │ ├─ Analytics dashboard
        │ ├─ Custom rules engine
        │ └─ Multi-device sync (planning)
        │
2026-H2 │ 🔌 Extensibility
        │ ├─ Plugin system
        │ ├─ API for external tools
        │ └─ Advanced customization
```

---

## Upstream Lawnchair Roadmap

For Lawnchair-specific features, see:

- [Lawnchair Roadmap](https://github.com/LawnchairLauncher/lawnchair/blob/15-dev/ROADMAP.md)
- [Lawnchair Issue Tracker](https://github.com/LawnchairLauncher/lawnchair/issues)
- [Lawnchair Kanban Board](https://github.com/orgs/LawnchairLauncher/projects/9/views/1)

**AutoCat Focus**: AI categorization and organization

**Lawnchair Focus**: Core launcher features (gestures, widgets, themes)

---

## Contributing to the Roadmap

### How to Influence Priorities

1. **Open an issue**: Describe your feature request
2. **Join discussions**: Participate in issue threads
3. **Vote with reactions**: 👍 on issues you want
4. **Contribute code**: Submit PRs for features
5. **Share feedback**: Test builds and report results

### What Makes a Good Feature Request

**Good**:
- Solves a specific problem
- Benefits many users
- Aligns with AutoCat vision
- Includes use cases and examples

**Not Good**:
- Too vague or broad
- Duplicates existing features
- Better handled by other apps
- Requires upstream changes

---

## Questions?

**For roadmap questions**:
- Open a discussion on GitHub
- Comment on existing issues
- Join Telegram/Discord community

**For feature requests**:
- Create a new issue with [FEATURE] tag
- Describe use case and expected behavior
- Include mockups/examples if applicable

---

*Last Updated: 2025-12-25*

*This roadmap is a living document and subject to change based on community feedback and development priorities.*
