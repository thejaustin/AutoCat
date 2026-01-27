# AutoCat

> **Fork of [Lawnchair 15](https://github.com/LawnchairLauncher/lawnchair)** with intelligent app auto-categorization

[![Build debug APK](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml/badge.svg)](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml)

## About AutoCat

**AutoCat** is a personal fork of Lawnchair Launcher that adds **AI-powered automatic app categorization** to the app drawer. It uses a multi-stage intelligent pipeline with LLM integration and automatic folder sync to keep your apps organized.

### Differences from Upstream

| Feature | Lawnchair 15 | AutoCat |
|---|---|---|
| Multi-provider LLM categorization (Gemini, Claude, GPT, Llama) | No | Yes |
| Batch API processing (20x faster, auto batch sizing) | No | Yes |
| Provider fallback & auto-select best model | No | Yes |
| Model accuracy tracking & analytics | No | Yes |
| Dual folder sync (drawer + home screen) | No | Yes |
| Smart categorization pipeline (built-in + LLM + user overrides) | No | Yes |
| User correction learning system | No | Yes |
| Category tabs in app drawer | No | Yes |
| Smart Launcher import (.slbk) | No | Yes |
| Developer diagnostics & error logging | No | Yes |
| App description display (LLM reasoning) | No | Yes |
| Room database for categories & folders | No | Yes |

### Key Features

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
3. **Verify**: Check the release notes for the SHA-256 hash to verify integrity.
4. **Test**: See [WHAT_TO_TEST.md](WHAT_TO_TEST.md) for testing checklist

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

Lawnchair is a free, open-source home app for Android. Taking Launcher3—Android’s default home app—as a starting point, it ports Pixel Launcher features and introduces rich customization options.

This project is a fork of [Lawnchair](https://github.com/LawnchairLauncher/lawnchair). All credit for the base launcher goes to the Lawnchair team.