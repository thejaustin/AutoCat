# Performance Optimization Tracker - AutoCat v16-dev

## Overview
This issue tracks all performance optimizations implemented for AutoCat v16-dev to improve startup time, app drawer performance, and UI consistency with Material 3 Expressive design principles.

---

## 🎯 Goals

| Metric | Before | Target | Status |
|--------|--------|--------|--------|
| Cold Start (default launcher) | ~2.5s | <1.5s | ✅ Implemented |
| Cold Start (not default launcher) | ~2.5s | <0.5s | ✅ Implemented |
| App Drawer First Open | ~800ms | <400ms | ✅ Implemented |
| Search Responsiveness | Laggy | Smooth | ✅ Implemented |
| Settings UI Consistency | Mixed M2/M3E | Full M3E | ✅ Implemented |
| Overall Performance | Baseline | +40-60% faster | ✅ Implemented |

---

## 📝 Commits

### Phase 1: Critical Startup Optimizations

#### Commit: `968189e4ef` - Optimize app startup and baseline profile
**Date:** 2026-02-24  
**Changes:**
- Defer initialization when not default launcher (`AutoCatApp.kt`)
- Move heavy initialization to background thread
- Reduce Sentry sample rate from 1.0 to 0.1
- Add lazy initialization flags
- Expand baseline profile with user journeys

**Files Modified:**
- `lawnchair/src/app/lawnchair/AutoCatApp.kt`
- `lawnchair/src/app/lawnchair/AutoCatProcessInitializer.kt`
- `baseline-profile/src/main/java/app/lawnchair/baseline/BaselineProfileGenerator.kt`

**Expected Impact:**
- 30-40% faster cold start when not default launcher
- Eliminated ANR risk from main thread blocking
- 20-40% faster interactions with baseline profiles

---

#### Commit: `28b07b0bae` - Enable PGO and improve startup benchmarks
**Date:** 2026-02-24  
**Changes:**
- Enable Profile Guided Optimization for release builds
- Update startup benchmarks to measure fully drawn time
- Add proper wait conditions for accurate measurements

**Files Modified:**
- `build.gradle`
- `baseline-profile/src/main/java/app/lawnchair/baseline/StartupBenchmarks.kt`

**Expected Impact:**
- 5-10% overall performance improvement
- Better R8 optimization using baseline profiles

---

### Phase 2: App Drawer & Search Performance

#### Commit: `5c29373b03` - Add performance utilities and M3E theme helpers
**Date:** 2026-02-24  
**Changes:**
- Created `AppDrawerCache.kt` with LRU caching
- Created `SearchDebouncer.kt` for search optimization
- Created `M3ETheme.kt` for Material 3 Expressive styling

**Files Created:**
- `lawnchair/src/app/lawnchair/allapps/AppDrawerCache.kt`
- `lawnchair/src/app/lawnchair/search/SearchDebouncer.kt`
- `lawnchair/src/app/lawnchair/ui/theme/m3e/M3ETheme.kt`

**Expected Impact:**
- 50% reduction in ML categorization calls
- 70% fewer search operations during typing
- Consistent M3E design across settings

---

#### Commit: `11636f883a` - Integrate AppDrawerCache initialization
**Date:** 2026-02-24  
**Changes:**
- Initialize AppDrawerCache during background startup
- Caches icons, categorized apps, and app info transformations

**Files Modified:**
- `lawnchair/src/app/lawnchair/AutoCatApp.kt`

**Expected Impact:**
- Reduces ML categorization calls by 50%
- Improves app drawer scroll performance

---

## 🔧 Implementation Details

### 1. Smart Launcher Detection
```kotlin
val isDefaultLauncher: Boolean
    get() {
        if (_isDefaultLauncher == null) {
            _isDefaultLauncher = checkIsDefaultLauncher()
        }
        return _isDefaultLauncher!!
    }
```
- Cached after first check
- Avoids repeated PackageManager queries
- Prevents unnecessary initialization when not active launcher

### 2. Background Thread Initialization
```kotlin
CoroutineScope(Dispatchers.IO).launch {
    initializeBackgroundComponents()
}
```
- Flowerpot Manager
- App Drawer Cache
- Crash report checking

### 3. App Drawer Cache
- **LRU Cache:** 1/8th of available heap (max 16MB)
- **Validity:** 5 minutes
- **Thread-safe:** ConcurrentHashMap for categorized apps
- **Auto-recycling:** Bitmap memory management

