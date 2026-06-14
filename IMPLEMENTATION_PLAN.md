# AutoCat Complete Implementation Plan

## Overview
This plan addresses four critical improvements to the AutoCat app categorization system:

1. **Enhanced Error Logging & API Testing** - Better debugging capabilities in settings
2. **Dynamic Model Selection & Gemini 2.5 Migration** - Handle model deprecation and enable smart model selection
3. **Batch Processing Optimization** - Send multiple apps in a single API call to save tokens and time
4. **Automatic Folder Creation & Sync** - Create actual folders in app drawer from AI categories

---

## Current State Analysis

### Error Logging (Current)
- **Logging**: Basic `android.util.Log.d/e` scattered across providers
- **UI Feedback**: Limited to checkmarks (✓) for API key presence and generic error messages
- **Testing**: No built-in API connection testing in settings
- **Debugging**: Requires checking logcat from terminal, rebuilding app to see logs
- **Issues**:
  - No structured logging or error tracking
  - Error messages lack context (HTTP status, request/response details)
  - Cannot test API configuration without running full categorization
  - No visibility into rate limiting or quota issues

### Gemini Model Configuration (Current)
- **Hardcoded**: `gemini-1.5-flash` in `GoogleAIProvider.kt:153`
- **Problem**: Gemini 1.5 is deprecated, REQUIRES 2.5 migration
- **No Dynamic Selection**: Cannot switch models without code changes
- **No Fallback**: If model is unavailable, entire provider fails
- **Files Affected**:
  - `GoogleAIProvider.kt` - Hardcoded model string
  - All other providers have hardcoded models too (Claude, OpenAI, Perplexity)

### Batch Processing (Current)
- **Sequential**: Apps processed one-by-one in `LLMCategorizer.categorizeBatch()`
- **Rate Limiting**: 4-second delay between requests for Google (15 RPM limit)
- **Token Waste**: Each request includes full prompt overhead
- **Time**: For 100 apps = 100 requests × 4s = 6.7 minutes minimum
- **API Limitations**: Current providers support batch in request body but we don't use it
- **Files Involved**:
  - `LLMCategorizer.kt:139-168` - Sequential batch processing
  - `CategorizationManager.kt:119-220` - Orchestration

### App Drawer Folder Organization (Current)
- **Two Parallel Systems**:
  1. **AutoCat Categories** (AI-powered, in-memory folders)
     - Categories stored in `AppCategory` database table
     - Folders created in memory only (not persisted to `FolderInfoEntity`)
     - Used when `drawerList` preference = FALSE
  2. **Legacy Folders** (manual, persisted)
     - Folders stored in `FolderInfoEntity` + `FolderItemEntity` tables
     - Manually created via settings UI
     - Used when `drawerList` preference = TRUE

- **THE PROBLEM**:
  - AI categories NOT creating actual persistent folders
  - Apps categorized by AI but folders vanish on restart
  - No sync between `AppCategory` table and `FolderInfoEntity` table
  - User has to manually create folders matching category names

- **Files Involved**:
  - `LawnchairAlphabeticalAppsList.kt:91-105` - Creates temporary FolderInfo in memory
  - `AutoCatAppProvider.kt:94-120` - Categorizes apps but doesn't persist folders
  - `FolderService.kt` - Has methods to persist folders (unused for AutoCat)
  - `CategorizationManager.kt:106-107` - Refreshes cache but doesn't create folders

---

## Goal 1: Enhanced Error Logging & API Testing

### Objectives
- Add "Test Connection" button for each provider in settings
- Implement detailed request/response logging
- Create structured error messages with actionable suggestions
- Add visual error log viewer in settings (optional, for debugging)
- Track API quota/rate limit status

### Implementation Plan

#### 1.1 Create Logging Infrastructure
**New File**: `lawnchair/src/app/lawnchair/categorization/llm/LLMLogger.kt`

**Purpose**: Centralized logging with structured error tracking

**Features**:
- Log levels: DEBUG, INFO, WARNING, ERROR
- Request/response logging with timestamps
- HTTP status code tracking
- Error categorization (auth, rate limit, network, parsing, etc.)
- In-memory log buffer (last 100 entries) for UI display
- Export logs to file for debugging

