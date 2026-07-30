package app.lawnchair.ui.popup

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppGlobals
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.SuspendDialogInfo
import android.net.Uri
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import app.lawnchair.AutoCatLauncher
import app.lawnchair.appops.AppBatchOperationService
import app.lawnchair.override.CustomizeAppDialog
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.views.ComposeBottomSheet
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_TASK
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.model.data.AppInfo as ModelAppInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.util.ApplicationInfoWrapper
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.views.ActivityContext
import com.patrykmichalik.opto.core.firstBlocking
import java.net.URISyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoCatShortcut {

    companion object {

        val CUSTOMIZE =
            SystemShortcut.Factory { activity: AutoCatLauncher, itemInfo, originalView ->
                if (PreferenceManager2.getInstance(activity).lockHomeScreen.firstBlocking()) {
                    null
                } else {
                    getAppInfo(activity, itemInfo)?.let { Customize(activity, it, itemInfo, originalView) }
                }
            }

        private fun getAppInfo(launcher: AutoCatLauncher, itemInfo: ItemInfo): ModelAppInfo? {
            if (itemInfo is ModelAppInfo) return itemInfo
            if (itemInfo.itemType != ITEM_TYPE_APPLICATION) return null
            val key = ComponentKey(itemInfo.targetComponent, itemInfo.user)
            return launcher.appsView.appsStore.getApp(key)
        }

        val UNINSTALL =
            SystemShortcut.Factory { activity: ActivityContext, itemInfo: ItemInfo, view: View ->
                if (itemInfo.targetComponent == null) {
                    return@Factory null
                }
                if (ApplicationInfoWrapper(
                        activity.asContext(),
                        itemInfo.targetComponent!!.packageName,
                        itemInfo.user,
                    ).isSystem()
                ) {
                    return@Factory null
                }
                UnInstall(activity, itemInfo, view)
            }

        val PAUSE_APPS = SystemShortcut.Factory { activity: AutoCatLauncher, itemInfo: ItemInfo, originalView: View ->
            val targetCmp = itemInfo.targetComponent
            val packageName = targetCmp?.packageName ?: return@Factory null

            if (ApplicationInfoWrapper(
                    activity.asContext(),
                    packageName,
                    itemInfo.user,
                ).isSuspended()
            ) {
                return@Factory null
            }

            PauseApps(activity, itemInfo, originalView)
        }

        val ARCHIVE =
            SystemShortcut.Factory { activity: ActivityContext, itemInfo: ItemInfo, view: View ->
                if (itemInfo.targetComponent == null) {
                    return@Factory null
                }
                val wrapper = ApplicationInfoWrapper(
                    activity.asContext(),
                    itemInfo.targetComponent!!.packageName,
                    itemInfo.user,
                )
                if (wrapper.isSystem() || wrapper.isArchived()) {
                    return@Factory null
                }
                ArchiveApp(activity, itemInfo, view)
            }

        val DISABLE =
            SystemShortcut.Factory { activity: ActivityContext, itemInfo: ItemInfo, view: View ->
                if (itemInfo.targetComponent == null) {
                    return@Factory null
                }
                val wrapper = ApplicationInfoWrapper(
                    activity.asContext(),
                    itemInfo.targetComponent!!.packageName,
                    itemInfo.user,
                )
                if (wrapper.isSystem() || !wrapper.isEnabled()) {
                    return@Factory null
                }
                DisableApp(activity, itemInfo, view)
            }

        val ENABLE =
            SystemShortcut.Factory { activity: ActivityContext, itemInfo: ItemInfo, view: View ->
                if (itemInfo.targetComponent == null) {
                    return@Factory null
                }
                val wrapper = ApplicationInfoWrapper(
                    activity.asContext(),
                    itemInfo.targetComponent!!.packageName,
                    itemInfo.user,
                )
                if (wrapper.isEnabled()) {
                    return@Factory null
                }
                EnableApp(activity, itemInfo, view)
            }

        val UNARCHIVE =
            SystemShortcut.Factory { activity: ActivityContext, itemInfo: ItemInfo, view: View ->
                if (itemInfo.targetComponent == null) {
                    return@Factory null
                }
                if (!ApplicationInfoWrapper(
                        activity.asContext(),
                        itemInfo.targetComponent!!.packageName,
                        itemInfo.user,
                    ).isArchived()
                ) {
                    return@Factory null
                }
                RestoreApp(activity, itemInfo, view)
            }

        val APP_INFO =
            SystemShortcut.Factory { activity: ActivityContext, itemInfo: ItemInfo, view: View ->
                if (itemInfo.targetComponent == null) {
                    return@Factory null
                }
                AppInfo(activity, itemInfo, view)
            }

        val STORE_PAGE =
            SystemShortcut.Factory { activity: ActivityContext, itemInfo: ItemInfo, view: View ->
                val packageName = itemInfo.targetComponent?.packageName ?: return@Factory null
                val context = activity.asContext()
                val installerPackage = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        context.packageManager.getInstallSourceInfo(packageName).installingPackageName
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getInstallerPackageName(packageName)
                    }
                } catch (e: Exception) {
                    null
                }
                if (installerPackage == null) {
                    return@Factory null
                }
                // Only show for known stores
                if (installerPackage !in KNOWN_STORES) {
                    return@Factory null
                }
                StorePage(activity, itemInfo, view, installerPackage)
            }

        val AI_SUMMARY =
            SystemShortcut.Factory { activity: AutoCatLauncher, itemInfo: ItemInfo, view: View ->
                if (itemInfo.targetComponent == null) {
                    return@Factory null
                }
                AISummary(activity, itemInfo, view)
            }

        private val KNOWN_STORES = setOf(
            "com.android.vending", // Google Play Store
            "com.sec.android.app.samsungapps", // Galaxy Store
            "org.fdroid.fdroid", // F-Droid
            "org.fdroid.basic", // F-Droid Basic
            "com.aurora.store", // Aurora Store
            "com.amazon.venezia", // Amazon Appstore
        )
    }

    class Customize(
        private val launcher: AutoCatLauncher,
        private val appInfo: ModelAppInfo,
        itemInfo: ItemInfo,
        originalView: View,
    ) : SystemShortcut<AutoCatLauncher>(R.drawable.ic_edit, R.string.action_customize, launcher, itemInfo, originalView) {

        override fun onClick(v: View) {
            val outObj = Array<Any?>(1) { null }
            var icon = Utilities.loadFullDrawableWithoutTheme(launcher, appInfo, 0, 0, outObj)
            if (mItemInfo.screenId != NO_ID && icon is BitmapInfo.Extender) {
                // AutoCat-TODO-BubbleTea: Fix getThemedDrawable
                // icon = icon.getThemedDrawable(launcher)
            }
            val launcherActivityInfo = outObj[0] as LauncherActivityInfo?
            if (launcherActivityInfo != null) {
                val defaultTitle = launcherActivityInfo.label.toString()

                AbstractFloatingView.closeAllOpenViews(launcher)
                ComposeBottomSheet.show(
                    context = launcher,
                    contentPaddings = PaddingValues(bottom = 64.dp),
                ) {
                    CustomizeAppDialog(
                        icon = icon,
                        defaultTitle = defaultTitle,
                        componentKey = appInfo.toComponentKey(),
                    ) { close(true) }
                }
            } else {
                Toast.makeText(launcher, R.string.activity_not_found, Toast.LENGTH_SHORT).show()
                AbstractFloatingView.closeAllOpenViews(launcher)
            }
        }
    }

    class PauseApps(
        target: AutoCatLauncher,
        itemInfo: ItemInfo,
        originalView: View,
    ) : SystemShortcut<AutoCatLauncher>(
        R.drawable.ic_hourglass_top,
        R.string.paused_apps_drop_target_label,
        target,
        itemInfo,
        originalView,
    ) {
        @SuppressLint("NewApi")
        override fun onClick(view: View) {
            val context = view.context
            val appLabel = ApplicationInfoWrapper(
                context,
                mItemInfo.targetComponent?.packageName ?: "",
                mItemInfo.user,
            ).toString()
            AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_hourglass_top)
                .setTitle(context.getString(R.string.pause_apps_dialog_title, appLabel))
                .setMessage(context.getString(R.string.pause_apps_dialog_message, appLabel))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.pause) { _, _ ->
                    try {
                        AppGlobals.getPackageManager().setPackagesSuspendedAsUser(
                            arrayOf(mItemInfo.targetComponent?.packageName ?: ""),
                            true, null, null,
                            SuspendDialogInfo.Builder()
                                .setIcon(R.drawable.ic_hourglass_top)
                                .setTitle(R.string.paused_apps_dialog_title)
                                .setMessage(R.string.paused_apps_dialog_message)
                                .setNeutralButtonAction(SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
                                .build(),
                            0,
                            context.opPackageName,
                            context.userId,
                            mItemInfo.user.identifier,
                        )
                    } catch (e: Throwable) {
                        Log.e("AutoCatShortcut", "Failed to pause app", e)
                    }
                }
                .show()
            AbstractFloatingView.closeAllOpenViews(mTarget)
        }
    }

    class UnInstall(private var target: ActivityContext?, private var itemInfo: ItemInfo?, originalView: View?) :
        SystemShortcut<ActivityContext>(
            R.drawable.ic_uninstall_no_shadow,
            R.string.uninstall_drop_target_label,
            target,
            itemInfo,
            originalView,
        ) {

        /**
         * @return the component name that should be uninstalled or null.
         */
        private fun getUninstallTarget(item: ItemInfo?, context: Context): ComponentName? {
            var intent: Intent? = null
            var user: UserHandle? = null
            if (item != null &&
                (item.itemType == ITEM_TYPE_APPLICATION || item.itemType == ITEM_TYPE_TASK)
            ) {
                intent = item.intent
                user = item.user
            }
            if (intent != null) {
                val info: LauncherActivityInfo? =
                    context.getSystemService(LauncherApps::class.java)
                        ?.resolveActivity(intent, user)
                if (info != null && (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    return info.componentName
                }
            }
            return null
        }

        override fun onClick(view: View) {
            val cn = getUninstallTarget(itemInfo, view.context)
            if (cn == null) {
                // System applications cannot be installed. For now, show a toast explaining that.
                // We may give them the option of disabling apps this way.
                Toast.makeText(
                    view.context,
                    R.string.uninstall_system_app_text,
                    Toast.LENGTH_SHORT,
                ).show()
                return
            }
            try {
                val intent = Intent.parseUri(
                    view.context.getString(R.string.delete_package_intent),
                    0,
                )
                    .setData(
                        Uri.fromParts(
                            "package",
                            itemInfo?.targetComponent?.packageName,
                            itemInfo?.targetComponent?.className,
                        ),
                    )
                    .putExtra(Intent.EXTRA_USER, itemInfo?.user)
                target?.startActivitySafely(view, intent, itemInfo)
                AbstractFloatingView.closeAllOpenViews(target)
            } catch (e: URISyntaxException) {
                // Do nothing.
            }
        }
    }

    class ArchiveApp(
        private var target: ActivityContext?,
        private var itemInfo: ItemInfo?,
        originalView: View?,
    ) : SystemShortcut<ActivityContext>(
        R.drawable.ic_archive,
        R.string.archive_app_label,
        target,
        itemInfo,
        originalView,
    ) {
        init {
            mAccessibilityActionId = R.id.action_archive
        }

        override fun onClick(view: View) {
            val context = view.context
            val packageName = itemInfo?.targetComponent?.packageName ?: return
            val service = AppBatchOperationService(context)
            val appLabel = service.getAppLabel(packageName)

            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.archive_app_dialog_title, appLabel))
                .setMessage(R.string.archive_app_dialog_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.archive_app_label) { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = service.archiveApp(packageName)
                        when (result) {
                            is AppBatchOperationService.OperationResult.Success -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.archive_app_success, appLabel),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            is AppBatchOperationService.OperationResult.RequiresUserConfirmation -> {
                                context.startActivity(service.createUninstallIntent(packageName))
                            }

                            is AppBatchOperationService.OperationResult.Failed -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.archive_app_failed, appLabel),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            is AppBatchOperationService.OperationResult.Skipped -> {
                                Toast.makeText(
                                    context,
                                    R.string.archive_app_system_error,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                }
                .show()
            AbstractFloatingView.closeAllOpenViews(target)
        }
    }

    class DisableApp(
        private var target: ActivityContext?,
        private var itemInfo: ItemInfo?,
        originalView: View?,
    ) : SystemShortcut<ActivityContext>(
        R.drawable.ic_block_no_shadow,
        R.string.disable_app_label,
        target,
        itemInfo,
        originalView,
    ) {
        override fun onClick(view: View) {
            val context = view.context
            val packageName = itemInfo?.targetComponent?.packageName ?: return
            val service = AppBatchOperationService(context)
            val appLabel = service.getAppLabel(packageName)

            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.disable_app_dialog_title, appLabel))
                .setMessage(R.string.disable_app_dialog_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.disable_app_label) { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = service.disableApp(packageName)
                        when (result) {
                            is AppBatchOperationService.OperationResult.Success -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.disable_app_success, appLabel),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            is AppBatchOperationService.OperationResult.Failed -> {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.disable_app_failed, appLabel),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            else -> {}
                        }
                    }
                }
                .show()
            AbstractFloatingView.closeAllOpenViews(target)
        }
    }

    class EnableApp(
        private var target: ActivityContext?,
        private var itemInfo: ItemInfo?,
        originalView: View?,
    ) : SystemShortcut<ActivityContext>(
        R.drawable.ic_install_no_shadow,
        R.string.enable_app_label,
        target,
        itemInfo,
        originalView,
    ) {
        override fun onClick(view: View) {
            val context = view.context
            val packageName = itemInfo?.targetComponent?.packageName ?: return
            val service = AppBatchOperationService(context)
            val appLabel = service.getAppLabel(packageName)

            CoroutineScope(Dispatchers.Main).launch {
                val result = service.enableApp(packageName)
                when (result) {
                    is AppBatchOperationService.OperationResult.Success -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.enable_app_success, appLabel),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    is AppBatchOperationService.OperationResult.Failed -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.enable_app_failed, appLabel),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    else -> {}
                }
            }
            AbstractFloatingView.closeAllOpenViews(target)
        }
    }

    class AppInfo(
        private var target: ActivityContext?,
        private var itemInfo: ItemInfo?,
        originalView: View?,
    ) : SystemShortcut<ActivityContext>(
        R.drawable.ic_info_no_shadow,
        R.string.app_info_label,
        target,
        itemInfo,
        originalView,
    ) {
        override fun onClick(view: View) {
            val context = view.context
            val packageName = itemInfo?.targetComponent?.packageName ?: return
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("AutoCatShortcut", "Failed to open app info", e)
            }
            AbstractFloatingView.closeAllOpenViews(target)
        }
    }

    class StorePage(
        private var target: ActivityContext?,
        private var itemInfo: ItemInfo?,
        originalView: View?,
        private val installerPackage: String,
    ) : SystemShortcut<ActivityContext>(
        R.drawable.ic_storefront,
        R.string.store_page_label,
        target,
        itemInfo,
        originalView,
    ) {
        override fun onClick(view: View) {
            val context = view.context
            val packageName = itemInfo?.targetComponent?.packageName ?: return

            val intent = when (installerPackage) {
                "com.android.vending" -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                        setPackage("com.android.vending")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                "com.sec.android.app.samsungapps" -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/$packageName")).apply {
                        setPackage("com.sec.android.app.samsungapps")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                else -> {
                    // Generic fallback: try market:// URI with installer package
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                        setPackage(installerPackage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to web Play Store
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                    )
                } catch (e2: Exception) {
                    Toast.makeText(context, R.string.store_not_found, Toast.LENGTH_SHORT).show()
                }
            }
            AbstractFloatingView.closeAllOpenViews(target)
        }
    }

    class RestoreApp(
        private var target: ActivityContext?,
        private var itemInfo: ItemInfo?,
        originalView: View?,
    ) : SystemShortcut<ActivityContext>(
        R.drawable.ic_install_no_shadow,
        R.string.action_restore,
        target,
        itemInfo,
        originalView,
    ) {
        init {
            mAccessibilityActionId = R.id.action_unarchive
        }

        override fun onClick(view: View) {
            val context = view.context
            val packageName = itemInfo?.targetComponent?.packageName ?: return
            val service = AppBatchOperationService(context)
            val appLabel = service.getAppLabel(packageName)

            CoroutineScope(Dispatchers.Main).launch {
                val result = service.unarchiveApp(packageName)
                when (result) {
                    is AppBatchOperationService.OperationResult.Success -> {
                        Toast.makeText(
                            context,
                            "Restoring $appLabel...",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    is AppBatchOperationService.OperationResult.RequiresUserConfirmation -> {
                        // Launch the app, which usually triggers restore
                        target?.startActivitySafely(view, itemInfo?.intent, itemInfo)
                    }

                    is AppBatchOperationService.OperationResult.Failed -> {
                        Toast.makeText(
                            context,
                            "Failed to restore $appLabel",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    else -> {}
                }
            }
            AbstractFloatingView.closeAllOpenViews(target)
        }
    }

    class AISummary(
        private val launcher: AutoCatLauncher,
        private var itemInfo: ItemInfo?,
        originalView: View?,
    ) : SystemShortcut<AutoCatLauncher>(
        R.drawable.ic_autocat,
        R.string.ai_summary_label,
        launcher,
        itemInfo,
        originalView,
    ) {
        override fun onClick(view: View) {
            val packageName = itemInfo?.targetComponent?.packageName ?: return
            val appLabel = ApplicationInfoWrapper(
                view.context,
                packageName,
                itemInfo?.user,
            ).toString()

            AbstractFloatingView.closeAllOpenViews(launcher)
            ComposeBottomSheet.show(
                context = launcher,
                contentPaddings = PaddingValues(bottom = 32.dp),
            ) {
                AppSummaryDialog(
                    appName = appLabel,
                    packageName = packageName,
                    onClose = { close(true) },
                )
            }
        }
    }
}

@Composable
fun AppSummaryDialog(
    appName: String,
    packageName: String,
    onClose: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var summaryText by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var isLoading by remember { androidx.compose.runtime.mutableStateOf(true) }
    var errorMessage by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            val cacheSp = context.getSharedPreferences("autocat_app_summaries_cache", Context.MODE_PRIVATE)
            val cached = cacheSp.getString(packageName, null)
            if (!cached.isNullOrBlank()) {
                summaryText = cached
                isLoading = false
                return@withContext
            }

            try {
                val prefs = app.lawnchair.preferences.PreferenceManager.getInstance(context)
                val googleKey = prefs.llmGoogleAIKey.get()
                val claudeKey = prefs.llmClaudeKey.get()
                val openAIKey = prefs.llmOpenAIKey.get()
                val perplexityKey = prefs.llmPerplexityKey.get()

                val activeProviderId = prefs.llmProviderPreference.get()
                val provider = when (activeProviderId) {
                    "google_ai" -> if (googleKey.isNotBlank()) app.lawnchair.categorization.llm.GoogleAIProvider(context) else null
                    "claude" -> if (claudeKey.isNotBlank()) app.lawnchair.categorization.llm.ClaudeProvider(context) else null
                    "openai" -> if (openAIKey.isNotBlank()) app.lawnchair.categorization.llm.OpenAIProvider(context) else null
                    "perplexity" -> if (perplexityKey.isNotBlank()) app.lawnchair.categorization.llm.PerplexityProvider(context) else null
                    else -> null
                } ?: run {
                    when {
                        googleKey.isNotBlank() -> app.lawnchair.categorization.llm.GoogleAIProvider(context)
                        claudeKey.isNotBlank() -> app.lawnchair.categorization.llm.ClaudeProvider(context)
                        openAIKey.isNotBlank() -> app.lawnchair.categorization.llm.OpenAIProvider(context)
                        perplexityKey.isNotBlank() -> app.lawnchair.categorization.llm.PerplexityProvider(context)
                        else -> null
                    }
                }

                if (provider == null) {
                    errorMessage = "No AI provider configured. Add an API key in AutoCat Settings."
                    isLoading = false
                    return@withContext
                }

                val prompt = "Provide a concise 2-sentence summary of what the Android application '$appName' (package: $packageName) is used for, its primary feature set, and category/genre."
                val result = provider.categorizeApp(appName, packageName, prompt, listOf("Summary"))
                val text = result.reasoning?.takeIf { it.isNotBlank() }
                    ?: "$appName is a mobile application. Categorized under: ${result.category}."
                summaryText = text
                cacheSp.edit().putString(packageName, text).apply()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to generate summary: ${e.message}"
                isLoading = false
            }
        }
    }

    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(16.dp),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(20.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = androidx.compose.ui.Modifier.size(24.dp),
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                androidx.compose.material3.Text(
                    text = "$appName Summary",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    modifier = androidx.compose.ui.Modifier.weight(1f),
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

            if (isLoading) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(24.dp),
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = androidx.compose.ui.Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                    androidx.compose.material3.Text(
                        text = "Generating AI Summary...",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (errorMessage != null) {
                androidx.compose.material3.Text(
                    text = errorMessage!!,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                )
            } else {
                androidx.compose.material3.Text(
                    text = summaryText ?: "No summary available.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))

            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            ) {
                if (summaryText != null) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val cacheSp = context.getSharedPreferences("autocat_app_summaries_cache", Context.MODE_PRIVATE)
                            cacheSp.edit().remove(packageName).apply()
                            summaryText = null
                            isLoading = true
                            errorMessage = null
                        },
                    ) {
                        androidx.compose.material3.Text("Regenerate")
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(4.dp))
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("App Summary", summaryText)
                            clipboard?.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Copied summary to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        androidx.compose.material3.Text("Copy")
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                androidx.compose.material3.Button(onClick = onClose) {
                    androidx.compose.material3.Text("Close")
                }
            }
        }
    }
}

