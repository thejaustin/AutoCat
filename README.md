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

<details>
<summary><strong>What's different from Lawnchair</strong></summary>

| Feature | Lawnchair | AutoCat |
|---|---|---|
| Multi-provider LLM categorization (Gemini, Claude, GPT, Llama) | -- | Yes |
| Batch API processing with auto batch sizing | -- | Yes |
| Provider fallback and auto-select best model | -- | Yes |
| Model accuracy tracking and analytics | -- | Yes |
| Drawer folder sync from categories | -- | Yes |
| Home screen folder sync from categories | -- | Yes |
| Multi-stage categorization pipeline (built-in + LLM + user overrides) | -- | Yes |
| User correction learning system | -- | Yes |
| Category tabs in app drawer | -- | Yes |
| Smart Launcher backup import (.slbk) | -- | Yes |
| LLM reasoning display per app | -- | Yes |
| Room database for categories and folders | -- | Yes |
| Circuit breaker per provider (auto-disable on repeated failures) | -- | Yes |
| Confidence calibration across providers | -- | Yes |
| Developer diagnostics and structured error logging | -- | Yes |
| Basic auto-categorization (Caddy) | Yes | Yes |

</details>

<details>
<summary><strong>Architecture and development notes</strong></summary>

### Tech stack
- Kotlin with Coroutines
- Room (SQLite) for categories and folders
- OkHttp for LLM REST APIs
- Jetpack Compose + Material 3
- Base: Lawnchair 16-dev (Android 16 Launcher3)

### Categorization pipeline
```
CategorizationManager → LLMCategorizer → [Gemini | Claude | GPT | Llama]
                     ↓
               CategoryFolderSyncService → Drawer / Home folders
                     ↓
               UserCorrectionLearner → Accuracy tracking → Auto model selection
```

### Key files
- `CategorizationManager.kt` -- orchestrates the multi-stage pipeline
- `LLMCategorizer.kt` -- batch processing, provider selection, retry logic
- `CategoryFolderSyncService.kt` -- syncs categories to drawer/home folders
- `ProviderCircuitBreaker.kt` -- disables failing providers temporarily
- `ConfidenceCalibrator.kt` -- normalizes confidence scores across providers
- `UserCorrectionLearner.kt` -- learns from manual overrides

### Concurrency model
- `AtomicBoolean` guard on `recategorizeAll()` to prevent concurrent runs
- `Mutex` on folder sync to serialize operations
- `@Synchronized` circuit breaker with `ConcurrentHashMap`
- `StateFlow.update{}` for atomic progress emissions

### Building
Builds run via GitHub Actions on every push. See [CI workflow](.github/workflows/ci.yml).

### Versioning
`16.0.dev-autocat.{BUILD_NUMBER}` -- every push creates a versioned release and updates the `dev-next` rolling release.

</details>

## Credits

Based on [Lawnchair Launcher](https://github.com/LawnchairLauncher/lawnchair). All credit for the base launcher goes to the Lawnchair team.
