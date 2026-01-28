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

## Building

Builds run via GitHub Actions on every push. See [CI workflow](.github/workflows/ci.yml) for details.

## Credits

Based on [Lawnchair Launcher](https://github.com/LawnchairLauncher/lawnchair). All credit for the base launcher goes to the Lawnchair team.
