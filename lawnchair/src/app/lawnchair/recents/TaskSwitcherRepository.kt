package app.lawnchair.recents

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.hardware.HardwareBuffer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.core.content.FileProvider
import androidx.core.os.BundleCompat
import app.lawnchair.backend.BackendSelector
import app.lawnchair.backend.Capability
import app.lawnchair.shizuku.ShizukuManager
import app.lawnchair.shizuku.UserService
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecentTask(
    val taskId: Int,
    val component: ComponentName,
    val label: String,
    val icon: Drawable?,
)

/**
 * Reads and controls recent tasks through a privileged IUserService, so AutoCat can show
 * its own recents screen without being the system's recents provider. The [BackendSelector]
 * picks the host: the root service when root is chosen, otherwise the Shizuku service
 * (stock Shizuku and ShizukuPlus alike).
 */
class TaskSwitcherRepository(private val context: Context) {

    private val shizuku = ShizukuManager.getInstance(context)
    private val selector = BackendSelector.getInstance(context)
    private val packageManager = context.packageManager

    sealed interface Availability {
        data object Ready : Availability
        data object ShizukuNotRunning : Availability
        data object PermissionNeeded : Availability

        /** Shizuku is usable but turned off or pinned away in Privileged access settings. */
        data object BackendDisabled : Availability
    }

    /**
     * Ready when any enabled backend can host the task service. Reads cached state only, so it is
     * safe on the main thread; the Shizuku messages remain the hint when nothing is available.
     */
    fun availability(): Availability {
        if (selector.resolveNow(Capability.TaskControl).backends.isNotEmpty()) return Availability.Ready
        // Root state is probed off the main thread; the next check sees the result
        selector.refreshInBackground()
        return when {
            !shizuku.isShizukuAvailable() -> Availability.ShizukuNotRunning

            !shizuku.hasPermission() -> Availability.PermissionNeeded

            // Shizuku is fine but the user turned it off or pinned another backend
            else -> Availability.BackendDisabled
        }
    }

    fun requestPermission() {
        shizuku.checkAndRequestPermission()
    }

    /** Recent tasks, newest first, without home screens. Null when the service isn't reachable. */
    suspend fun loadTasks(): List<RecentTask>? = withContext(Dispatchers.IO) {
        val service = selector.taskService() ?: return@withContext null
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
        val service = selector.taskService() ?: return@withContext null
        val result = service.getTaskThumbnail(taskId)
        val buffer = BundleCompat.getParcelable(result, UserService.KEY_BUFFER, HardwareBuffer::class.java)
            ?: return@withContext null
        Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB))?.asImageBitmap()
    }

    /** A software copy of the task's snapshot; hardware bitmaps can't be compressed or saved. */
    private suspend fun snapshotBitmap(taskId: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        val service = selector.taskService() ?: return@withContext null
        val result = service.getTaskThumbnail(taskId)
        val buffer = BundleCompat.getParcelable(result, UserService.KEY_BUFFER, HardwareBuffer::class.java)
            ?: return@withContext null
        Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB))
            ?.copy(Bitmap.Config.ARGB_8888, false)
    }

    /** Saves the task's snapshot to DCIM/Screenshots, where Samsung keeps screenshots. */
    suspend fun saveScreenshot(taskId: Int): Boolean = withContext(Dispatchers.IO) {
        val bitmap = snapshotBitmap(taskId) ?: return@withContext false
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Screenshot_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/Screenshots")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        val saved = runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
        }.getOrDefault(false)
        if (!saved) resolver.delete(uri, null, null)
        saved
    }

    /** Opens the share sheet for the task's snapshot, or Lens when [lens] is set. */
    suspend fun shareSnapshot(taskId: Int, lens: Boolean): Boolean = withContext(Dispatchers.IO) {
        val bitmap = snapshotBitmap(taskId) ?: return@withContext false
        val uri = persist(bitmap) ?: return@withContext false
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (lens) send.setPackage(LENS_PACKAGE)
        val intent = if (lens) send else Intent.createChooser(send, null)
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }

    fun isLensAvailable(): Boolean = packageManager.getLaunchIntentForPackage(LENS_PACKAGE) != null

    fun openAppInfo(task: RecentTask) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", task.component.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun persist(bitmap: Bitmap): Uri? = runCatching {
        val dir = File(context.cacheDir, "task_images").apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }
        val file = File(dir, "task_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    /** Opens the task, zooming out from [bounds] (window coordinates of its card inside [source]) when given. */
    suspend fun open(taskId: Int, source: View? = null, bounds: IntRect? = null): Boolean = withContext(Dispatchers.IO) {
        val options = if (source != null && bounds != null && !bounds.isEmpty) {
            ActivityOptions.makeScaleUpAnimation(source, bounds.left, bounds.top, bounds.width, bounds.height).toBundle()
        } else {
            null
        }
        val service = selector.taskService()
        val opened = service?.startTaskFromRecents(taskId, options) == true
        Log.i(TAG, "open task $taskId via ${service?.javaClass?.simpleName} -> $opened")
        opened
    }

    /** Opens the task in a freeform window (One UI's pop-up view). */
    suspend fun openPopup(taskId: Int): Boolean = withContext(Dispatchers.IO) {
        val options = Bundle().apply { putInt(KEY_WINDOWING_MODE, WINDOWING_MODE_FREEFORM) }
        val opened = selector.taskService()?.startTaskFromRecents(taskId, options) == true
        Log.i(TAG, "popup task $taskId -> $opened")
        opened
    }

    /** Puts the task in the top half of split screen through SystemUI's WMShell command; the user then picks the other app. */
    suspend fun openSplit(taskId: Int): Boolean = withContext(Dispatchers.IO) {
        val args = listOf("dumpsys", "activity", "service", "SystemUIService", "WMShell", "splitscreen", "moveToSideStage", "$taskId", "0")
        val result = selector.shellExecutor()?.exec(args)
        Log.i(TAG, "split task $taskId -> ${result?.success} ${result?.error.orEmpty()}")
        result?.success == true
    }

    suspend fun remove(taskId: Int): Boolean = withContext(Dispatchers.IO) {
        val service = selector.taskService()
        val removed = service?.removeTask(taskId) == true
        Log.i(TAG, "remove task $taskId via ${service?.javaClass?.simpleName} -> $removed")
        removed
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
        private const val TAG = "TaskSwitcherRepo"
        private const val MAX_TASKS = 30
        private const val KEY_WINDOWING_MODE = "android.activity.windowingMode"
        private const val WINDOWING_MODE_FREEFORM = 5
        private const val LENS_PACKAGE = "com.google.ar.lens"
    }
}
