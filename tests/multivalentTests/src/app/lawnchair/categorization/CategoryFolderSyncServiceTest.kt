package app.lawnchair.categorization

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.ReloadHelper
import com.android.launcher3.model.data.AppInfo
import com.patrykmichalik.opto.core.BoolPreference
import com.patrykmichalik.opto.core.StringPreference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class CategoryFolderSyncServiceTest {

    @Mock private lateinit var prefs: PreferenceManager
    @Mock private lateinit var folderService: FolderService
    @Mock private lateinit var reloadHelper: ReloadHelper

    private lateinit var service: CategoryFolderSyncService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        service = CategoryFolderSyncService(
            context,
            prefs,
            folderService,
            reloadHelper
        )
    }

    @Test
    fun testIsSyncEnabled_WhenFalse() {
        val syncPref = mock(BoolPreference::class.java)
        `when`(syncPref.get()).thenReturn(false)
        `when`(prefs.autoCatSyncFolders).thenReturn(syncPref)
        
        assertFalse(service.isSyncEnabled())
    }

    @Test
    fun testSyncCategoriesToFolders_Disabled() = runBlocking {
        val syncPref = mock(BoolPreference::class.java)
        `when`(syncPref.get()).thenReturn(false)
        `when`(prefs.autoCatSyncFolders).thenReturn(syncPref)
        
        val result = service.syncCategoriesToFolders(mapOf("pkg" to "Tab"))
        
        assertFalse(result.success)
        assertEquals("Folder sync is disabled", result.message)
    }

    @Test
    fun testSyncCategoriesToFolders_NoSyncMode() = runBlocking {
        val syncPref = mock(BoolPreference::class.java)
        `when`(syncPref.get()).thenReturn(true)
        `when`(prefs.autoCatSyncFolders).thenReturn(syncPref)
        
        val modePref = mock(StringPreference::class.java)
        `when`(modePref.get()).thenReturn("none")
        `when`(prefs.autoCatFolderSyncMode).thenReturn(modePref)
        
        val result = service.syncCategoriesToFolders(mapOf("pkg" to "Tab"))
        
        assertFalse(result.success)
        assertEquals("No sync mode enabled", result.message)
    }
}
