package app.lawnchair.archival

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import app.lawnchair.appops.AppBatchOperationService
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.preferences.PreferenceManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Predictive Archive Service - P0 Feature.
 *
 * Uses UsageStatsManager + AI "Auto-Pilot" to identify apps that can be
 * safely archived to save space without impacting user experience.
 */
class PredictiveArchiveService(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager,
    private val packageManager: PackageManager = context.packageManager,
    private val categorizationManager: CategorizationManager = CategorizationManager.getInstance(context),
    private val metadataProvider: AppMetadataProvider = AppMetadataProvider(context),
    private val batchService: AppBatchOperationService = AppBatchOperationService(context),
    private val prefs: PreferenceManager = PreferenceManager.getInstance(context),
    private val tabDao: TabDao = TabDatabase.getInstance(context).tabDao(),
) {

    companion object {
        private const val TAG = "PredictiveArchive"
        private const val DEFAULT_UNUSED_THRESHOLD_DAYS = 30L

        // Importance scores for categories (higher = less likely to archive)
        private val CATEGORY_IMPORTANCE = mapOf(
            "Finance" to 0.9f,
            "Work" to 0.85f,
            "Productivity" to 0.8f,
            "Essentials" to 0.95f,
            "Communication" to 0.8f,
            "Utilities" to 0.7f,
            "Navigation" to 0.75f,
            "Shopping" to 0.4f,
            "Games" to 0.2f,
            "Entertainment" to 0.3f,
            "Social" to 0.5f,
            "News" to 0.4f,
        )
    }

    /**
     * Represents a candidate app suggested for archiving.
     */
    data class ArchiveCandidate(
        val packageName: String,
        val appLabel: String,
        val lastUsedMs: Long,
        val daysUnused: Int,
        val importanceScore: Float, // 0.0 to 1.0 (1.0 = highly important)
        val category: String?,
        val sizeBytes: Long = 0,
    )

    /**
     * Scans for archive candidates based on usage and AI scoring.
     */
    suspend fun getArchiveCandidates(): List<ArchiveCandidate> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return@withContext emptyList()

        val thresholdDays = prefs.predictiveArchiveThreshold.get().toLong().takeIf { it > 0 }
            ?: DEFAULT_UNUSED_THRESHOLD_DAYS
        val thresholdMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(thresholdDays)

        val apps = metadataProvider.getInstalledApps()
        val candidates = mutableListOf<ArchiveCandidate>()

        // Get usage stats for the threshold period
        val stats = usageStatsManager.queryAndAggregateUsageStats(thresholdMs, System.currentTimeMillis())

        for (app in apps) {
            val packageName = app.packageName

            // Skip system apps or the launcher itself
            if (batchService.isSystemApp(packageName) || packageName == context.packageName) continue

            val appStats = stats[packageName]
            val lastUsed = appStats?.lastTimeUsed ?: 0L

            if (lastUsed < thresholdMs) {
                val daysUnused = if (lastUsed == 0L) {
                    thresholdDays.toInt() + 1 // Never used in tracked history
                } else {
                    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUsed).toInt()
                }

                // AI Scoring Phase
                val category = getAppCategory(packageName)
                val importance = calculateImportance(packageName, category, daysUnused)

                // Only suggest if importance is below threshold (e.g., 0.6)
                if (importance < 0.6f) {
                    candidates.add(
                        ArchiveCandidate(
                            packageName = packageName,
                            appLabel = app.label,
                            lastUsedMs = lastUsed,
                            daysUnused = daysUnused,
                            importanceScore = importance,
                            category = category,
                            sizeBytes = getAppSize(packageName),
                        ),
                    )
                }
            }
        }

        candidates.sortedBy { it.importanceScore }
    }

    private suspend fun getAppCategory(packageName: String): String? {
        return tabDao.getAppTab(packageName)?.tabName
    }

    private fun calculateImportance(packageName: String, category: String?, daysUnused: Int): Float {
        var score = CATEGORY_IMPORTANCE[category] ?: 0.5f

        // Decay score further for extremely long periods of inactivity
        if (daysUnused > 90) {
            score *= 0.5f
        } else if (daysUnused > 60) {
            score *= 0.75f
        }

        // Future: Ask LLM for a refined importance score based on app purpose

        return score
    }

    private fun getAppSize(packageName: String): Long {
        return try {
            val file = java.io.File(packageManager.getApplicationInfo(packageName, 0).publicSourceDir)
            file.length()
        } catch (e: Exception) {
            0L
        }
    }
}
