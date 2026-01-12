package app.lawnchair.data.apps

import android.content.Context
import android.content.pm.PackageManager

data class AppMetadata(
    val packageName: String,
    val label: String,
)

class AppMetadataProvider(private val context: Context) {
    fun getInstalledApps(): List<AppMetadata> {
        return try {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName != context.packageName }
                .map { appInfo ->
                    AppMetadata(
                        packageName = appInfo.packageName,
                        label = pm.getApplicationLabel(appInfo).toString(),
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
