package app.lawnchair.categorization.stages

import android.content.Context
import android.util.Log
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.entities.AppTab
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * On-device ML categorizer using TensorFlow Lite.
 *
 * This is Stage 2 of categorization (between LLM and built-in),
 * running fully offline without requiring an API key.
 *
 * When a trained TFLite model is present at assets/ml/autocat_categorizer.tflite,
 * it uses neural inference. Otherwise it falls back to a keyword-based classifier
 * which covers hundreds of popular apps and common naming patterns.
 *
 * Model format (when present):
 *   Input:  float[1][INPUT_SIZE]  — character-frequency feature vector
 *   Output: float[1][CATEGORY_COUNT] — category probability distribution
 *
 * Confidence: 0.60 (medium — lower than LLM, higher than built-in fallback)
 */
class MLCategorizer(
    private val context: Context,
    private val categoryDao: TabDao,
) {
    private var interpreter: Interpreter? = null

    init {
        tryLoadModel()
    }

    // ─── TFLite model loading ───────────────────────────────────────────────

    private fun tryLoadModel() {
        try {
            val modelBuffer = loadModelFromAssets() ?: return
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "TFLite model loaded from assets")
        } catch (e: Exception) {
            Log.i(TAG, "No TFLite model found – using keyword classifier fallback")
        }
    }

    private fun loadModelFromAssets(): MappedByteBuffer? {
        return try {
            val file = File(context.cacheDir, "autocat_model_tmp.tflite")
            context.assets.openFd(MODEL_ASSET_PATH).use { assetFd ->
                assetFd.createInputStream().use { inputStream ->
                    file.outputStream().use { out -> inputStream.copyTo(out) }
                }
            }
            val buffer = file.inputStream().channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            file.delete()
            buffer
        } catch (e: Exception) {
            LLMLogger.logWarning("MLCategorizer", "MODEL_LOAD", "TFLite model not found in assets, using keyword classifier", mapOf("error" to (e.message ?: "unknown")))
            null
        }
    }

    // ─── Batch categorize ──────────────────────────────────────────────────

    suspend fun categorizeBatch(apps: List<AppInfo>): Int = withContext(Dispatchers.IO) {
        val customTabs = categoryDao.getVisibleCustomTabs()
        if (customTabs.isEmpty()) return@withContext 0

        val tabNames = customTabs.map { it.name }
        var count = 0

        val appTabs = apps.mapNotNull { app ->
            val tabName = classify(app, tabNames) ?: return@mapNotNull null
            count++
            AppTab(
                packageName = app.packageName,
                tabName = tabName,
                confidence = CONFIDENCE_ML,
                source = AppTab.SOURCE_ML,
                isUserOverride = false,
                reasoning = if (interpreter != null) "On-device ML model" else "Keyword classifier",
            )
        }

        if (appTabs.isNotEmpty()) {
            categoryDao.insertAppTabs(appTabs)
        }
        count
    }

    suspend fun categorize(appInfo: AppInfo): Boolean = withContext(Dispatchers.IO) {
        val customTabs = categoryDao.getVisibleCustomTabs()
        if (customTabs.isEmpty()) return@withContext false

        val tabNames = customTabs.map { it.name }
        val tabName = classify(appInfo, tabNames) ?: return@withContext false

        val appTab = AppTab(
            packageName = appInfo.packageName,
            tabName = tabName,
            confidence = CONFIDENCE_ML,
            source = AppTab.SOURCE_ML,
            isUserOverride = false,
            reasoning = if (interpreter != null) "On-device ML model" else "Keyword classifier",
        )
        categoryDao.insertAppTab(appTab)
        true
    }

    // ─── Classification ─────────────────────────────────────────────────────

    private fun classify(appInfo: AppInfo, availableTabs: List<String>): String? {
        interpreter?.let { interp ->
            val result = runTFLiteInference(interp, appInfo, availableTabs)
            if (result != null) return result
        }
        return keywordClassify(appInfo, availableTabs)
    }

    // ─── TFLite inference ───────────────────────────────────────────────────

    private fun runTFLiteInference(
        interp: Interpreter,
        appInfo: AppInfo,
        availableTabs: List<String>,
    ): String? {
        return try {
            val inputBuffer = buildFeatureVector(appInfo)
            val outputBuffer = ByteBuffer.allocateDirect(CATEGORY_COUNT * 4)
                .apply { order(ByteOrder.nativeOrder()) }

            interp.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val probabilities = FloatArray(CATEGORY_COUNT) { outputBuffer.float }

            val bestIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: return null
            if (probabilities[bestIndex] < TFLITE_MIN_CONFIDENCE) return null

            val predictedCategory = MODEL_CATEGORIES.getOrNull(bestIndex) ?: return null
            findBestMatchingTab(predictedCategory, availableTabs)
        } catch (e: Exception) {
            Log.w(TAG, "TFLite inference failed for ${appInfo.packageName}: ${e.message}")
            LLMLogger.logWarning("MLCategorizer", "TFLITE_INFERENCE", "TFLite inference failed, falling back to keyword classifier", mapOf("package" to appInfo.packageName, "error" to (e.message ?: "unknown")))
            null
        }
    }

    /**
     * Builds a fixed-length character-frequency feature vector for the model.
     * Encodes the app label and package name as a float[INPUT_SIZE] buffer.
     */
    private fun buildFeatureVector(appInfo: AppInfo): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * 4)
            .apply { order(ByteOrder.nativeOrder()) }

        val text = "${appInfo.label} ${appInfo.packageName}".lowercase()
        val freq = FloatArray(INPUT_SIZE)

        // Character frequency features (a-z = indices 0-25, 0-9 = indices 26-35)
        for (ch in text) {
            val idx = when {
                ch in 'a'..'z' -> ch - 'a'
                ch in '0'..'9' -> 26 + (ch - '0')
                else -> -1
            }
            if (idx >= 0) freq[idx] = (freq[idx] + 1f / text.length.coerceAtLeast(1))
        }

        freq.forEach { buffer.putFloat(it) }
        buffer.rewind()
        return buffer
    }

    // ─── Keyword-based fallback classifier ──────────────────────────────────

    private fun keywordClassify(appInfo: AppInfo, availableTabs: List<String>): String? {
        val pkg = appInfo.packageName.lowercase()
        val label = appInfo.label.lowercase()
        val combined = "$label $pkg"

        val predicted = KEYWORD_RULES.entries.firstOrNull { (_, patterns) ->
            patterns.any { pattern -> combined.contains(pattern) }
        }?.key ?: return null

        return findBestMatchingTab(predicted, availableTabs)
    }

    /**
     * Maps a predicted category to the closest matching user-defined tab.
     * Uses simple substring matching — e.g., "Social" matches "Social Media".
     */
    private fun findBestMatchingTab(predicted: String, tabs: List<String>): String? {
        // Exact match first
        tabs.firstOrNull { it.equals(predicted, ignoreCase = true) }?.let { return it }
        // Partial match: tab contains predicted OR predicted contains tab
        tabs.firstOrNull { tab ->
            tab.contains(predicted, ignoreCase = true) ||
                predicted.contains(tab, ignoreCase = true)
        }?.let { return it }
        return null
    }

    companion object {
        private const val TAG = "MLCategorizer"
        private const val MODEL_ASSET_PATH = "ml/autocat_categorizer.tflite"
        private const val CONFIDENCE_ML = 0.60f
        private const val TFLITE_MIN_CONFIDENCE = 0.50f

        // Feature vector size — must match the model's input shape
        private const val INPUT_SIZE = 36 // 26 letters + 10 digits

        // Category labels — must match the model's output order
        val MODEL_CATEGORIES = listOf(
            "Social", "Entertainment", "Productivity", "Games",
            "Photography", "Music", "News", "Navigation",
            "Finance", "Health", "Shopping", "Education",
            "Communication", "Tools",
        )
        private val CATEGORY_COUNT = MODEL_CATEGORIES.size

        /**
         * Keyword rules: category → list of substrings to match against
         * "label packageName" combined string (lowercase).
         */
        val KEYWORD_RULES: Map<String, List<String>> = linkedMapOf(
            "Social" to listOf(
                "instagram", "facebook", "twitter", "tiktok", "snapchat",
                "pinterest", "reddit", "tumblr", "linkedin", "discord",
                "telegram", "whatsapp", "signal", "mastodon", "threads",
                "social", "dating", "tinder", "bumble", "hinge",
            ),
            "Communication" to listOf(
                "messenger", "messages", "sms", "chat", "email",
                "gmail", "outlook", "mail", "slack", "teams",
                "zoom", "meet", "skype", "viber", "hangouts",
                "inbox", "mms", "communicate",
            ),
            "Music" to listOf(
                "spotify", "music", "soundcloud", "pandora", "deezer",
                "tidal", "amazon music", "youtube music", "shazam",
                "podcast", "audible", "radio", "tunein", "lastfm",
            ),
            "Entertainment" to listOf(
                "netflix", "hulu", "disney", "hbo", "paramount",
                "youtube", "twitch", "peacock", "video", "stream",
                "crunchyroll", "funimation", "pluto", "tubi",
                "entertainment", "watch", "movie", "tv",
            ),
            "Games" to listOf(
                "game", "games", "gaming", "puzzle", "chess",
                "candy", "clash", "minecraft", "roblox", "fortnite",
                "pokemon", "play store game", "arcade", "rpg",
                ".game", "casino", "slot", "sudoku",
            ),
            "Photography" to listOf(
                "camera", "photo", "gallery", "pictures", "snapseed",
                "lightroom", "vsco", "picsart", "facetune", "lens",
                "portrait", "filter", "image editor", "screenshot",
            ),
            "Navigation" to listOf(
                "maps", "gps", "navigation", "waze", "uber",
                "lyft", "transit", "directions", "compass", "radar",
                "traffic", "mapbox", "here maps",
            ),
            "Finance" to listOf(
                "bank", "finance", "wallet", "pay", "paypal",
                "venmo", "cashapp", "robinhood", "mint", "coinbase",
                "crypto", "bitcoin", "stocks", "invest", "budget",
                "insurance", "tax", "expense",
            ),
            "Health" to listOf(
                "health", "fitness", "workout", "gym", "run",
                "yoga", "meditation", "sleep", "diet", "calories",
                "doctor", "medical", "pharmacy", "strava", "fitbit",
                "myfitnesspal", "headspace", "calm",
            ),
            "Shopping" to listOf(
                "amazon", "shop", "shopping", "ebay", "etsy",
                "aliexpress", "walmart", "target", "store", "buy",
                "market", "deals", "coupon", "order",
            ),
            "Productivity" to listOf(
                "calendar", "todo", "notes", "task", "reminder",
                "notion", "trello", "asana", "evernote", "onenote",
                "office", "word", "excel", "sheets", "docs",
                "pdf", "scanner", "productivity", "planner",
            ),
            "Education" to listOf(
                "learn", "study", "education", "school", "course",
                "duolingo", "khan", "coursera", "udemy", "quiz",
                "language", "math", "tutor", "university",
            ),
            "News" to listOf(
                "news", "article", "feed", "rss", "headlines",
                "bbc", "cnn", "nytimes", "guardian", "flipboard",
                "pocket", "feedly",
            ),
            "Tools" to listOf(
                "tool", "utility", "settings", "system", "battery",
                "cleaner", "antivirus", "vpn", "file manager",
                "calculator", "flashlight", "clock", "timer",
                "backup", "launcher", "keyboard",
            ),
        )
    }
}
