/*
 * Copyright 2022, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.lawnchair.LawnchairLauncher
import app.lawnchair.font.FontCache
import app.lawnchair.util.isOnePlusStock
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.InvariantDeviceProfile.INDEX_DEFAULT
import com.android.launcher3.model.DeviceGridState
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.util.MainThreadInitializedObject
import com.android.launcher3.util.SafeCloseable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppSingleton
import javax.inject.Inject

@LauncherAppSingleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) :
    BasePreferenceManager(context),
    SafeCloseable {
    private val idp get() = InvariantDeviceProfile.INSTANCE.get(context)
    private val reloadIcons = { idp.onPreferencesChanged(context) }
    private val reloadGrid: () -> Unit = { idp.onPreferencesChanged(context) }

    private val recreate = {
        LawnchairLauncher.instance?.recreateIfNotScheduled()
        Unit
    }

    val iconPackPackage = StringPref("pref_iconPackPackage", "", reloadIcons)
    val themedIconPackPackage = StringPref("pref_themedIconPackPackage", "", recreate)
    val allowRotation = BoolPref("pref_allowRotation", false)
    val wrapAdaptiveIcons = BoolPref("prefs_wrapAdaptive", false, recreate)
    val transparentIconBackground = BoolPref("prefs_transparentIconBackground", false, recreate)
    val shadowBGIcons = BoolPref("pref_shadowBGIcons", true, recreate)
    val addIconToHome = BoolPref("pref_add_icon_to_home", true)
    val hotseatColumns = IntPref("pref_hotseatColumns", 4, reloadGrid)
    val workspaceColumns = IntPref("pref_workspaceColumns", 4)
    val workspaceRows = IntPref("pref_workspaceRows", 5)
    val workspaceIncreaseMaxGridSize = BoolPref("pref_workspace_increase_max_grid_size", false)
    val folderRows = IdpIntPref("pref_folderRows", { numFolderRows[INDEX_DEFAULT] }, reloadGrid)

    val drawerOpacity = FloatPref("pref_drawerOpacity", 1F, recreate)
    val coloredBackgroundLightness = FloatPref("pref_coloredBackgroundLightness", 0.9F, recreate)
    val feedProvider = StringPref("pref_feedProvider", "")
    val ignoreFeedWhitelist = BoolPref("pref_ignoreFeedWhitelist", false)
    val launcherTheme = StringPref("pref_launcherTheme", "system")
    val overrideWindowCornerRadius = BoolPref("pref_overrideWindowCornerRadius", false, recreate)
    val windowCornerRadius = IntPref("pref_windowCornerRadius", 80, recreate)
    val autoLaunchRoot = BoolPref("pref_autoLaunchRoot", false)
    val wallpaperScrolling = BoolPref("pref_wallpaperScrolling", true)
    val infiniteScrolling = BoolPref("pref_infiniteScrolling", false)
    val enableDebugMenu = BoolPref("pref_enableDebugMenu", false)
    val customAppName = object : MutableMapPref<ComponentKey, String>("pref_appNameMap", reloadGrid) {
        override fun flattenKey(key: ComponentKey) = key.toString()
        override fun unflattenKey(key: String) = ComponentKey.fromString(key)!!
        override fun flattenValue(value: String) = value
        override fun unflattenValue(value: String) = value
    }

    val recentActionOrder = StringPref("pref_recentActionOrder", "0,1,2,3,4", recreate)

    private val fontCache = FontCache.INSTANCE.get(context)
    val fontWorkspace = FontPref("pref_workspaceFont", fontCache.uiText, recreate)
    val fontHeading = FontPref("pref_fontHeading", fontCache.uiRegular, recreate)
    val fontHeadingMedium = FontPref("pref_fontHeadingMedium", fontCache.uiMedium, recreate)
    val fontBody = FontPref("pref_fontBody", fontCache.uiText, recreate)
    val fontBodyMedium = FontPref("pref_fontBodyMedium", fontCache.uiTextMedium, recreate)

    // TODO REMOVE
    val deviceSearch = BoolPref("device_search", false, recreate)
    val searchResultShortcuts = BoolPref("pref_searchResultShortcuts", false)
    val searchResultPeople = BoolPref("pref_searchResultPeople", false, recreate)
    val searchResultPixelTips = BoolPref("pref_searchResultPixelTips", false)
    val searchResultSettings = BoolPref("pref_searchResultSettings", false)
    val searchResultCalculator = BoolPref("pref_searchResultCalculator", false)

    val searchResultApps = BoolPref("pref_searchResultApps", true, recreate)
    val searchResultFilesToggle = BoolPref("pref_searchResultFiles", false, recreate)
    val searchResultAllFiles = BoolPref("pref_searchResultAllFiles", false, recreate)
    val searchResultAudio = BoolPref("pref_searchResultAudio", false, recreate)
    val searchResultVisualMedia = BoolPref("pref_searchResultVisualMedia", false, recreate)
    val searchResultStartPageSuggestion = BoolPref("pref_searchResultStartPageSuggestion", true, recreate)
    val searchResultSettingsEntry = BoolPref("pref_searchResultSettingsEntry", false, recreate)
    val searchResulRecentSuggestion = BoolPref("pref_searchResultRecentSuggestion", false, recreate)

    val allAppBulkIconLoading = BoolPref("pref_allapps_bulk_icon_loading", false, recreate)

    val themedIcons = BoolPref("themed_icons", false, recreate)
    val drawerThemedIcons = BoolPref("drawer_themed_icons", false, recreate)
    val tintIconPackBackgrounds = BoolPref("tint_icon_pack_backgrounds", false, recreate)

    val hotseatQsbCornerRadius = FloatPref("pref_hotseatQsbCornerRadius", 1F, recreate)
    val hotseatQsbAlpha = IntPref("pref_searchHotseatTranparency", 100, recreate)
    val hotseatQsbStrokeWidth = FloatPref("pref_searchStrokeWidth", 0F, recreate)
    val hotseatBG = BoolPref("pref_hotseatBG", false, recreate)
    val hotseatBGHorizontalInsetLeft = IntPref("pref_hotseatBGHRinsetLeft", 0, recreate)
    val hotseatBGVerticalInsetTop = IntPref("pref_hotseatBGVRinsetTop", 0, recreate)
    val hotseatBGHorizontalInsetRight = IntPref("pref_hotseatBGHRinsetRight", 0, recreate)
    val hotseatBGVerticalInsetBottom = IntPref("pref_hotseatBGVRinsetBottom", 0, recreate)

    val hotseatBGAlpha = IntPref("pref_hotseatBGTransparency", 100, recreate)

    val enableWallpaperBlur = BoolPref("pref_enableWallpaperBlur", false, recreate)
    val wallpaperBlur = IntPref("pref_wallpaperBlur", 25, recreate)
    val wallpaperBlurFactorThreshold = FloatPref("pref_wallpaperBlurFactor", 3.0F, recreate)

    val drawerListOrder = StringPref("pref_drawerListOrder", "", reloadGrid)
    val drawerList = BoolPref("pref_drawerList", true, recreate)
    val folderApps = BoolPref("pref_hideFolderApps", true, reloadGrid)

    // AutoCat: Work apps settings
    val showWorkTab = BoolPref("pref_showWorkTab", false, recreate)
    val hideWorkApps = BoolPref("pref_hideWorkApps", false, recreate)

    // AutoCat: Category tabs settings
    val useAppTabs = BoolPref("pref_useAppTabs", true, recreate)
    val smartCategoriesOnboardingCompleted = BoolPref("pref_smartCategoriesOnboardingCompleted", false, {})

    // AutoCat: LLM API keys
    val llmGoogleAIKey = StringPref("pref_llmGoogleAIKey", "", {})
    val llmClaudeKey = StringPref("pref_llmClaudeKey", "", {})
    val llmOpenAIKey = StringPref("pref_llmOpenAIKey", "", {})
    val llmPerplexityKey = StringPref("pref_llmPerplexityKey", "", {})
    val llmProviderPreference = StringPref("pref_llmProvider", "google_ai", {})
    val localEndpointEnabled = BoolPref("pref_localEndpointEnabled", false, {})
    val localEndpointUrl = StringPref("pref_localEndpointUrl", "http://localhost:11434/v1", {})
    val localEndpointModelId = StringPref("pref_localEndpointModelId", "", {})
    val localCustomModelPath = StringPref("pref_localCustomModelPath", "", {})
    val selectedLocalModelId = StringPref("pref_selectedLocalModelId", "", {})
    val llmAutoSelectBestModel = BoolPref("pref_llmAutoSelectBestModel", false, {})

    // AutoCat: LLM model selection
    val llmGoogleAIModel = StringPref("pref_llmGoogleAIModel", "gemini-2.0-flash-exp", {})
    val llmClaudeModel = StringPref("pref_llmClaudeModel", "claude-3-5-haiku-20241022", {})
    val llmOpenAIModel = StringPref("pref_llmOpenAIModel", "gpt-4o-mini", {})
    val llmPerplexityModel = StringPref("pref_llmPerplexityModel", "llama-3.1-sonar-small-128k-online", {}) // Updated to 2025 API

    // AutoCat: Batch processing settings
    val llmEnableBatching = BoolPref("pref_llmEnableBatching", true, {})
    val llmBatchSize = IntPref("pref_llmBatchSize", 0, {}) // 0 = auto-calculate

    // AutoCat: Folder sync settings
    val autoCatSyncFolders = BoolPref("pref_autoCatSyncFolders", true, {})
    val autoCatFolderSyncMode = StringPref("pref_autoCatFolderSyncMode", "DRAWER", {})

    // AutoCat: App tabs in app drawer
    val autoCatUseTabs = BoolPref("pref_autoCatUseTabs", false, recreate)

    // AutoCat: Developer mode
    val autoCatDevMode = BoolPref("pref_autoCatDevMode", false, {})

    // AutoCat: GenAI Folder Naming
    val autoCatGenAIFolderNaming = BoolPref("pref_autoCatGenAIFolderNaming", true, {})

    // AutoCat: Smart Dock
    val autoCatSmartDockEnabled = BoolPref("pref_autoCatSmartDockEnabled", false, recreate)

    // AutoCat: PWA App Drawer Integration
    val autoCatPwaIntegrationEnabled = BoolPref("pref_autoCatPwaIntegrationEnabled", true, recreate)

    // AutoCat: Zen Mode
    val autoCatEnableZenMode = BoolPref("pref_autoCatEnableZenMode", false, recreate)

    // AutoCat: Rate limiting
    val autoCatEnableRateLimiting = BoolPref("pref_autoCatEnableRateLimiting", false, {})

    // AutoCat: Circuit Breaker settings
    val circuitBreakerEnabled = BoolPref("pref_circuitBreakerEnabled", true, {})
    val circuitBreakerFailureThreshold = IntPref("pref_circuitBreakerFailureThreshold", 3, {})
    val circuitBreakerTimeoutMs = LongPref("pref_circuitBreakerTimeoutMs", 60_000L, {})
    val circuitBreakerHalfOpenDurationMs = LongPref("pref_circuitBreakerHalfOpenDurationMs", 10_000L, {})

    // Advanced / Dev Options
    val hideQuickstepSettings = BoolPref("pref_hideQuickstepSettings", false)
    val hideSettingsWarnings = BoolPref("pref_hideSettingsWarnings", false)

    val recentsActionScreenshot = BoolPref("pref_recentsActionScreenshot", !isOnePlusStock)
    val recentsActionShare = BoolPref("pref_recentsActionShare", isOnePlusStock)
    val recentsActionLens = BoolPref("pref_recentsActionLens", true)
    val recentsActionClearAll = BoolPref("pref_clearAllAsAction", false)
    val recentsActionLocked = BoolPref("pref_lockedAsAction", false)
    val recentsTranslucentBackground = BoolPref("pref_recentsTranslucentBackground", false, recreate)
    val recentsTranslucentBackgroundAlpha = FloatPref("pref_recentTranslucentBackgroundAlpha", .8f, recreate)

    override fun close() {
        scope.cancel()
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var migrationsPerformed = false

    private fun performMigrations() {
        if (migrationsPerformed) return
        migrationsPerformed = true

        migratePrefs(CURRENT_VERSION) { oldVersion ->
            if (oldVersion < 2) {
                val gridState = DeviceGridState(context).toProtoMessage()
                if (gridState.hotseatCount != -1) {
                    val colsAndRows = gridState.gridSize.split(",")
                    workspaceColumns.set(colsAndRows[0].toInt())
                    workspaceRows.set(colsAndRows[1].toInt())
                    hotseatColumns.set(gridState.hotseatCount)
                }
            }
        }

        // AutoCat: Migrate deprecated Gemini models
        if (llmGoogleAIModel.get() == "gemini-1.5-flash" ||
            llmGoogleAIModel.get() == "gemini-1.5-pro"
        ) {
            llmGoogleAIModel.set("gemini-2.0-flash-exp")
            android.util.Log.i(
                "PreferenceManager",
                "Auto-migrated Gemini model from deprecated 1.5 to 2.0",
            )
        }
    }

    init {
        sp.registerOnSharedPreferenceChangeListener(this)
        scope.launch {
            performMigrations()
        }
    }

    companion object {
        private const val CURRENT_VERSION = 2

        @JvmField
        val INSTANCE = MainThreadInitializedObject(::PreferenceManager)

        @JvmStatic
        fun getInstance(context: Context) = INSTANCE.get(context)!!
    }
}

@Composable
fun preferenceManager() = PreferenceManager.getInstance(LocalContext.current)
