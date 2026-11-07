package app.lawnchair.recents

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.os.BundleCompat
import app.lawnchair.shizuku.ShizukuManager
import app.lawnchair.shizuku.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecentTask(
    val taskId: Int,
    val component: ComponentName,
    val label: String,
    val icon: Drawable?,
)

/**
 * Reads and controls recent tasks through the Shizuku shell service, so AutoCat can show
 * its own recents screen without being the system's recents provider.
 */
class TaskSwitcherRepository(private val context: Context) {

    private val shizuku = ShizukuManager.getInstance(context)
    private val packageManager = context.packageManager

    sealed interface Availability {
        data object Ready : Availability
        data object ShizukuNotRunning : Availability
        data object PermissionNeeded : Availability
    }

    fun availability(): Availability = when {
        !shizuku.isShizukuAvailable() -> Availability.ShizukuNotRunning
        !shizuku.hasPermission() -> Availability.PermissionNeeded
        else -> Availability.Ready
    }

    fun requestPermission() {
        shizuku.checkAndRequestPermission()
    }

    /** Recent tasks, newest first, without home screens. Null when the service isn't reachable. */
    suspend fun loadTasks(): List<RecentTask>? = withContext(Dispatchers.IO) {
        val service = shizuku.awaitService() ?: return@withContext null
        // UserHandle.hashCode() is the user id
        val result = service.getRecentTasks(MAX_TASKS, Process.myUserHandle().hashCode())
        val ids = result.getIntArray(UserService.KEY_TASK_IDS) ?: return@withContext emptyList()
        val components = result.getStringArray(UserService.KEY_COMPONENTS) ?: return@withContext emptyList()
        val homePackages = homePackages()
        ids.indices.mapNotNull { index ->
            val component = ComponentName.unflattenFromString(components[index]) ?: return@mapNotNull null
            if (component.packageName in homePackages) return@mapNotNull null
            RecentTask(
                taskId = ids[index],
                component = component,
                label = loadLabel(component),
                icon = loadIcon(component),
            )
        }
    }

    suspend fun loadThumbnail(taskId: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        val service = shizuku.awaitService() ?: return@withContext null
        val result = service.getTaskThumbnail(taskId)
        val buffer = BundleCompat.getParcelable(result, UserService.KEY_BUFFER, HardwareBuffer::class.java)
            ?: return@withContext null
        Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB))?.asImageBitmap()
    }

    suspend fun open(taskId: Int): Boolean = withContext(Dispatchers.IO) {
        shizuku.awaitService()?.startTaskFromRecents(taskId, null) == true
    }

    suspend fun remove(taskId: Int): Boolean = withContext(Dispatchers.IO) {
        shizuku.awaitService()?.removeTask(taskId) == true
    }

    private fun homePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapTo(mutableSetOf()) { it.activityInfo.packageName } + context.packageName
    }

    private fun loadLabel(component: ComponentName): String {
        return runCatching { packageManager.getActivityInfo(component, 0).loadLabel(packageManager).toString() }
            .recoverCatching {
                packageManager.getApplicationInfo(component.packageName, 0).loadLabel(packageManager).toString()
            }
            .getOrDefault(component.packageName)
    }

    private fun loadIcon(component: ComponentName): Drawable? {
        return runCatching { packageManager.getActivityIcon(component) }
            .recoverCatching { packageManager.getApplicationIcon(component.packageName) }
            .getOrNull()
    }

    companion object {
        private const val MAX_TASKS = 30
    }
}