**Interface**:
```kotlin
object LLMLogger {
    enum class LogLevel { DEBUG, INFO, WARNING, ERROR }

    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val provider: String,
        val operation: String,
        val message: String,
        val details: Map<String, Any>? = null,
        val exception: Throwable? = null
    )

    fun logRequest(provider: String, endpoint: String, requestBody: String)
    fun logResponse(provider: String, statusCode: Int, responseBody: String, durationMs: Long)
    fun logError(provider: String, operation: String, error: Throwable, context: Map<String, Any>? = null)
    fun getRecentLogs(count: Int = 100): List<LogEntry>
    fun clearLogs()
    fun exportLogs(file: File)
}
```

#### 1.2 Enhance Provider Error Handling
**Files to Modify**:
- `GoogleAIProvider.kt`
- `ClaudeProvider.kt`
- `OpenAIProvider.kt`
- `PerplexityProvider.kt`

**Changes**:
1. Wrap all API calls with detailed logging
2. Parse error responses and provide user-friendly messages
3. Detect specific error types:
   - Invalid API key (401/403)
   - Rate limit exceeded (429)
   - Network timeout
   - Invalid model (404)
   - Quota exceeded
   - Malformed JSON response

**Example Error Mapping**:
```kotlin
private fun mapHttpError(statusCode: Int, errorBody: String): String {
    return when (statusCode) {
        401 -> "Invalid API key. Please check your key in settings."
        403 -> "API access forbidden. Verify your key has proper permissions."
        429 -> "Rate limit exceeded. Please wait or upgrade your API plan."
        404 -> "Model not found. The model may be deprecated."
        500 -> "Server error. Try again later."
        else -> "HTTP $statusCode: $errorBody"
    }
}
```

#### 1.3 Add Test Connection Feature
**File to Modify**: `LLMSettingsPreferences.kt`

**UI Changes**:
1. Add "Test Connection" button next to each API key field
2. Show loading spinner during test
3. Display result with checkmark ✓ or error ✗
4. Show detailed error message if test fails

**New Method in Each Provider**:
```kotlin
data class TestResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long? = null,
    val modelVersion: String? = null
)

suspend fun testConnection(): TestResult {
    // Make minimal API call to verify connectivity
    // Return detailed result
}
```

---

## Goal 2: Dynamic Model Selection & Gemini 2.5 Migration

### Objectives
- Migrate from hardcoded models to configurable model selection
- Support Gemini 2.5 (deprecated 1.5)
- Implement model availability checking
- Add fallback model chains per provider
- Enable model-specific configuration (temperature, tokens, etc.)

### Implementation Plan

#### 2.1 Create Model Configuration System
**New File**: `lawnchair/src/app/lawnchair/categorization/llm/ModelConfig.kt`

**Purpose**: Define available models and their capabilities

```kotlin
data class ModelInfo(
    val id: String,                    // API identifier (e.g., "gemini-2.5-flash")
    val displayName: String,           // UI name (e.g., "Gemini 2.5 Flash")
    val provider: String,              // "google_ai", "claude", etc.
    val isAvailable: Boolean = true,   // Deprecated models = false
    val costTier: String,              // "free", "low", "medium", "high"
    val contextWindow: Int,            // Max tokens
    val speedTier: String,             // "fast", "medium", "slow"
    val qualityTier: String,           // "standard", "high", "premium"
    val recommendedFor: List<String>   // ["categorization", "suggestions", etc.]
)

object ModelRegistry {
    val GOOGLE_AI_MODELS = listOf(
        ModelInfo(
            id = "gemini-2.0-flash-exp",
            displayName = "Gemini 2.0 Flash (Recommended)",
            provider = "google_ai",
            isAvailable = true,
            costTier = "free",
            contextWindow = 1048576,
            speedTier = "fast",
            qualityTier = "high",
            recommendedFor = listOf("categorization", "suggestions", "batch")
        ),
        ModelInfo(
            id = "gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash (Deprecated)",
            provider = "google_ai",
            isAvailable = false,  // Mark as deprecated
            costTier = "free",
            contextWindow = 32768,
            speedTier = "fast",
            qualityTier = "standard",
            recommendedFor = emptyList()
        )
    )

    // ... CLAUDE_MODELS, OPENAI_MODELS, PERPLEXITY_MODELS ...
}
```

