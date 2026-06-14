# What to Test in Current Build

This document explains what features are in the current AutoCat build and what to look for when testing.

## Current Version: v15.0.b1-autocat.6 (Expected)

**Release Date**: November 23, 2025
**Base**: Lawnchair 15.0 Beta 1 (b1)
**AutoCat Features**: Room database foundation + versioning

---

## ⚠️ Important: No Visible UI Changes Yet

**The current build looks and behaves exactly like Lawnchair.**

Why? The Room database foundation is **backend infrastructure** - it provides data storage for future categorization features, but none of those features are implemented yet.

### What's Actually Different?

1. **Version Information**
   - Settings → Apps → AutoCat shows version `15.0-autocat.5`
   - Version code increments with each build

2. **Database Files Created**
   - AutoCat creates a database file on first launch
   - Location: `/data/data/app.lawnchair.debug/databases/category_database`
   - **You won't see this** without root or ADB access

3. **Build Configuration**
   - BuildConfig includes AutoCat-specific version fields
   - AUTOCAT_VERSION and AUTOCAT_BUILD_NUMBER constants available

### What's NOT Different (Yet)

- ❌ App drawer appearance (same as Lawnchair)
- ❌ App organization/categorization (not implemented)
- ❌ Settings UI (no AutoCat settings yet)
- ❌ Any visible branding changes (same launcher name)

---

## What You Should Test

Since visible features aren't implemented yet, focus on **stability and baseline functionality**:

### 1. Installation & Launch

**Test**:
- [ ] APK installs without errors
- [ ] App launches successfully
- [ ] No crash on first launch
- [ ] Can set as default launcher

**Expected**: Works exactly like Lawnchair

### 2. Basic Launcher Functions

**Test**:
- [ ] Home screen loads and displays icons
- [ ] App drawer opens (swipe up or app drawer button)
- [ ] Can add/remove apps from home screen
- [ ] Widgets can be added
- [ ] Folders work correctly
- [ ] Search works

**Expected**: All standard Lawnchair features work normally

### 3. Settings Access

**Test**:
- [ ] Long-press home screen → Settings
- [ ] Settings app opens
- [ ] Can navigate through all settings pages
- [ ] No new AutoCat-specific settings (not added yet)

**Expected**: Standard Lawnchair settings, no crashes

### 4. App Info & Version

**Test**:
- [ ] Go to Android Settings → Apps → AutoCat (or Lawnchair Debug)
- [ ] Check version string matches downloaded build
- [ ] Note the version format: `15.0.b1-autocat.{BUILD_NUMBER}`

**Expected**: Version shows AutoCat versioning

### 5. Performance & Stability

**Test**:
- [ ] Launcher doesn't lag or stutter
- [ ] No random crashes during normal use
- [ ] Memory usage seems normal
- [ ] Battery drain is acceptable

**Expected**: Same performance as Lawnchair (database adds minimal overhead)

---

## Advanced Testing (Optional)

### Verify Database Creation (Requires ADB)

```bash
# Check if database file exists
adb shell run-as app.lawnchair.debug ls -la databases/

# Expected output should include:
# category_database
# category_database-shm
# category_database-wal

# Inspect database schema
adb shell run-as app.lawnchair.debug sqlite3 databases/category_database ".schema"

# Expected tables:
# app_categories
# custom_categories
# room_master_table
```

### Check Build Configuration (Requires Logcat or Code Inspection)

The following constants are available in `BuildConfig`:
- `BuildConfig.AUTOCAT_VERSION` = "autocat.6"
- `BuildConfig.AUTOCAT_BUILD_NUMBER` = "6"
- `BuildConfig.VERSION_DISPLAY_NAME` = "15.0.b1-autocat.6"

### Verify Custom Categories Initialized

