package app.lawnchair.categorization.local

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.core.content.getSystemService
import app.lawnchair.preferences.PreferenceManager
import java.io.File

/**
 * Where on-device models live, which one is selected, and downloading them.
 */
object LocalModelStore {

    private const val MODELS_DIR = "local_models"

    /** Space to leave free after a download, so the phone isn't left completely full. */
    private const val FREE_SPACE_MARGIN_BYTES = 1024L * 1024 * 1024

    fun modelsDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, MODELS_DIR).apply { mkdirs() }
    }

    fun fileFor(context: Context, model: LocalModelInfo): File? {
        val name = model.fileName ?: return null
        return File(modelsDir(context), name)
    }

    fun isDownloaded(context: Context, model: LocalModelInfo): Boolean {
        val file = fileFor(context, model) ?: return false
        return file.isFile && file.length() > 0
    }

    /**
     * Path of the model to run, or null when none is selected or the selected file is missing.
     */
    fun selectedModelPath(context: Context): String? {
        val prefs = PreferenceManager.getInstance(context)
        val selectedId = prefs.selectedLocalModelId.get()
        if (selectedId == "custom_file") {
            return prefs.localCustomModelPath.get().takeIf { it.isNotBlank() && File(it).isFile }
        }
        val model = LocalModelRegistry.findById(selectedId) ?: return null
        if (model.type != LocalModelType.LITERT_LM || !isDownloaded(context, model)) return null
        return fileFor(context, model)?.path
    }

    /**
     * Starts a download through the system download manager.
     *
     * @return null when the download started, or a message explaining why it didn't
     */
    fun download(context: Context, model: LocalModelInfo): String? {
        val url = model.downloadUrl ?: return "This model has no download link."
        val name = model.fileName ?: return "This model has no download link."
        val dir = modelsDir(context)
        val needed = model.sizeMb * 1024L * 1024 + FREE_SPACE_MARGIN_BYTES
        val available = StatFs(dir.path).availableBytes
        if (available < needed) {
            val shortMb = (needed - available) / (1024 * 1024)
            return "Not enough storage: free about $shortMb MB more and try again."
        }
        val downloadManager = context.getSystemService<DownloadManager>()
            ?: return "The system download manager isn't available."
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(model.displayName)
            .setDescription("AutoCat on-device AI model")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "$MODELS_DIR/$name")
        downloadManager.enqueue(request)
        return null
    }

    fun delete(context: Context, model: LocalModelInfo): Boolean {
        return fileFor(context, model)?.delete() == true
    }
}