#### 2.2 Add Model Selection to Preferences
**File to Modify**: `PreferenceManager.kt`

**New Preferences**:
```kotlin
// Model selection per provider
val llmGoogleAIModel = StringPref("pref_llmGoogleAIModel", "gemini-2.0-flash-exp", {})
val llmClaudeModel = StringPref("pref_llmClaudeModel", "claude-3-5-haiku-20241022", {})
val llmOpenAIModel = StringPref("pref_llmOpenAIModel", "gpt-4o-mini", {})
val llmPerplexityModel = StringPref("pref_llmPerplexityModel", "llama-3.1-sonar-small-128k-online", {})
```

#### 2.3 Update Providers to Use Dynamic Models
**Files to Modify**: All `*Provider.kt` files

**Example for GoogleAIProvider.kt**:
```kotlin
private val effectiveModel: String
    get() {
        val prefManager = PreferenceManager.getInstance(context)
        val configuredModel = prefManager.llmGoogleAIModel.get()

        // Validate model is available
        val modelInfo = ModelRegistry.getAvailableModels("google_ai")
            .find { it.id == configuredModel }

        if (modelInfo == null || !modelInfo.isAvailable) {
            android.util.Log.w(TAG, "Model $configuredModel not available, using default")
            return ModelRegistry.getDefaultModel("google_ai")?.id ?: "gemini-2.0-flash-exp"
        }

        return configuredModel
    }

private fun callGeminiAPI(prompt: String): String {
    val url = URL("https://generativelanguage.googleapis.com/v1/models/$effectiveModel:generateContent?key=$effectiveApiKey")
    // ... rest of implementation
}
```

---

## Goal 3: Batch Processing Optimization

### Objectives
- Send multiple apps in a single API request to save tokens and time
- Implement smart batching based on context window limits
- Reduce API calls from 100 (4s each = 6.7min) to ~5 (= 20s)
- Maintain compatibility with providers that don't support batch processing
- Add configurable batch sizes

### Performance Comparison

**Current** (100 apps, Google AI):
- Requests: 100
- Time: 100 × 4s = 6 minutes 40 seconds
- Tokens: ~200 prompt tokens × 100 = 20,000 prompt tokens

**Target** (100 apps, batch size 20):
- Requests: 5
- Time: 5 × 4s = 20 seconds (20x faster!)
- Tokens: ~500 prompt tokens × 5 = 2,500 prompt tokens (8x reduction!)

### Implementation Plan

#### 3.1 Create Batch Processing Infrastructure
**New Files**:
1. `lawnchair/src/app/lawnchair/categorization/llm/BatchCalculator.kt` - Calculate optimal batch sizes
2. Update `LLMProvider.kt` interface with batch methods

**Add to LLMProvider.kt**:
```kotlin
interface LLMProvider {
    // ... existing methods ...

    /**
     * Categorizes multiple apps in a single batch request.
     * Returns map of packageName → CategorizationResult
     */
    suspend fun categorizeAppBatch(
        apps: List<AppBatchInfo>,
        availableCategories: List<String>
    ): Map<String, CategorizationResult>
}

data class AppBatchInfo(
    val packageName: String,
    val appName: String,
    val appDescription: String?
)
```

#### 3.2 Implement Batch Methods in All Providers
**Files to Modify**:
- `GoogleAIProvider.kt`
- `ClaudeProvider.kt`
- `OpenAIProvider.kt`
- `PerplexityProvider.kt`

**Batch Prompt Format**:
```
Categorize these apps in a single response:

Available Categories:
- Social
- Productivity
- Games

Apps to categorize:
1. App: Gmail, Package: com.google.android.gm
2. App: Chrome, Package: com.android.chrome
3. App: Instagram, Package: com.instagram.android

Respond in JSON:
{
  "results": [
    {
      "packageName": "com.google.android.gm",
      "category": "Productivity",
      "confidence": 0.95,
      "reasoning": "Email client"
    }
  ]
}
```

#### 3.3 Update LLMCategorizer to Use Batching
**File to Modify**: `LLMCategorizer.kt`

