/*
 * Copyright 2021, AutoCat
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

package app.lawnchair

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.lawnchair.backup.AutoCatBackup
import app.lawnchair.bugreport.BugReportActivity
import app.lawnchair.flowerpot.Flowerpot
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.ui.ModalBottomSheetContent
import app.lawnchair.ui.preferences.destinations.openAppInfo
import app.lawnchair.update.UpdateChannel
import app.lawnchair.update.UpdateChecker
import app.lawnchair.util.restartLauncher
import app.lawnchair.util.unsafeLazy
import app.lawnchair.views.ComposeBottomSheet
import com.android.launcher3.BuildConfig
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.quickstep.RecentsActivity
import com.android.systemui.shared.system.QuickStepContract
import com.patrykmichalik.opto.core.firstBlocking
import io.sentry.android.core.SentryAndroid
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AutoCat Application class with optimized startup performance.
 *
 * Performance optimizations:
 * - Lazy initialization of non-critical components
 * - Deferred initialization when not default launcher
 * - Background thread initialization for heavy operations
 */
class AutoCatApp : Application() {
    private val compatible = Build.VERSION.SDK_INT in BuildConfig.QUICKSTEP_MIN_SDK..BuildConfig.QUICKSTEP_MAX_SDK
    private val isRecentsComponent: Boolean by unsafeLazy { checkRecentsComponent() }
    private val recentsEnabled: Boolean get() = compatible && isRecentsComponent
    private val isAtleastT = Utilities.ATLEAST_T
    internal var accessibilityService: AutoCatAccessibilityService? = null
    val isVibrateOnIconAnimation: Boolean by unsafeLazy { getSystemUiBoolean("config_vibrateOnIconAnimation", false) }

    // Lazy initialization flags to avoid unnecessary work during startup
    private var _isDefaultLauncher: Boolean? = null
    private var sentryInitialized: Boolean = false
    private var flowerpotInitialized: Boolean = false

    /**
     * Check if AutoCat is the default launcher.
     * Cached after first check to avoid repeated PackageManager queries.
     */
    val isDefaultLauncher: Boolean
        get() {
            if (_isDefaultLauncher == null) {
                _isDefaultLauncher = checkIsDefaultLauncher()
            }
            return _isDefaultLauncher!!
        }

    private fun checkIsDefaultLauncher(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == packageName
        } catch (e: Exception) {
            Log.w(TAG, "Error checking default launcher", e)
            false
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Only initialize heavy components if we're the default launcher
        // This prevents unnecessary resource usage when AutoCat is not active
        if (isDefaultLauncher) {
            initializeCriticalComponents()
        } else {
            Log.d(TAG, "AutoCat is not the default launcher - deferring initialization")
            // Still set up minimal state needed for potential future activation
            QuickStepContract.sRecentsDisabled = !recentsEnabled
        }
    }

    /**
     * Initialize critical components for when AutoCat is the default launcher.
     * Runs on main thread but defers non-critical work to background.
     */
    private fun initializeCriticalComponents() {
        // Initialize Sentry first for crash reporting
        if (BuildConfig.SENTRY_DSN.isNotEmpty() && !sentryInitialized) {
            try {
                SentryAndroid.init(this) { options ->
                    options.dsn = BuildConfig.SENTRY_DSN
                    options.tracesSampleRate = 0.1 // Reduced from 1.0 for performance
                    options.sampleRate = 0.1
                }
                sentryInitialized = true
                Log.d(TAG, "Sentry initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Sentry", e)
            }
        }

        // Set up instance state
        QuickStepContract.sRecentsDisabled = !recentsEnabled

        // Defer non-critical initialization to background thread
        CoroutineScope(Dispatchers.IO).launch {
            initializeBackgroundComponents()
        }
    }

