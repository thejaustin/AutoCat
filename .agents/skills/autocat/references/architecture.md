# AutoCat Architecture & Logic

This reference guide documents the core categorization pipeline, database schemas, LLM provider abstraction, and folder sync systems in AutoCat.

## 🚀 Categorization Pipeline

The orchestration is managed by `CategorizationManager.kt` via `recategorizeAll()`.

1. **Clean-Up**: Deletes existing categorized items that are not marked as user overrides.
2. **Retrieve Apps**: Fetches all installed applications from the device.
3. **Stage 1 (LLM Categorizer)**: Uses `LLMCategorizer.kt` to categorize apps. If batch mode is enabled, it uses the auto-calculated batch size and fallbacks.
4. **Stage 2 (Built-In Categorizer)**: Falls back to using the Android framework's built-in categories for any remaining uncategorized apps.
5. **Cache Refresh**: Updates the in-memory cache in `AutoCatAppProvider.kt` to make categorizations instantly queryable.
6. **Folder Sync**: Runs `CategoryFolderSyncService.kt` to sync categories into folders based on the chosen mode.

## 🏗️ Core Interfaces & Systems

### 1. LLM Provider System (`llm/`)
- **Interface**: `LLMProvider.kt` defines required properties and methods (e.g. `categorizeApp`, `categorizeAppBatch`, `testConnection`, `getCurrentModel`).
- **Implementations**:
  - `GoogleAIProvider.kt`: Interfaces with Gemini models (e.g. `gemini-2.0-flash-exp`).
  - `ClaudeProvider.kt`: Interfaces with Anthropic models (e.g. `claude-3-5-haiku`).
  - `OpenAIProvider.kt`: Interfaces with GPT models (e.g. `gpt-4o-mini`).
  - `PerplexityProvider.kt`: Interfaces with Perplexity (e.g. `sonar`).
- **Log Management**: `LLMLogger.kt` records requests/responses and keeps a rolling log of up to 200 items, facilitating debugging and metrics tracking.

### 2. Batch Processing & Sizing
- **Calculations**: Done via `BatchCalculator.kt` based on context windows (with 30% safety margins) and token estimations (~50 tokens/app).
- **Efficiency**: Batch processing reduces token consumption by ~68% and speeds up categorization by up to 20x compared to sequential queries.

### 3. Folder Synchronization (`CategoryFolderSyncService.kt`)
Supports three sync modes:
- **DRAWER**: Groups apps by category and populates folders in the app drawer (updates `FolderInfoEntity` via `FolderService`).
- **HOME_SCREEN**: Populates workspace folders. Only places apps already present on the home screen. Sets the flag `0x80000000` to distinguish folders created by AutoCat.
- **BOTH**: Performs both drawer and home screen sync operations.

## 📊 Database Schema

### Category Database (Room)
Defined in `app/lawnchair/data/category/`:
- **`CustomCategory`**: Represents the category metadata (id, name, colorHex, isVisible, sortOrder).
- **`AppCategory`**: Holds app-to-category mappings (packageName, category, confidence, source, isUserOverride, reasoning).

### Folder Database (Room)
- **`FolderInfoEntity`**: Folder metadata (id, title, hide, rank).
- **`FolderItemEntity`**: Maps apps to folders (id, folderId, rank, componentKey).