**Replace categorizeBatch() method** with smart batching logic.

---

## Goal 4: Automatic Folder Creation & Sync (NEW!)

### THE CRITICAL PROBLEM
Currently, AI categories exist only in the `AppCategory` database table. The app drawer creates temporary `FolderInfo` objects in memory for display, but they:
- **Don't persist** across app restarts
- **Don't sync** with the actual folder system
- **Don't appear** in the legacy folder list in settings
- **Disappear** when switching between drawer modes

### Objectives
- Automatically create persistent `FolderInfoEntity` entries when AI categorizes apps
- Sync `AppCategory` table → `FolderInfoEntity` + `FolderItemEntity` tables
- Make AI-created folders appear in settings folder management
- Support bidirectional sync (folder edits update categories)
- Merge AutoCat and legacy folder systems into one unified system

### Implementation Plan

#### 4.1 Create Folder Sync Service
**New File**: `lawnchair/src/app/lawnchair/categorization/CategoryFolderSyncService.kt`

**Purpose**: Bridge between AI categories and persistent folders

```kotlin
class CategoryFolderSyncService(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val folderService: FolderService
) {
    companion object {
        private const val TAG = "CategoryFolderSync"
        const val FOLDER_SOURCE_AUTOCAT = "autocat"  // Marker for AI-created folders
    }

    /**
     * Creates or updates folders based on current app categorizations.
     * Called after categorization completes.
     */
    suspend fun syncCategoriesToFolders() = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "Starting category → folder sync")

            // Get all categorized apps
            val appCategories = categoryDao.getAllAppCategories()

            // Group by category name
            val categorizedApps = groupAppsByCategory(appCategories)

            // Get existing folders (to update instead of duplicate)
            val existingFolders = folderService.getAllFolders()
            val existingFolderMap = existingFolders.associateBy { it.title }

            // Get visible custom categories for metadata (colors, ordering)
            val customCategories = categoryDao.getVisibleCustomCategories()
            val categoryMetadata = customCategories.associateBy { it.name }

            var createdCount = 0
            var updatedCount = 0

            categorizedApps.forEach { (categoryName, packages) ->
                // Get app info for all packages in this category
                val appInfos = packages.mapNotNull { pkg ->
                    getAppInfo(context, pkg)
                }

                if (appInfos.isEmpty()) {
                    android.util.Log.d(TAG, "No apps found for category: $categoryName")
                    return@forEach
                }

                // Check if folder already exists
                val existingFolder = existingFolderMap[categoryName]

                if (existingFolder != null) {
                    // Update existing folder with new apps
                    android.util.Log.d(TAG, "Updating folder: $categoryName with ${appInfos.size} apps")
                    folderService.updateFolderWithItems(
                        folderInfoId = existingFolder.id,
                        title = categoryName,
                        appInfos = appInfos
                    )
                    updatedCount++
                } else {
                    // Create new folder
                    android.util.Log.d(TAG, "Creating folder: $categoryName with ${appInfos.size} apps")

                    val folderInfo = FolderInfo().apply {
                        title = categoryName
                        // Add all apps to folder
                        appInfos.forEach { add(it) }

                        // Set metadata from CustomCategory if available
                        categoryMetadata[categoryName]?.let { customCat ->
                            // Could store color/sortOrder in folder options if supported
                        }
                    }

                    folderService.saveFolderInfo(folderInfo)
                    createdCount++
                }
            }

            // Update folder ordering based on category sort order
            updateFolderOrdering(categorizedApps.keys.toList(), customCategories)

            android.util.Log.d(TAG, "Sync complete: created $createdCount, updated $updatedCount folders")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error syncing categories to folders", e)
            throw e
        }
    }

    /**
     * Groups app categories by category name.
     * Returns map of category name → list of package names
     */
    private fun groupAppsByCategory(appCategories: List<AppCategory>): Map<String, List<String>> {
        return appCategories.groupBy({ it.category }, { it.packageName })
    }

    /**
     * Gets AppInfo for a package name
     */
    private fun getAppInfo(context: Context, packageName: String): AppInfo? {
        return try {
            AppMetadataProvider(context).getAppInfo(packageName)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get app info for $packageName", e)
            null
        }
    }

    /**
     * Updates folder ordering preference based on category sort orders
     */
    private suspend fun updateFolderOrdering(
        categoryNames: List<String>,
        customCategories: List<CustomCategory>
    ) {
        // Get all folders again to get their IDs
        val folders = folderService.getAllFolders()
        val folderIdMap = folders.associate { it.title to it.id }

        // Sort categories by sortOrder
        val sortedCategories = categoryNames.sortedBy { categoryName ->
            customCategories.find { it.name == categoryName }?.sortOrder ?: Int.MAX_VALUE
        }

        // Convert to folder IDs
        val folderIds = sortedCategories.mapNotNull { folderIdMap[it] }

        // Save to drawerListOrder preference
        val prefManager = PreferenceManager.getInstance(context)
        val orderString = FolderOrderUtils.intListToString(folderIds)
        prefManager.drawerListOrder.set(orderString)

        android.util.Log.d(TAG, "Updated folder order: $orderString")
    }

    /**
     * Removes folders that no longer have any categorized apps
     */
    suspend fun cleanupEmptyFolders() {
        // Get all folders
        val folders = folderService.getAllFolders()

        folders.forEach { folder ->
            if (folder.contents.isEmpty()) {
                android.util.Log.d(TAG, "Removing empty folder: ${folder.title}")
                folderService.deleteFolderInfo(folder.id)
            }
        }
    }

    /**
     * Singleton instance
     */
    companion object {
        @Volatile
        private var instance: CategoryFolderSyncService? = null

        fun getInstance(context: Context): CategoryFolderSyncService {
            return instance ?: synchronized(this) {
                instance ?: CategoryFolderSyncService(
                    context.applicationContext,
                    CategoryDatabase.getInstance(context.applicationContext).categoryDao(),
                    FolderService(context.applicationContext)
                ).also { instance = it }
            }
        }
    }
}
```

