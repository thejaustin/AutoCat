package app.lawnchair.archival

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.lawnchair.appops.AppBatchOperationService
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.preferences.PreferenceManager
import com.patrykmichalik.opto.core.IntPreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class PredictiveArchiveServiceTest {

    @Mock private lateinit var usageStatsManager: UsageStatsManager
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var categorizationManager: CategorizationManager
    @Mock private lateinit var metadataProvider: AppMetadataProvider
    @Mock private lateinit var batchService: AppBatchOperationService
    @Mock private lateinit var prefs: PreferenceManager
    @Mock private lateinit var tabDao: TabDao

    private lateinit var service: PredictiveArchiveService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        service = PredictiveArchiveService(
            context,
            usageStatsManager,
            packageManager,
            categorizationManager,
            metadataProvider,
            batchService,
            prefs,
            tabDao
        )
    }

    @Test
    fun testGetArchiveCandidates_UnusedGame() = runBlocking {
        val packageName = "com.example.game"
        val apps = listOf(AppInfo(packageName = packageName, label = "Unused Game"))
        
        `when`(metadataProvider.getInstalledApps()).thenReturn(apps)
        `when`(batchService.isSystemApp(packageName)).thenReturn(false)
        
        // Mock 30 days threshold
        val thresholdPref = mock(IntPreference::class.java)
        `when`(thresholdPref.get()).thenReturn(30)
        `when`(prefs.predictiveArchiveThreshold).thenReturn(thresholdPref)
        
        // Mock usage stats: last used 60 days ago
        val lastUsed = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)
        val stats = mock(UsageStats::class.java)
        `when`(stats.lastTimeUsed).thenReturn(lastUsed)
        `when`(usageStatsManager.queryAndAggregateUsageStats(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(mapOf(packageName to stats))
            
        // Mock category: Games
        `when`(tabDao.getAppTab(packageName)).thenReturn(AppTab(packageName, "Games", 1.0f, "test"))
        
        val candidates = service.getArchiveCandidates()
        
        assertEquals(1, candidates.size)
        assertEquals(packageName, candidates[0].packageName)
        assertEquals("Games", candidates[0].category)
        // Importance for Games is 0.2, should be low
        assert(candidates[0].importanceScore < 0.5f)
    }

    @Test
    fun testGetArchiveCandidates_UsedFinanceApp() = runBlocking {
        val packageName = "com.example.bank"
        val apps = listOf(AppInfo(packageName = packageName, label = "Used Bank"))
        
        `when`(metadataProvider.getInstalledApps()).thenReturn(apps)
        `when`(batchService.isSystemApp(packageName)).thenReturn(false)
        
        val thresholdPref = mock(IntPreference::class.java)
        `when`(thresholdPref.get()).thenReturn(30)
        `when`(prefs.predictiveArchiveThreshold).thenReturn(thresholdPref)
        
        // Mock usage stats: last used 1 day ago
        val lastUsed = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
        val stats = mock(UsageStats::class.java)
        `when`(stats.lastTimeUsed).thenReturn(lastUsed)
        `when`(usageStatsManager.queryAndAggregateUsageStats(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(mapOf(packageName to stats))
            
        val candidates = service.getArchiveCandidates()
        
        // Should NOT be a candidate because it was used recently
        assertEquals(0, candidates.size)
    }
}
