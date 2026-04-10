package app.lawnchair.categorization

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.lawnchair.categorization.stages.MLCategorizer
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.entities.CustomTab
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class MLCategorizerTest {

    @Mock
    private lateinit var tabDao: TabDao

    private lateinit var categorizer: MLCategorizer
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        categorizer = MLCategorizer(context, tabDao)
    }

    @Test
    fun testCategorize_Finance() = runBlocking {
        val appInfo = AppInfo(packageName = "com.android.bank", label = "My Bank")
        val visibleTabs = listOf(CustomTab(name = "Finance", id = 1))
        
        `when`(tabDao.getVisibleCustomTabs()).thenReturn(visibleTabs)
        
        val result = categorizer.categorize(appInfo)
        assertTrue(result)
    }

    @Test
    fun testCategorize_Games() = runBlocking {
        val appInfo = AppInfo(packageName = "com.supercell.clash", label = "Clash")
        val visibleTabs = listOf(CustomTab(name = "Games", id = 2))
        
        `when`(tabDao.getVisibleCustomTabs()).thenReturn(visibleTabs)
        
        val result = categorizer.categorize(appInfo)
        assertTrue(result)
    }

    @Test
    fun testCategorize_NoMatchingTab() = runBlocking {
        val appInfo = AppInfo(packageName = "com.android.bank", label = "My Bank")
        val visibleTabs = listOf(CustomTab(name = "Social", id = 3))
        
        `when`(tabDao.getVisibleCustomTabs()).thenReturn(visibleTabs)
        
        val result = categorizer.categorize(appInfo)
        // Finance app won't match Social tab using findBestMatchingTab substring logic
        assertTrue(!result)
    }

    @Test
    fun testCategorizeBatch() = runBlocking {
        val apps = listOf(
            AppInfo(packageName = "com.android.bank", label = "My Bank"),
            AppInfo(packageName = "com.facebook.orca", label = "Messenger")
        )
        val visibleTabs = listOf(
            CustomTab(name = "Finance", id = 1),
            CustomTab(name = "Social", id = 2)
        )
        
        `when`(tabDao.getVisibleCustomTabs()).thenReturn(visibleTabs)
        
        val count = categorizer.categorizeBatch(apps)
        assertEquals(2, count)
    }
}