#### 4.2 Integrate Sync into Categorization Pipeline
**File to Modify**: `CategorizationManager.kt`

**Add after categorization completes**:

```kotlin
suspend fun initializeCategorization() = withContext(Dispatchers.IO) {
    try {
        // ... existing categorization code ...

        // Refresh cache after categorization
        appProvider.refreshCache()

        // NEW: Sync categories to persistent folders
        val syncService = CategoryFolderSyncService.getInstance(context)
        syncService.syncCategoriesToFolders()

        android.util.Log.d(TAG, "All stages complete with folder sync")

    } catch (e: Exception) {
        android.util.Log.e(TAG, "Error during categorization", e)
    }
}

suspend fun recategorizeAll() = withContext(Dispatchers.IO) {
    try {
        // ... existing recategorization code ...

        // Refresh cache after categorization
        appProvider.refreshCache()

        // NEW: Sync categories to persistent folders
        val syncService = CategoryFolderSyncService.getInstance(context)
        syncService.syncCategoriesToFolders()

        // Mark as complete
        _progress.value = CategorizationProgress(
            isRunning = false,
            currentStage = "Complete",
            processedCount = apps.size,
            totalCount = apps.size
        )

    } catch (e: Exception) {
        android.util.Log.e(TAG, "Error during re-categorization", e)
    }
}
```

#### 4.3 Add Folder Sync Toggle to Settings
**File to Modify**: `PreferenceManager.kt`

**New Preference**:
```kotlin
// AutoCat folder sync
val autoCatSyncFolders = BoolPref("pref_autoCatSyncFolders", true, {})
```

**File to Modify**: `LLMSettingsPreferences.kt`

**Add UI toggle**:
```
Actions
├─ [Re-categorize All Apps] button
├─ Auto-create folders from categories [✓]
   └─ Automatically create app drawer folders for each AI category
```

#### 4.4 Handle Folder Deletion & Updates
**File to Modify**: `CategoryFolderSyncService.kt`

**Add bidirectional sync**:

