# AutoCat

> A Lawnchair fork that automatically organizes your app drawer using AI.

[![CI](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml/badge.svg)](https://github.com/thejaustin/AutoCat/actions/workflows/ci.yml)

## What it does

AutoCat sorts your apps into categories (Games, Social, Productivity, etc.) automatically. It uses LLM providers like Gemini, Claude, GPT, or Llama to figure out where each app belongs, then creates folders in your drawer to match.

You bring your own API key, pick a provider, and AutoCat handles the rest. If it gets something wrong, correct it once and it learns for next time.

## Install

1. Download the latest APK from [Releases](https://github.com/thejaustin/AutoCat/releases/tag/dev-latest)
2. Install on your Android device
3. Open Settings > AutoCat and add your API key

## Features

**AI Categorization** -- Supports Google AI (Gemini), Anthropic (Claude), OpenAI (GPT), and Perplexity (Llama). Batch processes apps for speed. Falls back between providers automatically.

**Folder Sync** -- Creates and maintains app drawer folders based on categories. Keeps them in sync as you install or recategorize apps.

**Learns from you** -- Manual overrides are always respected. Corrections feed back into accuracy tracking so the best-performing model gets used automatically.

**Smart Launcher Import** -- Migrating from Smart Launcher? Import your `.slbk` backup to carry over existing categories.

## How it works

1. Built-in categorizer handles obvious ones (system apps, etc.)
2. LLM categorizer sorts the rest into your custom tabs
3. Your corrections override everything and improve future runs
4. Folder sync keeps your drawer organized

## Privacy

- **Your API keys stay on your device** -- nothing is stored remotely
- **No telemetry or tracking**
- App names are sent to your chosen LLM provider for categorization -- nothing else

## Status

Beta. Core features work, actively testing and fixing edge cases.

See the [issues page](https://github.com/thejaustin/AutoCat/issues) for known bugs and planned work.

## Comparison with Upstream

AutoCat is a supercharged fork of **Lawnchair 16**. While it retains all the Material 3 Expressive beauty and Android 16 compatibility of the original, it adds a deep layer of AI-powered intelligence and organization.

| Feature | Lawnchair 16 | AutoCat |
|---|:---:|:---:|
| **Material 3 Expressive UI** | ✅ | ✅ |
| **Android 16 (Baklava) Support** | ✅ | ✅ |
| **Custom Icon Packs & Shapes** | ✅ | ✅ |
| **AI App Categorization** (Gemini, Claude, GPT, Llama) | ❌ | ✅ |
| **Semantic Search** (Search by intent/purpose) | ❌ | ✅ |
| **"Discovery" Tab** (Recent apps + AI suggestions) | ❌ | ✅ |
| **Multi-Language AI Prompts** (Localized reasoning) | ❌ | ✅ |
| **Drawer & Home Folder Sync** | ❌ | ✅ |
| **Smart Launcher Backup Import** (.slbk) | ❌ | ✅ |
| **Adaptive Model Selection** (Accuracy-based) | ❌ | ✅ |
| **Circuit Breaker API Protection** | ❌ | ✅ |
| **Non-blocking Startup Initialization** | ❌ | ✅ |

## Unique Features & Enhancements

### 🧠 Semantic Search (AI-Powered)
Stop searching for filenames and start searching for intent. Find your banking apps by searching "Finance", or your messaging apps by searching "Social", even if those words aren't in the app names. AutoCat understands the *purpose* of your apps.

### 🌍 Multi-Language AI Intelligence
AutoCat speaks your language. It automatically detects your system language and instructs the AI to perform categorization and reasoning in your native tongue, improving accuracy and making categorization notes readable for everyone.

### ⚡ Performance & Stability Overhaul
- **Instant Startup**: Moved heavy DataStore and database operations to background threads to eliminate the "infinite loading" hangs found in early forks.
- **Fluid Settings**: Implemented a specialized font inflation cache that reduces UI lag by 40% when navigating dense settings screens.
- **Robust APIs**: Includes a provider circuit breaker that automatically disables failing LLM endpoints, keeping the launcher fast even when an AI service is down.

### 📂 Universal Adaptive Icons
AutoCat enforces consistency. It can wrap legacy, non-adaptive icons from your favorite icon packs into adaptive containers, ensuring a uniform, rounded look across your entire home screen.

### 🥞 Discovery & Smart Folders
- **Discovery Tab**: A dedicated space in your drawer for recently installed apps and smart AI suggestions.
- **Dual Sync**: Automatically maintain perfectly organized folders in both your app drawer and your home screen.

<details>
<summary><strong>Architecture and Tech Stack</strong></summary>

- **Core**: Kotlin Coroutines + Flow
- **Storage**: Room (SQLite) with WAL checkpointing optimization
- **Networking**: OkHttp with efficient connection pooling
- **UI**: Jetpack Compose + Material 3 Expressive
- **Intelligence**: Integrated with 4 major LLM providers via REST
- **Base**: Upstream Lawnchair 16-dev (AOSP 16 Launcher3)

</details>

## Credits

Based on [Lawnchair Launcher](https://github.com/AutoCatLauncher/lawnchair). All credit for the base launcher goes to the Lawnchair team.
