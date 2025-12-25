# AutoCat

> **Fork of [Lawnchair 15](https://github.com/LawnchairLauncher/lawnchair)** with intelligent app auto-categorization

[![Build debug APK](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml/badge.svg)](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml)

## About AutoCat

**AutoCat** is a personal fork of Lawnchair Launcher that adds **AI-powered automatic app categorization** to the app drawer. It uses a multi-stage intelligent pipeline with LLM integration and automatic folder sync to keep your apps organized.

### ✨ Key Features

#### 🤖 **Multi-Provider LLM Categorization**
- **4 LLM Providers**: Google AI (Gemini 2.0), Claude (Anthropic), OpenAI (GPT-4o), Perplexity (Llama 3.1)
- **Batch Processing**: 20x faster than sequential (100 apps in 20 seconds vs 6.7 minutes)
- **Auto Batch Sizing**: Optimizes batch size based on model context windows (16K-1M tokens)
- **Provider Fallback**: Automatic failover between providers for reliability
- **Model Selection**: Choose specific models per provider with deprecation handling
- **Accuracy Tracking**: Tracks model performance based on your correction history
- **Auto-Select Best Model**: Automatically uses the most accurate provider for your apps

#### 📁 **Dual Folder Sync**
- **App Drawer Folders**: Automatically creates folders in caddy-style app drawer
- **Home Screen Folders**: Optional sync to workspace folders (Android Launcher3)
- **Three Sync Modes**: DRAWER (default), HOME_SCREEN, or BOTH
- **10x Performance**: Optimized folder sync (recent improvement)

#### 🎯 **Smart Categorization Pipeline**
1. **Built-in Categories** - Android system categories (70% coverage)
2. **LLM Categorization** - AI-powered custom categories (85% accuracy)
3. **User Overrides** - Manual corrections always respected
4. **Learning System** - Learns from user corrections to improve over time

#### 🎨 **Rich User Experience**
- **Category Tabs**: Organized drawer with customizable category tabs
- **Manual Override UI**: Long-press to change app categories
- **Smart Launcher Import**: Import categories from Smart Launcher backups (.slbk files)
- **Developer Diagnostics**: Comprehensive error logging and diagnostics
- **Progress Tracking**: Real-time progress with batch information
- **App Descriptions**: Display LLM reasoning for categorization decisions

### 📊 Development Status

🚀 **Beta** - Core features complete, actively testing and refining.

**Completed Features:**
- ✅ Room database foundation
- ✅ Built-in system categorization
- ✅ LLM integration (4 providers)
- ✅ Batch API processing
- ✅ Dual folder sync (drawer + home)
- ✅ Category management UI
- ✅ User correction learning
- ✅ Smart Launcher import
- ✅ Developer diagnostics
- ✅ Comprehensive error handling
- ✅ Model accuracy tracking & analytics
- ✅ Adaptive model auto-selection

**In Progress:**
- 🔄 Compilation & integration testing
- 🔄 Performance verification

**Upcoming:**
- 🎯 Retry logic with exponential backoff
- 🎯 Parallel batch processing
- 🎯 Folder sync mode UI preference
- 🎯 Multi-language support

### 🏗️ Technical Architecture

**Tech Stack:**
- **Language**: Kotlin with Coroutines
- **Database**: Room (SQLite) for categories and folders
- **LLM Integration**: 4 providers with REST APIs
- **UI**: Android Jetpack Compose + Material Design 3
- **Base**: Lawnchair 15 (Android 15 Launcher3)

**Performance Metrics:**
- **Categorization Speed**: 20x faster with batch processing (20s vs 6.7min for 100 apps)
- **Token Efficiency**: 68% token savings with batching
- **Accuracy**: ~90% combined (70% built-in + 85% LLM for remaining)
- **Folder Sync**: 10x performance optimization

**Key Components:**
```
CategorizationManager → LLMCategorizer → [4 Providers]
                     ↓
                CategoryFolderSyncService → [Drawer/Home Folders]
                     ↓
                UserCorrectionLearner → [Improve over time]
```

### Download & Testing

**Latest Development Build**: [dev-latest release](https://github.com/thejaustin/AutoCat/releases/tag/dev-latest)

AutoCat uses a versioning scheme: `15.0.b1-autocat.{BUILD_NUMBER}`
- `15.0.b1` = Lawnchair 15.0 Beta 1 (base version)
- `autocat.X` = AutoCat build number

Every push creates TWO releases:
- **`dev-latest`** (rolling) - Always points to newest build
- **`v15.0.b1-autocat.X`** (versioned) - Permanent release for each build

**Quick Install**:
1. **Download**: [dev-latest release](https://github.com/thejaustin/AutoCat/releases/tag/dev-latest) → Download `AutoCat-dev-latest.apk`
2. **Install**: Enable "Install from Unknown Sources" in Android settings
3. **Test**: See [WHAT_TO_TEST.md](WHAT_TO_TEST.md) for testing checklist

**All Versions**: [Releases page](https://github.com/thejaustin/AutoCat/releases) | **Testing Guide**: [TESTING.md](TESTING.md) | **Changelog**: [CHANGELOG.md](CHANGELOG.md)

### 🔐 Security & Privacy

- **API Keys**: Users must provide their own LLM API keys (not included)
- **Local Processing**: All categorization happens on-device after fetching from LLM
- **No Telemetry**: No data collection or tracking
- **Open Source**: All code is publicly available for audit

**Supported LLM Providers:**
- Google AI (Gemini) - Free tier available
- OpenAI (GPT) - Pay-per-use
- Anthropic (Claude) - Pay-per-use
- Perplexity (Llama) - Free tier available

### Development Logs

This project includes detailed development session logs in the [`dev-logs/`](dev-logs/) directory. Each log contains:
- Full conversation transcripts between developer and AI assistant
- Technical decisions and rationale
- Code changes and commits
- Next steps and open questions

See [`dev-logs/README.md`](dev-logs/README.md) for more information.

---

## About Lawnchair

[![Crowdin](https://badges.crowdin.net/e/188ba69d884418987f0b7f1dd55e3a4e/localized.svg)](https://lawnchair.crowdin.com/lawnchair)
[![OpenCollective](https://img.shields.io/opencollective/all/lawnchair?label=financial%20contributors&logo=open-collective)](https://opencollective.com/lawnchair)
[![Telegram](https://img.shields.io/endpoint?url=https%3A%2F%2Ftg.sumanjay.workers.dev%2Flccommunity)](https://t.me/lccommunity)
[![Discord](https://img.shields.io/discord/803299970169700402?label=server&logo=discord)](https://discord.gg/3x8qNWxgGZ)

<picture>
    <!-- Avoid image being clickable with slight workaround -->
    <!-- ❤️ Credit to Raine for the original mockup on the Lawnchair Discord -->
    <!-- ❤️ Credit to Lawrence Kayku for the current mockup on Unsplash 
            https://unsplash.com/photos/photography-of-green-leaves-ZVKr8wADhpc 
    -->
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/device-frame.png" width="250px">
    <img alt="Google Pixel running Lawnchair Launcher with green wallpaper" src="docs/assets/device-frame.png" width="250px">
</picture>

Lawnchair is a free, open-source home app for Android. Taking Launcher3—Android’s default home app—as a starting point, it ports Pixel Launcher features and introduces rich customization options.

This branch houses the codebase of Lawnchair 15, which is currently in beta and is based on Launcher3 from Android 15. For Lawnchair 9 to 14, see the branches with the `9-` to `14-` prefixes, respectively.

## Features

-   **Material You Theming:** Adapts to your wallpaper and system theme.
-   **At a Glance Widget:** Displays information *at a glance* with support for [Smartspacer](https://github.com/KieronQuinn/Smartspacer).
-   **QuickSwitch Support:** Integrates with Android Recents on Android 10 and newer. (requires root)
-   **Global Search:** Allows quick access to apps, contacts, and web results from the home screen.
-   **Customization Options:** Provides options to tweak icons, fonts, and colors to your liking.
-   And more!

## Download

<p align="left">
  <a href="https://play.google.com/store/apps/details?id=app.lawnchair.play">
    <picture>
      <!-- Avoid image being clickable with slight workaround -->
      <source media="(prefers-color-scheme: dark)" srcset="docs/assets/badge-google-play.png" height="60">
      <img alt="Get it on Google Play" src="docs/assets/badge-google-play.png" height="60">
    </picture>
  </a>
  <a href="https://apt.izzysoft.de/fdroid/index/apk/app.lawnchair">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="docs/assets/badge-izzyondroid.png" height="60">
      <img alt="Get it on IzzyOnDroid" src="docs/assets/badge-izzyondroid.png" height="60">
    </picture>
  </a>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/LawnchairLauncher/lawnchair/">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="docs/assets/badge-obtainium.png" height="60">
      <img alt="Get it on Obtainium" src="docs/assets/badge-obtainium.png" height="60">
    </picture>
  </a>
    <a href="https://github.com/LawnchairLauncher/lawnchair/releases">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="docs/assets/badge-github.png" height="60">
      <img alt="Get it on GitHub" src="docs/assets/badge-github.png" height="60">
    </picture>
  </a>
</p>

Lawnchair on Play Store will install as a different app from other sources. Some features may be restricted to comply with Google Play's publishing rules.

### Development builds

Interested in keeping yourself up-to-date with every Lawnchair development? Try our development builds!

These builds offer the latest features and bug fixes at a cost of being slower and introducing new bugs. Ensure that you make backups before installing.

**Download:** [Obtainium][Obtainium link] • [GitHub][GitHub link] • [nightly.link][Nightly link]

### Verification

Verify the integrity of your Lawnchair download using these SHA-256 hashes:

###### Google Play
```
47:AC:92:63:1C:60:35:13:CC:8D:26:DD:9C:FF:E0:71:9A:8B:36:55:44:DC:CE:C2:09:58:24:EC:25:61:20:A7
```

###### Elsewhere
```
74:7C:36:45:B3:57:25:8B:2E:23:E8:51:E5:3C:96:74:7F:E0:AD:D0:07:E5:BA:2C:D9:7E:8C:85:57:2E:4D:C5
```

## Contributing

Please visit the [Lawnchair Contributing Guidelines](CONTRIBUTING.md) for information and tips on contributing to Lawnchair.

## Supporting Lawnchair

If you love what we do, consider [supporting us on Open Collective](https://opencollective.com/lawnchair)! Your contributions help keep Lawnchair independent and enable us to develop faster.

A huge thank you to our **Core Backers ($5+)**:
*(These backers directly fund our Project Velocity Fund)*

[![Core Backers](https://opencollective.com/lawnchair/tiers/backer.svg?avatarHeight=64&width=890&button=false)](https://opencollective.com/lawnchair)

[Become a supporter](https://opencollective.com/lawnchair) to help us cover our operational costs, or become a Core Backer to be featured here!

## Quick links

-   [Website](https://lawnchair.app)
-   [News on Telegram](https://t.me/lawnchairci)
-   [Discord](https://discord.com/invite/3x8qNWxgGZ)
-   [Lawnchair on X (formerly Twitter)](https://x.com/lawnchairapp)
-   [_XDA_ thread](https://xdaforums.com/t/lawnchair-customizable-pixel-launcher.3627137/)

You can view all our links in the [Lawnchair Wiki](https://github.com/LawnchairLauncher/lawnchair/wiki).

<!-- Download link -->
[Nightly link]: https://nightly.link/LawnchairLauncher/lawnchair/workflows/ci/15-dev
[Obtainium link]: https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22app.lawnchair.nightly%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Flawnchairlauncher%2Flawnchair%22%2C%22author%22%3A%22Lawnchair%20Launcher%22%2C%22name%22%3A%22Lawnchair%20(Debug)%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Afalse%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Lawnchair%20Nightly%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22dontSortReleasesList%5C%22%3Afalse%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Atrue%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22Lawnchair%20is%20a%20free%2C%20open-source%20home%20app%20for%20Android.%20(NOTE%3A%20This%20is%20the%20debug%20version%20of%20Lawnchair%2C%20for%20the%20beta%2Fstable%20versions%20see%20%5C%5C%5C%22Lawnchair%5C%5C%5C%22)%5C%22%7D%22%7D
[GitHub link]: https://github.com/LawnchairLauncher/lawnchair/releases/tag/nightly