```kotlin
/**
 * When user deletes a folder in settings, optionally remove category assignments
 */
suspend fun onFolderDeleted(folderId: Int, categoryName: String, removeCategories: Boolean = false) {
    if (removeCategories) {
        // Remove all AppCategory entries for this category
        categoryDao.deleteAppCategoriesByCategory(categoryName)
        android.util.Log.d(TAG, "Removed category assignments for: $categoryName")
    } else {
        android.util.Log.d(TAG, "Folder deleted but category assignments preserved")
    }
}

/**
 * When user manually adds/removes apps from a folder, update categories
 */
suspend fun onFolderItemsChanged(folderId: Int, categoryName: String, newAppPackages: List<String>) {
    // Update AppCategory table to match folder contents
    val existingApps = categoryDao.getAppsByCategory(categoryName).map { it.packageName }

    // Apps added to folder
    val added = newAppPackages - existingApps.toSet()
    // Apps removed from folder
    val removed = existingApps - newAppPackages.toSet()

    added.forEach { packageName ->
        categoryDao.insertAppCategory(
            AppCategory(
                packageName = packageName,
                category = categoryName,
                confidence = 1.0f,
                source = AppCategory.SOURCE_USER,  // User manually assigned
                isUserOverride = true
            )
        )
    }

    removed.forEach { packageName ->
        categoryDao.deleteAppCategory(packageName)
    }

    android.util.Log.d(TAG, "Folder sync: added ${added.size}, removed ${removed.size} apps")
}
```

#### 4.5 Update App Drawer to Use Unified Folder System
**File to Modify**: `LawnchairAlphabeticalAppsList.kt`

**Current code (lines 91-105)** creates temporary folders:
```kotlin
if (!drawerList) {
    // AutoCat categorization mode
    val categorizedApps = appProvider.categorizeApps(filteredApps)
    categorizedApps.forEach { (category, apps) ->
        val folderInfo = FolderInfo()
        folderInfo.title = category
        apps.forEach { folderInfo.add(it) }
        data.add(folderInfo)
    }
} else {
    // Legacy folder mode
    val folders = folderViewModel.folders.value
    folders.forEach { data.add(it) }
}
```

**NEW unified approach**:
```kotlin
// ALWAYS use persistent folders (unified system)
val folders = folderViewModel.folders.value

if (folders.isEmpty() && PreferenceManager.getInstance(context).useCategoryTabs.get()) {
    // Fallback: If no folders exist yet, create temporary ones from categories
    // (This handles first-run before sync completes)
    val categorizedApps = appProvider.categorizeApps(filteredApps)
    categorizedApps.forEach { (category, apps) ->
        val folderInfo = FolderInfo()
        folderInfo.title = category
        apps.forEach { folderInfo.add(it) }
        data.add(folderInfo)
    }
} else {
    // Use persistent folders
    folders.forEach { data.add(it) }
}
```

