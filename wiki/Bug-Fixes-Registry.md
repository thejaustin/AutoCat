# Bug Fixes Registry

Comprehensive tracking of all bugs and their fixes with sortable data.

## Quick Stats

- **Total Bugs Fixed**: 18 (ALL closed issues)
- **Critical Fixes**: 3
- **High Priority**: 6
- **Medium Priority**: 6
- **Low Priority**: 3

---

## All Bug Fixes

| Build | Bug Description | Severity | Status | Issue | Commit | Date Fixed |
|-------|----------------|----------|--------|-------|--------|------------|
| 100+ | Missing AdaptiveModelSelector import causes build failure | High | ✅ Fixed | [#18](https://github.com/thejaustin/AutoCat/issues/18) | [e15734e](https://github.com/thejaustin/AutoCat/commit/e15734e5e8) | 2025-12-25 |
| 100+ | Spotless formatting violations in Phase 2 code | Low | ✅ Fixed | [#18](https://github.com/thejaustin/AutoCat/issues/18) | [0bfbc5a](https://github.com/thejaustin/AutoCat/commit/0bfbc5ab8c) | 2025-12-25 |
| ~80 | **SECURITY**: Prompt injection vulnerability in LLM categorization | Critical | ✅ Fixed | [#11](https://github.com/thejaustin/AutoCat/issues/11) | [06e6cdb](https://github.com/thejaustin/AutoCat/commit/06e6cdcb29) | 2025-12-10 |
| ~80 | **SECURITY**: Socket timeout not configured (hanging connections) | Critical | ✅ Fixed | [#10](https://github.com/thejaustin/AutoCat/issues/10) | Multiple | 2025-12-10 |
| ~80 | N+1 query problem in app categorization (80-90% slowdown) | High | ✅ Fixed | [#2](https://github.com/thejaustin/AutoCat/issues/2) | [fdf131b](https://github.com/thejaustin/AutoCat/commit/fdf131b77c) | 2025-12-10 |
| 96 | Missing Flowerpot import after upstream merge | Medium | ✅ Fixed | N/A | [e4776fb](https://github.com/thejaustin/AutoCat/commit/e4776fbbbf) | 2025-12-22 |
| 95 | Incorrect background() usage in reasoning section | Low | ✅ Fixed | N/A | [709e674](https://github.com/thejaustin/AutoCat/commit/709e674fd9) | 2025-12-21 |
| 95 | Background usage in AppCategorizationListPreferences | Low | ✅ Fixed | N/A | [30fb2e3](https://github.com/thejaustin/AutoCat/commit/30fb2e3987) | 2025-12-21 |
| 93 | **CRITICAL**: App categorization screen freeze/crash on open | Critical | ✅ Fixed | [#31](https://github.com/thejaustin/AutoCat/issues/31) | [303db41](https://github.com/thejaustin/AutoCat/commit/303db413be) | 2025-12-17 |
| 93 | Categorization settings screen freeze | Critical | ✅ Fixed | [#31](https://github.com/thejaustin/AutoCat/issues/31) | [36b128a](https://github.com/thejaustin/AutoCat/commit/36b128af87) | 2025-12-17 |
| 93 | Category management screen freeze | Critical | ✅ Fixed | [#31](https://github.com/thejaustin/AutoCat/issues/31) | [36b128a](https://github.com/thejaustin/AutoCat/commit/36b128af87) | 2025-12-17 |
| 93 | LLM settings screen freeze | High | ✅ Fixed | [#31](https://github.com/thejaustin/AutoCat/issues/31) | [36b128a](https://github.com/thejaustin/AutoCat/commit/36b128af87) | 2025-12-17 |
| 93 | Database schema mismatch after AppTab changes | High | ✅ Fixed | [#35](https://github.com/thejaustin/AutoCat/issues/35) | [546714d](https://github.com/thejaustin/AutoCat/commit/546714dcb6) | 2025-12-17 |
| 93 | "Get AI Suggestions" button ignores Google AI env variable | Medium | ✅ Fixed | [#34](https://github.com/thejaustin/AutoCat/issues/34) | [53a1564](https://github.com/thejaustin/AutoCat/commit/53a1564cff) | 2025-12-17 |
| 91 | UI blocking in LawnchairShortcut (runBlocking) | High | ✅ Fixed | N/A | [048bd16](https://github.com/thejaustin/AutoCat/commit/048bd16daa) | 2025-12-15 |
| 91 | Cache race condition in AutoCatAppProvider | Medium | ✅ Fixed | N/A | [048bd16](https://github.com/thejaustin/AutoCat/commit/048bd16daa) | 2025-12-15 |
| 90 | Blocking database calls on UI thread | High | ✅ Fixed | N/A | [0888f1e](https://github.com/thejaustin/AutoCat/commit/0888f1e0d3) | 2025-12-14 |
| <90 | Various startup crashes | High | ✅ Fixed | Multiple | Multiple | 2025-12-10 to 2025-12-14 |

---

## By Severity

### Critical (3)
- App categorization screen freeze/crash ([#31](https://github.com/thejaustin/AutoCat/issues/31))
- Categorization settings screen freeze ([#31](https://github.com/thejaustin/AutoCat/issues/31))
- Category management screen freeze ([#31](https://github.com/thejaustin/AutoCat/issues/31))

### High (5)
- LLM settings screen freeze ([#31](https://github.com/thejaustin/AutoCat/issues/31))
- Database schema mismatch ([#35](https://github.com/thejaustin/AutoCat/issues/35))
- Missing import causes build failure
- UI blocking in LawnchairShortcut
- Blocking database calls on UI thread

### Medium (5)
- Missing imports after merges
- Cache race conditions
- Environment variable not recognized

### Low (2+)
- Formatting violations
- Incorrect Compose API usage

---

## By Category

### 🖥️ UI/UX Bugs
- Screen freezes and crashes (4 screens affected)
- UI blocking issues
- Tab positioning issues

### 💾 Database Bugs
- Schema version mismatch
- Migration issues
- Main thread database access

### 🔧 Build/Compilation Bugs
- Missing imports
- Type mismatches
- Formatting violations

### ⚙️ Configuration Bugs
- Environment variable support
- API key detection
- Provider fallback logic

---

## Resolution Patterns

### Common Root Causes
1. **Heavy initialization on UI thread** - Fixed by moving to LaunchedEffect + Dispatchers.IO
2. **Missing database version increments** - Fixed by proper version management
3. **Import management after merges** - Fixed by careful merge conflict resolution

### Best Practices Applied
- ✅ Always use LaunchedEffect for async operations
- ✅ Increment database version on schema changes
- ✅ Null-safe service access with `?.`
- ✅ Try-catch blocks for all initialization
- ✅ Environment variable fallback for API keys

---

## Testing Impact

### Before Fixes
- App would freeze on opening categorization screens
- Database crashes on schema changes
- Build failures from missing imports

### After Fixes
- ✅ All screens load smoothly
- ✅ Database migrations handle gracefully
- ✅ Clean builds with proper imports
- ✅ Better error handling and logging

---

*Last Updated: 2025-12-25*