The database should auto-create 7 default categories on first launch:
1. Games (#4CAF50)
2. Social (#2196F3)
3. Productivity (#FF9800)
4. Tools (#9E9E9E)
5. Entertainment (#E91E63)
6. Photography (#00BCD4)
7. Communication (#3F51B5)

**Check via ADB**:
```bash
adb shell run-as app.lawnchair.debug sqlite3 databases/category_database \
  "SELECT name, color_hex, sort_order FROM custom_categories;"
```

---

## What Should NOT Work Yet

These features are **planned but not implemented**:

- ❌ Automatic app categorization (database exists, but no categorizer)
- ❌ Categorized app drawer UI (no UI changes)
- ❌ Category management settings (no settings UI)
- ❌ Manual app categorization (no UI for this)
- ❌ Custom category creation (database supports it, UI doesn't)
- ❌ ML-based categorization (not implemented)
- ❌ Rule-based categorization (not implemented)

**If you try to use these features**: Nothing will happen. They simply don't exist in the UI yet.

---

## Known Limitations

1. **No Visual Changes**: This is a foundation build - UI features come later
2. **Database Unused**: The database is created but nothing populates it yet
3. **Same Package**: Uses `app.lawnchair.debug` (will change in future releases)
4. **No Migration Path**: If you install over Lawnchair, settings won't transfer (different app)

---

## What to Report as Issues

### Report These:

✅ **Crashes or Force Closes**
- App crashes on launch
- Crashes during normal use
- Specific actions that cause crashes

✅ **Installation Problems**
- APK won't install
- Installation fails with error
- Conflicts with existing Lawnchair

✅ **Regressions from Lawnchair**
- Features that worked in Lawnchair but don't in AutoCat
- Performance degradation
- Settings that don't save

✅ **Build Issues**
- Wrong version number displayed
- Build artifacts missing
- APK corruption

### Don't Report These (Not Bugs):

❌ **Missing AutoCat Features**
- "App categorization doesn't work" (not implemented yet)
- "No AutoCat settings" (not added yet)
- "Looks exactly like Lawnchair" (correct for this build)

❌ **Upstream Lawnchair Issues**
- Bugs that also exist in Lawnchair 15.0 Beta 1
- Missing features from Lawnchair (report to Lawnchair upstream)

---

## Next Build Preview

### Coming in v15.0.b1-autocat.7+:

The next significant build will likely include one of:
- **AppMetadataProvider**: Wraps PackageManager to get app information
- **Rule-Based Categorizer**: Automatically categorizes apps by package name patterns
- **Basic Categorized UI**: Section headers in app drawer

Check `dev-logs/` for latest development decisions.

---

## Comparing Versions

### How to Compare Two AutoCat Builds

1. **Download both APKs** from different version releases
2. **Check version codes**:
   ```bash
   aapt dump badging AutoCat-v15.0.b1-autocat.5-abc1234.apk | grep versionCode
   aapt dump badging AutoCat-v15.0.b1-autocat.6-def5678.apk | grep versionCode
   ```
3. **Install newer version** (will upgrade if version code is higher)
4. **Test for regressions** (make sure old features still work)

### Version Code Scheme

- Base: `15_00_02_00` (Lawnchair 15.0 Beta 1 base)
- Build number added as last 2 digits: `15_00_02_05` for build #5

Example:
- v15.0.b1-autocat.1 → versionCode: `1500020001`
- v15.0.b1-autocat.5 → versionCode: `1500020005`
- v15.0.b1-autocat.25 → versionCode: `1500020025`

---

## Conclusion

**TL;DR for Current Build**:

✅ **Test**: Installation, launch, basic launcher functions, stability
❌ **Don't expect**: Any categorization features or UI changes
📝 **Report**: Crashes, installation issues, regressions from Lawnchair
⏭️ **Next**: Real features coming in future builds

This is a **foundation release**. The exciting stuff (auto-categorization) comes next!

---

**Questions?** Check `dev-logs/` for development context or open an issue on GitHub.