    /**
     * Initialize non-critical components on background thread.
     * This prevents blocking the main thread during cold start.
     */
    private fun initializeBackgroundComponents() {
        try {
            // Initialize Flowerpot Manager (theming)
            if (!flowerpotInitialized) {
                Flowerpot.Manager.getInstance(this@AutoCatApp)
                flowerpotInitialized = true
                Log.d(TAG, "Flowerpot Manager initialized")
            }

            // Initialize App Drawer Cache for performance
            try {
                app.lawnchair.allapps.AppDrawerCache.initialize(this@AutoCatApp)
                Log.d(TAG, "App Drawer Cache initialized")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize App Drawer Cache", e)
            }

            // Check for crash reports (deferred from main thread)
            try {
                val preferenceManager2 = PreferenceManager2.getInstance(this@AutoCatApp)
                val lastCrashId = preferenceManager2.lastCrashId.get().first()
                val showLocalUi = preferenceManager2.showLocalCrashUi.get().first()
                if (lastCrashId != -1 && showLocalUi) {
                    CoroutineScope(Dispatchers.Main).launch {
                        BugReportActivity.show(this@AutoCatApp, lastCrashId)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking crash reports", e)
            }

            // Check for app updates
            try {
                checkForUpdates()
            } catch (e: Exception) {
                Log.w(TAG, "Error during update check", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in background initialization", e)
        }
    }

    private suspend fun checkForUpdates() {
        val prefs = PreferenceManager2.getInstance(this@AutoCatApp)
        val autoUpdateEnabled = prefs.autoUpdateEnabled.get().first()
        if (!autoUpdateEnabled) return

        val lastCheck = prefs.lastUpdateCheckTime.get().first()
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L
        if (now - lastCheck < twentyFourHours) {
            Log.d(TAG, "Skipping update check — last check was <24h ago")
            return
        }

        val channelStr = prefs.updateChannel.get().first()
        val channel = if (channelStr == "dev") UpdateChannel.DEV else UpdateChannel.STABLE
        val update = UpdateChecker.checkForUpdate(channel)

        // Record check time regardless of result
        CoroutineScope(Dispatchers.IO).launch {
            prefs.lastUpdateCheckTime.set(now)
        }

        if (update != null) {
            Log.i(TAG, "Update available: ${update.versionName}")
            showUpdateNotification(update.versionName, update.downloadUrl)
        } else {
            Log.d(TAG, "No update available")
        }
    }

    private fun showUpdateNotification(versionName: String, downloadUrl: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "autocat_updates"
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                "AutoCat Updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifications about new AutoCat releases"
            }
        )

        val openIntent = PendingIntent.getActivity(
            this@AutoCatApp,
            0,
            Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this@AutoCatApp, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("AutoCat update available")
            .setContentText("$versionName is ready to download")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    /**
     * Called when launcher state changes (e.g., user sets AutoCat as default).
     * Ensures all components are initialized when needed.
     */
    fun ensureInitialized() {
        if (!isDefaultLauncher) {
            _isDefaultLauncher = checkIsDefaultLauncher()
        }

        if (isDefaultLauncher && !sentryInitialized) {
            initializeCriticalComponents()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        app.lawnchair.categorization.AutoCatAppProvider.cleanup()
        app.lawnchair.categorization.llm.LLMLogger.cleanup()
    }

    fun hideClockInStatusBar() {
        if (!isRecentsEnabled) return
        try {
            val currentBlacklist = Settings.Secure.getString(contentResolver, "icon_blacklist") ?: ""
            val newBlacklist = if (currentBlacklist.contains("clock")) {
                currentBlacklist
            } else {
                "$currentBlacklist,clock"
            }
            Settings.Secure.putString(contentResolver, "icon_blacklist", newBlacklist)
        } catch (_: Exception) {
            // ignore
        }
    }

    fun restoreClockInStatusBar() {
        if (!isRecentsEnabled) return
        try {
            val currentBlacklist = Settings.Secure.getString(contentResolver, "icon_blacklist") ?: ""
            val newBlacklist = currentBlacklist.split(",").filter { it != "clock" }.joinToString(",")
            Settings.Secure.putString(contentResolver, "icon_blacklist", newBlacklist)
        } catch (_: Exception) {
        }
    }

    fun onLauncherAppStateCreated() {
        registerActivityLifecycleCallbacks(activityHandler)
    }

    fun restart(recreateLauncher: Boolean = true) {
        if (recreateLauncher) {
            activityHandler.finishAll()
        } else {
            restartLauncher(this)
        }
    }

    fun renameRestoredDb(dbName: String) {
        val restoredDbFile = getDatabasePath(AutoCatBackup.RESTORED_DB_FILE_NAME)
        if (!restoredDbFile.exists()) return
        val dbFile = getDatabasePath(dbName)
        restoredDbFile.renameTo(dbFile)
    }

    fun migrateDbName(dbName: String) {
        val dbFile = getDatabasePath(dbName)
        if (dbFile.exists()) return
        val prefs = PreferenceManager.INSTANCE.get(this)
        val dbJournalFile = getJournalFile(dbFile)
        val oldDbSlot = prefs.sp.getString("pref_currentDbSlot", "a")
        val oldDbName = if (oldDbSlot == "a") "launcher.db" else "launcher.db_b"
        val oldDbFile = getDatabasePath(oldDbName)
        val oldDbJournalFile = getJournalFile(oldDbFile)
        if (oldDbFile.exists()) {
            oldDbFile.copyTo(dbFile)
            oldDbJournalFile.copyTo(dbJournalFile)
            oldDbFile.delete()
            oldDbJournalFile.delete()
        }
    }

    fun cleanUpDatabases() {
        val idp = InvariantDeviceProfile.INSTANCE.get(this)
        val dbName = idp.dbFile
        val dbFile = getDatabasePath(dbName)
        dbFile?.parentFile?.listFiles()?.forEach { file ->
            val name = file.name
            if (name.startsWith("launcher") && !name.startsWith(dbName)) {
                file.delete()
            }
        }
    }

    private fun getJournalFile(file: File): File = File(file.parentFile, "${file.name}-journal")

    private fun getSystemUiBoolean(resName: String, fallback: Boolean): Boolean {
        val systemUiPackage = "com.android.systemui"
        val res = packageManager.getResourcesForApplication(systemUiPackage)

        @SuppressLint("DiscouragedApi")
        val resId = res.getIdentifier(resName, "bool", systemUiPackage)
        if (resId == 0) {
            return fallback
        }
        return res.getBoolean(resId)
    }

    private val activityHandler = object : ActivityLifecycleCallbacks {
        private val activities = HashSet<Activity>()
        private var foregroundActivity: Activity? = null

        fun finishAll() {
            HashSet(activities).forEach { it.finish() }
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {
            foregroundActivity = activity
        }

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityDestroyed(activity: Activity) {
            if (activity == foregroundActivity) foregroundActivity = null
            activities.remove(activity)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activities.add(activity)
        }
    }

    private fun checkRecentsComponent(): Boolean {
        @SuppressLint("DiscouragedApi")
        val resId = resources.getIdentifier("config_recentsComponentName", "string", "android")
        if (resId == 0) {
            Log.d(TAG, "config_recentsComponentName not found, disabling recents")
            return false
        }

        val recentsComponent = ComponentName.unflattenFromString(resources.getString(resId))
        if (recentsComponent == null) {
            Log.d(TAG, "config_recentsComponentName is empty, disabling recents")
            return false
        }

        val isRecentsComponent = recentsComponent.packageName == packageName &&
            recentsComponent.className == RecentsActivity::class.java.name
        if (!isRecentsComponent) {
            Log.d(TAG, "config_recentsComponentName ($recentsComponent) is not Lawnchair, disabling recents")
            return false
        }

        return true
    }

    fun isAccessibilityServiceBound(): Boolean = accessibilityService != null

    fun performGlobalAction(action: Int): Boolean {
        return accessibilityService?.performGlobalAction(action) ?: run {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .let(::startActivity)
            false
        }
    }

    companion object {
        private const val TAG = "AutoCatApp"
        private const val UPDATE_NOTIFICATION_ID = 7001

        @JvmStatic
        lateinit var instance: AutoCatApp
            private set

        @JvmStatic
        val isRecentsEnabled: Boolean get() = instance.recentsEnabled

        @JvmStatic
        val isAtleastT: Boolean get() = instance.isAtleastT

        @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
        fun Launcher.showQuickstepWarningIfNecessary() {
            val launcher = this
            if (!autoCatApp.isRecentsComponent || isRecentsEnabled) return
            ComposeBottomSheet.show(this) {
                ModalBottomSheetContent(
                    title = { Text(text = stringResource(id = R.string.quickstep_incompatible)) },
                    text = {
                        val description = stringResource(
                            id = R.string.quickstep_incompatible_description,
                            stringResource(id = R.string.derived_app_name),
                            Build.VERSION.RELEASE,
                        )
                        Text(text = description)
                    },
                    buttons = {
                        OutlinedButton(
                            onClick = {
                                openAppInfo(launcher)
                                close(true)
                            },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(text = stringResource(id = R.string.app_info_drop_target_label))
                        }
                        Spacer(modifier = Modifier.requiredWidth(8.dp))
                        Button(
                            onClick = { close(true) },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    },
                )
            }
        }

        fun getUriForFile(context: Context, file: File): Uri {
            return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        }
    }
}

val Context.autoCatApp get() = applicationContext as AutoCatApp
