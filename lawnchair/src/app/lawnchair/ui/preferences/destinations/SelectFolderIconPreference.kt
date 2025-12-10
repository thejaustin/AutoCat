package app.lawnchair.ui.preferences.destinations

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lawnchair.icons.IconPackProvider
import app.lawnchair.icons.IconPickerItem
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.LocalPreferenceInteractor
import app.lawnchair.ui.preferences.components.AppItem
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceLayoutLazyColumn
import app.lawnchair.ui.preferences.components.layout.preferenceGroupItems
import app.lawnchair.ui.preferences.navigation.IconPicker
import app.lawnchair.ui.util.OnResult
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings
import com.android.launcher3.R
import com.android.launcher3.icons.GraphicsUtils
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.util.ContentWriter
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

@Composable
fun SelectFolderIconPreference(folderId: Int) {
    val context = LocalContext.current
    val launcherAppState = LauncherAppState.getInstance(context)
    val folderInfo = launcherAppState.model.bgDataModel.collections.get(folderId) as? FolderInfo
    val label = folderInfo?.title?.toString() ?: stringResource(R.string.folder_name_format_overflow)

    val iconPacks by LocalPreferenceInteractor.current.iconPacks.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    OnResult<IconPickerItem> { item ->
        scope.launch {
            val iconPack = IconPackProvider.INSTANCE.get(context).getIconPack(item.packPackageName)
            if (iconPack != null) {
                iconPack.load()
                val iconDrawable = iconPack.getIcon(item.toIconEntry(), 0)
                if (iconDrawable != null) {
                    val iconBitmap = if (iconDrawable.intrinsicWidth <= 0 || iconDrawable.intrinsicHeight <= 0) {
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    } else {
                        iconDrawable.toBitmap()
                    }

                    val iconFile = File(context.cacheDir, "folder_icon_$folderId")
                    try {
                        FileOutputStream(iconFile).use { out ->
                            iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        if (folderInfo != null) {
                            folderInfo.icon = iconFile.absolutePath

                            val contentWriter = ContentWriter(context)
                            contentWriter.put(LauncherSettings.Favorites.ICON, GraphicsUtils.flattenBitmap(iconBitmap))
                            val dbController = launcherAppState.model.modelDbController

                            val values = contentWriter.getValues(context)
                            dbController.update(LauncherSettings.Favorites.TABLE_NAME, values, "_id = ?", arrayOf(folderId.toString()))

                            launcherAppState.reloadIcons()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            (context as Activity).let {
                it.setResult(Activity.RESULT_OK)
                it.finish()
            }
        }
    }

    val hasOverride = folderInfo?.icon != null

    PreferenceLayoutLazyColumn(label = label) {
        if (hasOverride) {
            preferenceGroupItems(1, isFirstChild = true) {
                ClickablePreference(
                    label = stringResource(id = R.string.icon_picker_reset_to_default),
                    onClick = {
                        scope.launch {
                            if (folderInfo != null) {
                                folderInfo.icon = null

                                val values = android.content.ContentValues()
                                values.putNull(LauncherSettings.Favorites.ICON)
                                val dbController = launcherAppState.model.modelDbController
                                dbController.update(LauncherSettings.Favorites.TABLE_NAME, values, "_id = ?", arrayOf(folderId.toString()))

                                launcherAppState.reloadIcons()
                            }
                            (context as Activity).let {
                                it.setResult(Activity.RESULT_OK)
                                it.finish()
                            }
                        }
                    },
                )
            }
        }
        preferenceGroupItems(
            heading = { stringResource(id = R.string.pick_icon_from_label) },
            items = iconPacks,
            isFirstChild = !hasOverride,
        ) { _, iconPack ->
            AppItem(
                label = iconPack.name,
                icon = remember(iconPack) { iconPack.icon.toBitmap() },
                onClick = {
                    if (iconPack.packageName.isEmpty()) {
                        navController.navigate(IconPicker())
                    } else {
                        navController.navigate(IconPicker(iconPack.packageName))
                    }
                },
            )
        }
    }
}
