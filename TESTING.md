# Testing AutoCat

This guide explains how to download, install, and test AutoCat development builds.

## Quick Start

### Download Latest Build

Every push to `15-dev` automatically builds a new APK:

**📥 [Download Latest Build](https://github.com/thejaustin/AutoCat/releases/tag/dev-latest)**

### Installation

1. **Download the APK**
   - Go to [Releases](https://github.com/thejaustin/AutoCat/releases)
   - Click on `dev-latest`
   - Download `AutoCat-debug-<commit>.apk`

2. **Enable Unknown Sources** (if not already enabled)
   - Android 8+: Settings → Apps → Special Access → Install Unknown Apps → Enable for your file manager/browser
   - Android 7-: Settings → Security → Unknown Sources → Enable

3. **Install**
   - Open the downloaded APK
   - Tap "Install"
   - Grant necessary permissions

4. **Set as Default Launcher**
   - Press Home button
   - Select "AutoCat"
   - Choose "Always" or "Just once" for testing

## Download Options

### Option 1: GitHub Releases (Recommended)

**Best for**: Quick testing, easy downloads

- **URL**: https://github.com/thejaustin/AutoCat/releases/tag/dev-latest
- **Updates**: Automatically on every push to `15-dev`
- **Filename**: `AutoCat-debug-<commit-hash>.apk`
- **No login required**

### Option 2: GitHub Actions Artifacts

**Best for**: Testing specific builds, accessing build logs

1. Go to [Actions tab](https://github.com/thejaustin/AutoCat/actions)
2. Click on a workflow run
3. Scroll to "Artifacts" section
4. Download "Debug APK"
5. Extract ZIP and install APK

**Note**: Requires GitHub account

### Option 3: Build Locally (Advanced)

**Only if GitHub Actions is unavailable**

```bash
# Not recommended - builds via GitHub Actions instead
# See instructions file for Termux setup if absolutely needed
```

## Build Information

Each release includes:

- **Commit Hash**: Identifies exact code version
- **Build Number**: Sequential build counter
- **Build Date**: When the APK was built
- **Commit Message**: What changed in this build

## Version Management

### Package Names

AutoCat uses different package names to avoid conflicts:

- **GitHub/Debug builds**: `app.lawnchair.debug`
- **Future release builds**: `app.lawnchair.autocat` (planned)

You can install AutoCat alongside the official Lawnchair launcher.

### Uninstalling

**Standard uninstall**:
```bash
Settings → Apps → AutoCat → Uninstall
```

**Via ADB** (if installation fails):
```bash
adb uninstall app.lawnchair.debug
```

**Via Termux** (if you have PM permission):
```bash
pm uninstall app.lawnchair.debug
```

## Testing Checklist

When testing a new build:

- [ ] APK installs successfully
- [ ] App launches without crashes
- [ ] Basic launcher functions work (app drawer, home screen)
- [ ] No visible regressions from previous build
- [ ] New features work as expected (check commit message)

## Reporting Issues

### Before Reporting

1. Check if issue exists in latest build
2. Try clearing app data: Settings → Apps → AutoCat → Storage → Clear Data
3. Check dev-logs for known issues

### What to Include

- **Build info**: Commit hash from release (e.g., `AutoCat-debug-abc1234.apk`)
- **Android version**: Settings → About Phone
- **Device model**: Manufacturer and model name
- **Steps to reproduce**: Exact steps that cause the issue
- **Expected vs actual**: What should happen vs what actually happens
- **Logs if possible**: Use `adb logcat` or a logcat app

### Where to Report

- **GitHub Issues**: https://github.com/thejaustin/AutoCat/issues
- **Dev Logs**: Check `dev-logs/` for session-specific discussions

## Build Status

Check build status: [![Build debug APK](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml/badge.svg)](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml)

- ✅ **Green**: Latest build succeeded, safe to download
- ❌ **Red**: Latest build failed, wait for fix or download previous build
- 🟡 **Yellow**: Build in progress

## Troubleshooting

### Installation Failed

**Error**: "App not installed"

**Solutions**:
1. Uninstall any existing AutoCat version
2. Enable Unknown Sources
3. Check storage space (need ~150MB free)
4. Download APK again (may be corrupted)

### App Crashes on Launch

**Solutions**:
1. Clear app data: Settings → Apps → AutoCat → Storage → Clear Data
2. Reinstall APK
3. Check logcat for crash details
4. Report issue with logs

### Can't Set as Default Launcher

**Solutions**:
1. Settings → Apps → Default Apps → Home App → Select AutoCat
2. Some launchers require disabling before switching
3. Restart device and try again

### Features Not Working

1. Check which features are implemented (see README.md development status)
2. Many features are still in development
3. Check commit message for what's new in current build

## Advanced Testing

### ADB Installation

```bash
# Install via ADB
adb install AutoCat-debug-<commit>.apk

# Install over existing (preserve data)
adb install -r AutoCat-debug-<commit>.apk

# Uninstall
adb uninstall app.lawnchair.debug

# View logs
adb logcat | grep -i "launcher\|autocat\|category"
```

### Termux Installation

```bash
# Install from Termux (if you have PM permission)
pm install /storage/emulated/0/Download/AutoCat-debug-<commit>.apk

# Grant storage permission
pm grant app.lawnchair.debug android.permission.READ_EXTERNAL_STORAGE
```

## CI/CD Pipeline

AutoCat uses GitHub Actions for continuous integration:

**Triggered by**: Push to `15-dev` branch

**Build steps**:
1. Checkout code with submodules
2. Set up Java 21
3. Build debug APK variants
4. Run unit tests
5. Upload artifacts
6. Create/update `dev-latest` release

**Build variants produced**:
- `lawnWithQuickstepGithubDebug` (published to releases)
- `lawnWithQuickstepPlayDebug`
- `lawnWithQuickstepNightlyRelease`

**Build time**: ~5-10 minutes

## FAQ

**Q: Do I need to uninstall Lawnchair to use AutoCat?**
A: No, they use different package names and can coexist.

**Q: Will my data transfer from Lawnchair?**
A: No, they're separate apps. You'll need to reconfigure AutoCat.

**Q: How often are builds updated?**
A: Every push to `15-dev`. Check commit history for frequency.

**Q: Is this stable for daily use?**
A: No, this is a development build. Use at your own risk.

**Q: Can I install this on multiple devices?**
A: Yes, download and install the same APK on all devices.

**Q: Do I need root?**
A: No, AutoCat works on non-rooted devices. QuickSwitch features require root (same as Lawnchair).

## Support

For support or questions:
- **Issues**: https://github.com/thejaustin/AutoCat/issues
- **Discussions**: https://github.com/thejaustin/AutoCat/discussions (if enabled)
- **Dev Logs**: Check `dev-logs/` for context on recent changes
