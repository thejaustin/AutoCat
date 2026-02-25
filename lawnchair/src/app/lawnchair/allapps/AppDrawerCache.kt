package app.lawnchair.allapps

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import app.lawnchair.data.apps.AppInfo
import com.android.launcher3.model.data.AppInfo as LauncherAppInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance cache for app drawer data.
 * 
 * Features:
 * - LRU cache for icon bitmaps
 * - Concurrent hash maps for thread-safe app data
 * - Automatic memory management based on available heap
 * 
 * Performance benefits:
 * - Eliminates redundant ML categorization calls
 * - Caches converted app info to avoid repeated transformations
 * - Icon bitmap caching reduces drawable loading time
 */
object AppDrawerCache {
    
    /**
     * LRU cache for app icons.
     * Memory size is calculated as 1/8th of available heap, max 16MB.
     */
    private var iconCache: LruCache<String, Bitmap>? = null
    
    /**
     * Cache for categorized apps by tab.
     * Key: Tab name, Value: Map of category to apps
     */
    private val categorizedAppsCache = ConcurrentHashMap<String, Map<String, List<AppInfo>>>()
    
    /**
     * Cache for converted app info.
     * Key: Package name, Value: AutoCat AppInfo
     */
    private val appInfoCache = ConcurrentHashMap<String, AppInfo>()
    
    /**
     * Timestamp of last categorization to avoid repeated ML calls.
     */
    private var lastCategorizationTime: Long = 0
    
    /**
     * Cache validity duration (5 minutes).
     * Prevents excessive ML API calls while keeping data fresh.
     */
    private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L
    
    /**
     * Initialize caches with appropriate sizes.
     * Should be called during app initialization.
     */
    fun initialize(context: Context) {
        val memoryClass = (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
            .memoryClass
        
        // Use 1/8th of available memory class for icon cache
        val cacheSize = memoryClass * 1024 * 1024 / 8
        val cappedSize = minOf(cacheSize, 16 * 1024 * 1024) // Max 16MB
        
        iconCache = object : LruCache<String, Bitmap>(cappedSize.toInt()) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount
            }
            
            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                if (evicted && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }
    
    /**
     * Get icon from cache.
     * @param key Unique identifier (package name + user)
     * @return Cached bitmap or null
     */
    fun getIcon(key: String): Bitmap? = iconCache?.get(key)
    
    /**
     * Put icon in cache.
     * @param key Unique identifier
     * @param bitmap Icon bitmap
     */
    fun putIcon(key: String, bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        iconCache?.put(key, bitmap)
    }
    
    /**
     * Get categorized apps from cache.
     * @return Cached categorized apps or null if expired/empty
     */
    fun getCategorizedApps(): Map<String, Map<String, List<AppInfo>>>? {
        if (System.currentTimeMillis() - lastCategorizationTime > CACHE_VALIDITY_MS) {
            return null // Cache expired
        }
        return categorizedAppsCache.toMap()
    }
    
    /**
     * Cache categorized apps.
     * @param apps Categorized apps by tab
     */
    fun cacheCategorizedApps(apps: Map<String, Map<String, List<AppInfo>>>) {
        categorizedAppsCache.clear()
        categorizedAppsCache.putAll(apps)
        lastCategorizationTime = System.currentTimeMillis()
    }
    
    /**
     * Get converted app info from cache.
     * @param packageName Package name key
     * @return Cached AppInfo or null
     */
    fun getAppInfo(packageName: String): AppInfo? = appInfoCache[packageName]
    
    /**
     * Cache converted app info.
     * @param appInfo App info to cache
     */
    fun cacheAppInfo(appInfo: AppInfo) {
        if (appInfo.packageName.isNotEmpty()) {
            appInfoCache[appInfo.packageName] = appInfo
        }
    }
    
    /**
     * Cache multiple app infos at once.
     * @param appInfos List of app infos to cache
     */
    fun cacheAppInfos(appInfos: List<AppInfo>) {
        appInfos.forEach { cacheAppInfo(it) }
    }
    
    /**
     * Clear all caches.
     * Call this when apps are installed/uninstalled.
     */
    fun clear() {
        iconCache?.evictAll()
        categorizedAppsCache.clear()
        appInfoCache.clear()
        lastCategorizationTime = 0
    }
    
    /**
     * Clear only categorized apps cache.
     * Call this when ML categorization needs refresh.
     */
    fun clearCategorizedApps() {
        categorizedAppsCache.clear()
        lastCategorizationTime = 0
    }
    
    /**
     * Get cache statistics for debugging.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            iconCacheSize = iconCache?.size() ?: 0,
            iconCacheMaxSize = iconCache?.maxSize() ?: 0,
            iconCacheHits = iconCache?.hitCount() ?: 0,
            iconCacheMisses = iconCache?.missCount() ?: 0,
            categorizedAppsCount = categorizedAppsCache.size,
            appInfoCacheCount = appInfoCache.size,
            cacheAge = System.currentTimeMillis() - lastCategorizationTime,
        )
    }
    
    data class CacheStats(
        val iconCacheSize: Int,
        val iconCacheMaxSize: Int,
        val iconCacheHits: Int,
        val iconCacheMisses: Int,
        val categorizedAppsCount: Int,
        val appInfoCacheCount: Int,
        val cacheAge: Long,
    )
}
