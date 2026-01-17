# AutoCat User Guide

Complete guide to downloading, installing, and using AutoCat.

## 📥 Installation

### Download Latest Build

Every push to `15-dev` automatically builds a new APK:

**[Download Latest Build](https://github.com/thejaustin/AutoCat/releases/tag/dev-latest)**

### Installation Steps

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

---

## 🎯 Core Features

### AI-Powered App Categorization

AutoCat automatically categorizes your apps using multiple LLM providers:

- **Google AI** (Gemini 2.0 Flash)
- **Claude** (3.5 Haiku/Sonnet)
- **OpenAI** (GPT-4o Mini)
- **Perplexity** (Llama 3.1 Sonar)

### App Tabs

Apps are organized into tabs at the bottom of the app drawer:

- **Default Tabs**: Games, Social, Productivity, Tools, Entertainment, Photography, Communication
- **Custom Tabs**: Create your own tabs
- **Batch Processing**: Assign 100 apps to tabs in ~20 seconds

### Folder Sync

AI assignments automatically create persistent folders:

- **Drawer Folders**: Organized app drawer
- **Home Screen Folders**: Optional home screen organization
- **Auto-Sync**: Folders update when tab assignments change

### Accuracy Tracking

AutoCat learns from your corrections:

- **Performance Metrics**: See which LLM provider works best for your apps
- **Auto-Select Best Model**: Automatically uses the most accurate provider
- **30-Day Analysis**: Continuous learning and improvement

---

## ⚙️ Settings & Configuration

### LLM Provider Setup

**Location**: Settings → App Categorization → LLM Settings

1. **Choose Provider**: Google AI, Claude, OpenAI, or Perplexity
2. **Add API Key**: Enter your API key (or set environment variable)
3. **Select Model**: Choose specific model variant
4. **Test Connection**: Verify API key works

**Environment Variables** (optional):
- `GOOGLE_AI_API_KEY`
- `ANTHROPIC_API_KEY`
- `OPENAI_API_KEY`
- `PERPLEXITY_API_KEY`

### Categorization Settings

**Location**: Settings → App Categorization

- **Auto-Select Best Model**: Let AutoCat choose the most accurate provider
- **Batch Processing**: Enable faster assignment (recommended)
- **Folder Sync**: Auto-create folders from tabs
- **Use Tabs**: Show tabs in app drawer

### Tab Management

**Location**: Settings → App Categorization → Manage Tabs

- **Create Custom Tabs**: Add your own tabs
- **Edit Tab Names**: Rename existing tabs
- **Set Colors**: Customize tab colors
- **Reorder Tabs**: Drag to reorder tabs

---

## 🚀 Using AutoCat

### First-Time Setup

1. **Install AutoCat** (see Installation above)
2. **Configure LLM Provider**:
   - Go to Settings → App Categorization → LLM Settings
   - Choose a provider (Google AI recommended for free tier)
   - Add your API key
   - Test connection
3. **Run Initial Assignment**:
   - Settings → App Categorization → "Re-categorize All Apps"
   - Wait for assignment to complete (~20 seconds for 100 apps)
4. **Review Results**:
   - Settings → App Categorization → "Review & Override"
   - Correct any misclassified apps

### Daily Usage

**App Drawer**:
- Swipe up from home screen to open app drawer
- Tap tabs at bottom to filter apps
- All apps shown by default (no tab selected)

**Correcting Assignments**:
- Settings → App Categorization → "Review & Override"
- Tap app → Select correct tab
- AutoCat learns from your corrections

**Adding New Apps**:
- New apps automatically assigned to tabs on install
- Manual assignment: Settings → App Categorization → "Re-categorize All Apps"

---

## 📊 Performance Metrics

**Location**: Settings → Categorization → LLM Settings

View accuracy metrics for each LLM provider:

- **Accuracy Percentage**: Based on your correction history
- **Total Predictions**: Number of apps categorized
- **Color-Coded Ratings**:
  - Green (≥90%): Excellent
  - Yellow (70-89%): Good
  - Red (<70%): Needs improvement
- **Active Model Badge**: Shows which provider is currently selected

---

## 🔧 Troubleshooting

### App Categorization Issues

**Problem**: Apps not categorizing

**Solutions**:
1. Check LLM provider API key is valid (Test Connection)
2. Verify you have API quota remaining
3. Check internet connection
4. Review error logs: Settings → Categorization → "View Logs"

**Problem**: Wrong categories assigned

**Solutions**:
1. Correct manually: Settings → Categorization → "Review App Categories"
2. AutoCat learns from your corrections
3. Try different LLM provider
4. Enable "Auto-Select Best Model"

### Installation Issues

**Problem**: "App not installed" error

**Solutions**:
1. Uninstall any existing AutoCat version
2. Enable Unknown Sources
3. Check storage space (need ~150MB free)
4. Download APK again (may be corrupted)

**Problem**: App crashes on launch

**Solutions**:
1. Clear app data: Settings → Apps → AutoCat → Storage → Clear Data
2. Reinstall APK
3. Report issue with logs: `adb logcat | grep AutoCat`

### Folder Sync Issues

**Problem**: Folders disappear on restart

**Solutions**:
1. Enable folder sync: Settings → App Categorization → "Auto-create folders"
2. Run categorization again
3. Verify folders in Settings → Home Screen → Folders

---

## 🎨 Customization

### Custom Tabs

1. **Create Tab**:
   - Settings → App Categorization → "Manage Tabs"
   - Tap "+" to add new tab
   - Enter name and choose color

2. **Assign Apps**:
   - Settings → App Categorization → "Review & Override"
   - Select app → Choose your custom tab

3. **Delete Tab**:
   - Settings → App Categorization → "Manage Tabs"
   - Swipe or long-press tab → Delete

### App Tab Appearance

**Tab Position**: Bottom of app drawer (fixed)

**Tab Colors**: Customizable per tab

**Tab Order**: Drag to reorder in "Manage Tabs"

**Hide/Show Tabs**: Toggle visibility in App Categorization settings

---

## 📱 Build Information

Each AutoCat release includes:

- **Commit Hash**: Identifies exact code version
- **Build Number**: Sequential build counter (e.g., autocat.100)
- **Build Date**: When the APK was built
- **Commit Message**: What changed in this build

Check version: Settings → Apps → AutoCat → Version

---

## 🔄 Version Management

### Package Names

AutoCat uses different package names to avoid conflicts:

- **GitHub/Debug builds**: `app.autocat.debug`
- **Play Store builds**: `app.autocat.play`
- **Nightly builds**: `app.autocat.nightly`

**You can install AutoCat alongside the official Lawnchair launcher.**

### Uninstalling

**Standard uninstall**:
```
Settings → Apps → AutoCat → Uninstall
```

**Via ADB** (if installation fails):
```bash
adb uninstall app.autocat.debug
```

---

## 📈 Tips & Best Practices

### Optimize Assignment Accuracy

1. **Correct Mistakes**: Always correct misassigned apps
2. **Use Auto-Select**: Enable "Auto-Select Best Model" for best results
3. **Check Accuracy Metrics**: Review provider performance regularly
4. **Batch Process**: Enable batch processing for faster assignment

### API Key Management

1. **Use Environment Variables**: More secure than storing in settings
2. **Free Tiers**: Google AI offers generous free tier
3. **Rate Limits**: Be aware of API rate limits (15 RPM for Google AI)
4. **Test Connection**: Always test after adding API key

### Folder Organization

1. **Enable Folder Sync**: Let AutoCat create folders automatically
2. **Manual Adjustments**: Edit folders manually if needed
3. **Consistent Naming**: Use clear tab names
4. **Regular Review**: Check tabs periodically

---

## 🆘 Getting Help

### Before Reporting Issues

1. Check if issue exists in latest build
2. Try clearing app data
3. Check wiki/documentation for known issues
4. Review error logs

### Reporting Issues

**GitHub Issues**: https://github.com/thejaustin/AutoCat/issues

**Include**:
- Build info: Commit hash from release (e.g., `AutoCat-debug-abc1234.apk`)
- Android version: Settings → About Phone
- Device model: Manufacturer and model name
- Steps to reproduce: Exact steps that cause the issue
- Expected vs actual: What should happen vs what actually happens
- Logs if possible: `adb logcat | grep AutoCat`

---

## ❓ FAQ

**Q: Do I need to uninstall Lawnchair to use AutoCat?**
A: No, they use different package names and can coexist.

**Q: Will my data transfer from Lawnchair?**
A: No, they're separate apps. You'll need to reconfigure AutoCat.

**Q: How often are builds updated?**
A: Every push to `15-dev`. Check commit history for frequency.

**Q: Is this stable for daily use?**
A: AutoCat is in beta. Current build (100+) is stable for daily use.

**Q: Can I use AutoCat without an API key?**
A: No, AutoCat requires an LLM provider API key for categorization.

**Q: Which LLM provider is best?**
A: Google AI (Gemini 2.0 Flash) recommended for free tier. Enable "Auto-Select Best Model" for automatic optimization.

**Q: Do I need root?**
A: No, AutoCat works on non-rooted devices. QuickSwitch features require root (same as Lawnchair).

**Q: How much does it cost?**
A: AutoCat is free. API costs depend on provider (Google AI has generous free tier).

---

*Last Updated: 2025-12-25*