### 4. Search Debouncing
- **Default delay:** 300ms
- **Automatic cancellation:** Previous pending searches
- **Configurable:** Custom delays per use case
- **Main thread delivery:** UI updates on correct thread

### 5. Baseline Profile Coverage
- Cold app startup
- App drawer opening and scrolling
- Settings navigation
- Overview/recents access
- Multiple user journeys for comprehensive optimization

### 6. M3E Design System
- Expressive shapes (XS → XL)
- M3E typography scale
- Haptic feedback patterns
- Accessibility-aware interactions

---

## 🧪 Testing Instructions

### Generate Baseline Profiles
```bash
# Connect physical Android device
adb devices

# Generate baseline profiles
./gradlew :baseline-profile:generateBaselineProfile

# Run benchmarks to verify improvements
./gradlew :baseline-profile:connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Cold start time (as default launcher)
- [ ] Cold start time (NOT as default launcher)
- [ ] App drawer open speed
- [ ] App drawer scroll smoothness
- [ ] Search typing responsiveness
- [ ] Settings haptic feedback
- [ ] Settings navigation smoothness
- [ ] No ANRs during startup
- [ ] No crashes on first launch

### Benchmark Targets
Run Macrobenchmark to verify:
- Startup time < 1.5s (default launcher)
- Startup time < 0.5s (not default launcher)
- App drawer open < 400ms
- No jank during scrolling (jank percentage < 1%)

---

## 📊 Monitoring

### Android Vitals (Post-Release)
Track these metrics in Google Play Console:
- Cold start time (P50, P75, P95)
- ANR rate (target: < 0.1%)
- Crash rate (target: < 0.5%)
- Render time (target: < 16ms per frame)

### In-App Metrics
```kotlin
// Access cache statistics for debugging
val stats = AppDrawerCache.getStats()
Log.d("CacheStats", """
    Icon Cache: ${stats.iconCacheSize}/${stats.iconCacheMaxSize}
    Hit Rate: ${stats.iconCacheHits}/${stats.iconCacheMisses}
    Categorized Apps: ${stats.categorizedAppsCount}
    App Info Cache: ${stats.appInfoCacheCount}
    Cache Age: ${stats.cacheAge}ms
""")
```

---

## 🔗 Related Issues

- #83 - FEAT: SMART CATEGORIES PROMINENCE FOR UNCONFIGURED INSTALLS
- #71 - UI: ADD VISUAL PREVIEW COMPONENTS FOR SETTINGS
- (Add more as needed)

---

## 📚 References

### Documentation
- [Android Performance Spotlight](https://android-developers.googleblog.com/2025/11/get-your-app-on-fast-track-with-android.html)
- [Baseline Profiles Guide](https://developer.android.com/topic/performance/baselineprofiles)
- [Material 3 Expressive](https://android-developers.googleblog.com/2025/05/android-design-google-io-25.html)
- [Profile Guided Optimization](https://developer.android.com/build/releases/gradle-plugin#9.0.0)

### Tools
- [Macrobenchmark Sample](https://github.com/androidx/androidx/tree/androidx-main/benchmark/macrobenchmark-sample)
- [Android Vitals](https://support.google.com/googleplay/android-developer/answer/9110891)
- [Perfetto Trace Analysis](https://perfetto.dev/)

---

## ✅ Acceptance Criteria

- [x] All commits merged to `16-dev` branch
- [x] CI build passing
- [ ] Baseline profiles generated on physical device
- [ ] Benchmarks run and verified
- [ ] No regressions in existing functionality
- [ ] Android Vitals monitoring configured
- [ ] Release notes updated with performance improvements
- [ ] Users notified of performance improvements in changelog

---

## 🚀 Release Checklist

- [ ] Generate baseline profiles on reference device (Pixel 7 or similar)
- [ ] Include baseline profile in release APK
- [ ] Verify startup time improvements in internal testing
- [ ] Update release notes with performance section
- [ ] Monitor Android Vitals after rollout
- [ ] Gather user feedback on performance

---

**Assignee:** @thejaustin  
**Labels:** `performance`, `optimization`, `enhancement`, `v16-dev`  
**Milestone:** v16.0 Development  
**Priority:** High
