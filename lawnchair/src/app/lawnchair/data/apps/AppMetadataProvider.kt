package app.lawnchair.data.apps

import android.content.Context
import android.content.pm.PackageManager

/**
 * Represents app metadata for AutoCat categorization.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val category: Int? = null,
    val installedTime: Long = 0L,
    val description: String? = null,
)

/**
 * Simple metadata provider stub.
 * Returns empty list since app info is handled by launcher's AppInfo model.
 */
class AppMetadataProvider(private val context: Context) {
    fun getInstalledApps(): List<AppInfo> {
        return emptyList()
    }
}