**Remove `drawerList` preference** (unified system doesn't need this toggle):
- All folders are now persistent
- AutoCat creates them automatically
- Users can edit them manually
- Best of both worlds!

#### 4.6 Add Progress Tracking for Folder Sync
**File to Modify**: `CategorizationProgress.kt` (in CategorizationManager.kt)

**Add new stage**:
```kotlin
data class CategorizationProgress(
    val isRunning: Boolean = false,
    val currentStage: String = "",  // "LLM", "Built-in", "Syncing Folders", "Complete"
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentAppName: String? = null
)
```

**Update progress during sync**:
```kotlin
_progress.value = CategorizationProgress(
    isRunning = true,
    currentStage = "Syncing Folders",
    processedCount = apps.size,
    totalCount = apps.size,
    currentAppName = null
)

syncService.syncCategoriesToFolders()

_progress.value = CategorizationProgress(
    isRunning = false,
    currentStage = "Complete",
    processedCount = apps.size,
    totalCount = apps.size
)
```

---

## Implementation Phases

### Phase 1: Error Logging & Testing (Week 1)
**Priority**: HIGH - Enables debugging of all other features

**Tasks**:
1. Create `LLMLogger.kt` infrastructure
2. Add logging to all providers
3. Implement "Test Connection" in settings
4. Add error log viewer screen
5. Test with all providers

**Deliverables**:
- Can test API connections from settings
- Can view detailed error logs in-app
- Better error messages for users

**Files Created**: 1
**Files Modified**: 5

---

### Phase 2: Model Migration & Selection (Week 2)
**Priority**: CRITICAL - Gemini 1.5 deprecation is urgent

**Tasks**:
1. Create `ModelConfig.kt` with model registry
2. Add model preferences to `PreferenceManager.kt`
3. Update all providers to use dynamic models
4. Add model selection UI to settings
5. Implement automatic Gemini 1.5→2.5 migration
6. Create `ModelSelector.kt` for auto-selection

**Deliverables**:
- Gemini 2.0 Flash as default model
- Users can select models per provider
- Auto-migration of old settings
- Model deprecation warnings

**Files Created**: 2
**Files Modified**: 6

---

### Phase 3: Folder Sync System (Week 3)
**Priority**: HIGH - Core user experience issue

**Tasks**:
1. Create `CategoryFolderSyncService.kt`
2. Integrate sync into `CategorizationManager.kt`
3. Add sync toggle to settings
4. Implement bidirectional sync (folder edits → categories)
5. Update `LawnchairAlphabeticalAppsList.kt` for unified folders
6. Add progress tracking for folder sync
7. Test folder creation, updates, deletion

**Deliverables**:
- AI categories create persistent folders
- Folders survive app restart
- Folders appear in settings
- Manual folder edits sync back to categories
- Unified folder system (no more drawerList toggle)

**Files Created**: 1
**Files Modified**: 4

---

### Phase 4: Batch Processing (Week 4)
**Priority**: MEDIUM - Performance optimization

**Tasks**:
1. Create `BatchCalculator.kt`
2. Add batch methods to `LLMProvider` interface
3. Implement batch processing in all providers
4. Update `LLMCategorizer.kt` to use batching
5. Add batch settings UI
6. Update progress tracking

**Deliverables**:
- 10-20x faster categorization
- 5-10x token savings
- Configurable batch sizes
- Fallback to sequential if batch fails

**Files Created**: 1
**Files Modified**: 6

---

### Phase 5: Testing & Optimization (Week 5)
**Priority**: HIGH - Ensure quality

**Tasks**:
1. Test all providers with real API keys
2. Test batch processing with various app counts
3. Test folder sync in all scenarios
4. Test error handling and fallbacks
5. Performance benchmarking
6. UI/UX refinements
7. Documentation

**Deliverables**:
- Stable, tested implementation
- Performance metrics
- User documentation
- Developer documentation

---

## File Modification Summary

### New Files (7)
1. `lawnchair/src/app/lawnchair/categorization/llm/LLMLogger.kt` - Error logging infrastructure
2. `lawnchair/src/app/lawnchair/categorization/llm/ModelConfig.kt` - Model registry
3. `lawnchair/src/app/lawnchair/categorization/llm/ModelSelector.kt` - Auto model selection
4. `lawnchair/src/app/lawnchair/categorization/llm/BatchCalculator.kt` - Batch size optimization
5. `lawnchair/src/app/lawnchair/categorization/CategoryFolderSyncService.kt` - **NEW: Folder sync**
6. `lawnchair/src/app/lawnchair/ui/preferences/destinations/LLMErrorLogPreferences.kt` - Error log viewer

### Modified Files (12)
1. `lawnchair/src/app/lawnchair/categorization/llm/LLMProvider.kt` - Add batch + test methods
2. `lawnchair/src/app/lawnchair/categorization/llm/GoogleAIProvider.kt` - Dynamic models + batching + logging
3. `lawnchair/src/app/lawnchair/categorization/llm/ClaudeProvider.kt` - Dynamic models + batching + logging
4. `lawnchair/src/app/lawnchair/categorization/llm/OpenAIProvider.kt` - Dynamic models + batching + logging
5. `lawnchair/src/app/lawnchair/categorization/llm/PerplexityProvider.kt` - Dynamic models + batching + logging
6. `lawnchair/src/app/lawnchair/categorization/stages/LLMCategorizer.kt` - Batch processing logic
7. `lawnchair/src/app/lawnchair/categorization/CategorizationManager.kt` - **Folder sync integration + progress**
8. `lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt` - New preferences
9. `lawnchair/src/app/lawnchair/ui/preferences/destinations/LLMSettingsPreferences.kt` - UI updates
10. `lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt` - **Unified folder system**
11. `lawnchair/src/app/lawnchair/data/category/CategoryDao.kt` - **NEW: Query methods for sync**
12. `lawnchair/src/app/lawnchair/categorization/AutoCatAppProvider.kt` - **Integration with folder sync**

### Total Lines of Code
- New code: ~2,500 lines
- Modified code: ~700 lines
- Total effort: ~3,200 lines

---

## Success Metrics

### Goal 1: Error Logging
- ✓ Users can test API connections without full categorization
- ✓ Error messages are actionable and specific
- ✓ 90% of errors can be diagnosed from in-app logs
- ✓ Average debugging time reduced from 30min to 5min

### Goal 2: Model Selection
- ✓ All users auto-migrated to Gemini 2.0
- ✓ Users can select models per provider
- ✓ No categorization failures due to model deprecation
- ✓ Support for future model updates without code changes

### Goal 3: Batch Processing
- ✓ 10x reduction in categorization time (from 6min to 30s for 100 apps)
- ✓ 5x reduction in token usage
- ✓ <5% failure rate in batch processing
- ✓ Automatic fallback to sequential on batch failure

### Goal 4: Folder Sync (NEW!)
- ✓ AI categories create persistent folders automatically
- ✓ Folders survive app restart
- ✓ Folders appear in settings folder management
- ✓ Manual folder edits sync back to category database
- ✓ Zero folder loss on app restart
- ✓ Unified folder system (AutoCat + legacy merged)

---

## Risk Mitigation

### Risk 1: Batch Processing Failures
**Mitigation**:
- Always implement fallback to sequential processing
- Validate batch responses carefully
- Log detailed errors
- Allow users to disable batching

### Risk 2: Model Deprecation
**Mitigation**:
- Model registry with `isAvailable` flag
- Automatic fallback to default model
- Version checking in settings
- User notifications for deprecated models

### Risk 3: Folder Sync Conflicts
**Mitigation**:
- Transaction-based sync (all-or-nothing)
- User override flag in AppCategory (isUserOverride)
- Conflict resolution: user edits always win
- Sync logs for debugging

### Risk 4: API Rate Limits
**Mitigation**:
- Implement exponential backoff
- Respect rate limit headers
- Allow user to configure delays
- Show rate limit status in UI

### Risk 5: Breaking Changes
**Mitigation**:
- Maintain backward compatibility
- Automatic preference migration
- Thorough testing before release
- Gradual rollout (beta testing)

---

## Testing Plan

### Unit Tests
- Model registry parsing
- Batch size calculations
- Error mapping logic
- Folder sync logic

### Integration Tests
- Complete categorization pipeline
- Folder sync after categorization
- Provider fallback chains
- Batch → sequential fallback

### Manual Tests
- Test all 4 providers with real API keys
- Test with 10, 50, 100, 500 apps
- Test folder sync with category edits
- Test folder deletion and recreation
- Test app drawer display with synced folders

### Performance Tests
- Benchmark batch vs sequential
- Measure token usage reduction
- Test with slow network
- Test memory usage with large app counts

---

## Next Steps

1. ✓ **Review this plan** - Ensure all requirements are covered
2. **Get user approval** - Confirm approach and priorities
3. **Begin Phase 1** - Start with error logging infrastructure
4. **Phase 2** - Critical Gemini 2.0 migration
5. **Phase 3** - Implement folder sync (high user impact)
6. **Phase 4** - Add batch processing optimization
7. **Phase 5** - Test, refine, document

---

## Questions for Discussion

1. **Folder Naming**: Should AI-created folders have a special prefix/icon to distinguish from manual folders?
2. **Conflict Resolution**: If user manually moves an app to a different folder, should we:
   - a) Respect user choice and mark as override (recommended)
   - b) Re-categorize on next sync
   - c) Ask user what to do
3. **Batch Size**: Expose to users or keep automatic?
4. **Model Pricing**: Show cost estimates for different models?
5. **Migration Timeline**: When to remove `drawerList` preference completely?
6. **Folder Colors**: Sync category colors to folder backgrounds?

---

**Document Version**: 2.0
**Last Updated**: 2025-01-27
**Status**: Ready for Implementation
**Changes from v1.0**: Added Goal 4 (Folder Sync), expanded file list, updated metrics
