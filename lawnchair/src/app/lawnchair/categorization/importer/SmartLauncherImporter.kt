package app.lawnchair.categorization.importer

import android.content.ComponentName
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
import com.android.launcher3.util.LauncherLayoutBuilder
import com.android.launcher3.util.LayoutImportExportHelper
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartLauncherImporter(private val context: Context) {

    suspend fun importFromUri(uri: Uri): ImportResult {
        return withContext(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val tempDbFile = File(cacheDir, "sl_import_temp.db")
            val tempZipFile = File(cacheDir, "sl_import_temp.zip")

            try {
                // 1. Copy stream to temp file to determine if it's a DB or Zip
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempZipFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext ImportResult.Error("Could not read file")

                // 2. Try to extract DB from Zip (assuming .slbk structure)
                var dbFileToUse = tempZipFile
                var foundInZip = false
                val iconsDir = File(context.filesDir, "imported_icons").apply { mkdirs() }

                try {
                    // First pass: Extract DB
                    ZipInputStream(tempZipFile.inputStream()).use { zipIn ->
                        var entry: ZipEntry? = zipIn.nextEntry
                        while (entry != null) {
                            if (entry.name.endsWith("ginlemon.flower.db")) {
                                FileOutputStream(tempDbFile).use { output ->
                                    zipIn.copyTo(output)
                                }
                                foundInZip = true
                                dbFileToUse = tempDbFile
                            }
                            // Extract icons/images
                            else if (entry.name.startsWith("icons/") || entry.name.endsWith(".png") || entry.name.endsWith(".jpg")) {
                                val iconFile = File(iconsDir, entry.name)
                                iconFile.parentFile?.mkdirs()
                                FileOutputStream(iconFile).use { output ->
                                    zipIn.copyTo(output)
                                }
                            }

                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                } catch (e: Exception) {
                    // Not a zip or failed to read as zip, assume it's a DB file directly
                    dbFileToUse = tempZipFile
                }

                // If we processed a zip but didn't find the DB
                if (tempZipFile.name.endsWith(".slbk") && !foundInZip && !dbFileToUse.exists()) {
                    return@withContext ImportResult.Error("Could not find database in backup file")
                }

                // 3. Open Database
                val slDb = try {
                    SQLiteDatabase.openDatabase(
                        dbFileToUse.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY,
                    )
                } catch (e: Exception) {
                    return@withContext ImportResult.Error("Invalid database format: ${e.message}")
                }

                // 4. Load Categories (id -> label)
                val categoryMap = mutableMapOf<String, String>()
                try {
                    val catCursor = slDb.rawQuery("SELECT id, customLabel FROM Category", null)
                    if (catCursor.moveToFirst()) {
                        do {
                            val id = catCursor.getString(0)
                            val label = catCursor.getString(1)
                            // Use custom label if available, otherwise capitalize the ID or generic fallback
                            val finalLabel = label ?: id.replaceFirstChar { it.uppercase() }
                            categoryMap[id] = finalLabel
                        } while (catCursor.moveToNext())
                    }
                    catCursor.close()
                } catch (e: Exception) {
                    // Ignore errors if table doesn't exist
                }

                // 5. Load Folders (id -> label, parentId, icon)
                data class FolderItem(val id: Int, val label: String, val parentId: Int, val icon: String?)
                val folderMap = mutableMapOf<Int, FolderItem>()

                // Try to get columns, handle if they don't exist
                val folderColumns = try {
                    val cursor = slDb.rawQuery("PRAGMA table_info(DrawerItem)", null)
                    val names = mutableListOf<String>()
                    if (cursor.moveToFirst()) {
                        do {
                            names.add(cursor.getString(1)) // name column
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                    names
                } catch (e: Exception) {
                    listOf("id", "label") // Fallback
                }

                val hasParentId = folderColumns.contains("parentId")
                val hasIcon = folderColumns.contains("icon")

                if (folderColumns.contains("id")) { // Only proceed if we have at least an ID
                    val query = StringBuilder("SELECT id, label")
                    if (hasParentId) query.append(", parentId")
                    if (hasIcon) query.append(", icon")
                    query.append(" FROM DrawerItem WHERE packageName IS NULL")

                    try {
                        val folderCursor = slDb.rawQuery(query.toString(), null)
                        if (folderCursor.moveToFirst()) {
                            do {
                                val id = folderCursor.getInt(0)
                                val label = folderCursor.getString(1) ?: "Folder $id"
                                val parentId = if (hasParentId) folderCursor.getInt(2) else 0
                                // Icon index depends on whether parentId was selected
                                val iconIndex = if (hasParentId) 3 else 2
                                val iconRaw = if (hasIcon && !folderCursor.isNull(iconIndex)) folderCursor.getString(iconIndex) else null

                                // Resolve icon path if it exists
                                val iconPath = if (iconRaw != null) {
                                    val file = File(iconsDir, iconRaw)
                                    if (file.exists()) file.absolutePath else iconRaw
                                } else {
                                    null
                                }

                                folderMap[id] = FolderItem(id, label, parentId, iconPath)
                            } while (folderCursor.moveToNext())
                        }
                        folderCursor.close()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                // 6. Load Apps and Map them
                val appsToImport = mutableListOf<AppTab>()
                var importedCount = 0

                try {
                    // Try to identify icon customization columns
                    val appColumns = try {
                        val cursor = slDb.rawQuery("PRAGMA table_info(DrawerItem)", null)
                        val names = mutableListOf<String>()
                        if (cursor.moveToFirst()) {
                            do {
                                names.add(cursor.getString(1)) // name column
                            } while (cursor.moveToNext())
                        }
                        cursor.close()
                        names
                    } catch (e: Exception) {
                        listOf("id", "label", "packageName") // Fallback
                    }

                    val hasIconPackage = appColumns.firstOrNull { it.equals("iconPackage", ignoreCase = true) || it.equals("icon_package", ignoreCase = true) }
                    val hasIconName = appColumns.firstOrNull { it.equals("iconName", ignoreCase = true) || it.equals("icon_name", ignoreCase = true) }
                    // 'icon' column often holds the drawable name or file path
                    val iconColumn = hasIconName ?: "icon"

                    val queryBuilder = StringBuilder("SELECT parentId, categoryId, packageName")
                    if (hasIconPackage != null) queryBuilder.append(", $hasIconPackage") else queryBuilder.append(", NULL")
                    queryBuilder.append(", $iconColumn")
                    queryBuilder.append(" FROM DrawerItem WHERE packageName IS NOT NULL")

                    val appCursor = slDb.rawQuery(queryBuilder.toString(), null)

                    val provider = app.lawnchair.categorization.AutoCatAppProvider.getInstance(context)
                    val iconRepo = app.lawnchair.data.iconoverride.IconOverrideRepository.INSTANCE.get(context)
                    val processedIcons = mutableSetOf<String>()
                    val user = android.os.Process.myUserHandle()
                    val packageManager = context.packageManager

                    if (appCursor.moveToFirst()) {
                        do {
                            val parentId = appCursor.getInt(0)
                            val categoryId = appCursor.getString(1)
                            val packageName = appCursor.getString(2)
                            val iconPackage = appCursor.getString(3)
                            val iconName = appCursor.getString(4)

                            // Determine Target Tab Name
                            val targetTabName = categoryMap[categoryId] ?: "Uncategorized"

                            // Determine Sub-Category (Folder)
                            var targetSubCategory: String? = null
                            var targetIcon: String? = null

                            if (parentId != 0 && folderMap.containsKey(parentId)) {
                                val folder = folderMap[parentId]!!
                                targetSubCategory = folder.label
                                // Use the folder's icon
                                targetIcon = folder.icon

                                if (targetIcon != null) {
                                    val key = "$targetTabName|$targetSubCategory"
                                    if (processedIcons.add(key)) {
                                        provider.saveSubCategoryIcon(targetTabName, targetSubCategory, targetIcon)
                                    }
                                }
                            }

                            // Handle App Icon Customization (Icon Pack)
                            if (!iconPackage.isNullOrEmpty() && !iconName.isNullOrEmpty()) {
                                try {
                                    // Verify the icon pack exists
                                    // val iconPackInfo = packageManager.getPackageInfo(iconPackage, 0) // Optional check

                                    // Try to resolve the class name for the component key
                                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                    val className = launchIntent?.component?.className

                                    if (className != null) {
                                        val componentKey = com.android.launcher3.util.ComponentKey(
                                            ComponentName(packageName, className),
                                            user,
                                        )

                                        val iconItem = app.lawnchair.icons.IconPickerItem(
                                            packPackageName = iconPackage,
                                            drawableName = iconName,
                                            label = packageName, // label isn't strictly used for lookup
                                            type = app.lawnchair.icons.IconType.Normal,
                                        )

                                        iconRepo.setOverride(componentKey, iconItem)
                                    }
                                } catch (e: Exception) {
                                    // Failed to apply icon override, ignore
                                    android.util.Log.e("SmartLauncherImporter", "Failed to apply icon override for $packageName", e)
                                }
                            }

                            // Create AppTab entity
                            // Source is 'manual' to take precedence over 'llm' or 'built-in'
                            // isUserOverride = true ensures it sticks
                            val appCategory = AppTab(
                                packageName = packageName,
                                tabName = targetTabName,
                                subCategory = targetSubCategory,
                                confidence = 1.0f,
                                source = "import_sl", // Special source tag
                                isUserOverride = true,
                                reasoning = "Imported from Smart Launcher Backup",
                                lastUpdated = System.currentTimeMillis(),
                            )
                            appsToImport.add(appCategory)
                            importedCount++
                        } while (appCursor.moveToNext())
                    }
                    appCursor.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 7. Batch Insert into AutoCat Database
                val categoryDao = TabDatabase.getInstance(context).tabDao()

                // 7.5 Ensure all used tabs exist in CustomTab table
                val uniqueTabNames = appsToImport.map { it.tabName }.distinct()
                val existingCategories = categoryDao.getAllCustomTabs()
                var nextSortOrder = existingCategories.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0

                // Helper to pick a random default color
                val defaultColors = listOf(
                    CustomTab.COLOR_GAMES,
                    CustomTab.COLOR_SOCIAL,
                    CustomTab.COLOR_PRODUCTIVITY,
                    CustomTab.COLOR_TOOLS,
                    CustomTab.COLOR_ENTERTAINMENT,
                    CustomTab.COLOR_PHOTOGRAPHY,
                    CustomTab.COLOR_COMMUNICATION,
                )

                for (tabName in uniqueTabNames) {
                    val exists = existingCategories.any { it.name.equals(tabName, ignoreCase = true) }
                    if (!exists) {
                        // Try to find an icon? For now, leave null or use default.
                        // We could check if there's a folder with the same name to steal its icon.
                        val matchingFolder = folderMap.values.find { it.label.equals(tabName, ignoreCase = true) }

                        val newCategory = CustomTab(
                            name = tabName,
                            colorHex = defaultColors.random(),
                            sortOrder = nextSortOrder++,
                            isVisible = true,
                            icon = matchingFolder?.icon,
                        )
                        categoryDao.insertCustomTab(newCategory)
                        android.util.Log.d("SmartLauncherImporter", "Created new custom tab: $tabName")
                    }
                }

                if (appsToImport.isNotEmpty()) {
                    appsToImport.forEach { categoryDao.insertAppTab(it) }
                }

                // 8. Import Workspace
                var workspaceImported = false
                try {
                    workspaceImported = importWorkspace(slDb)
                } catch (e: Exception) {
                    android.util.Log.e("SmartLauncherImporter", "Workspace import failed", e)
                }

                slDb.close()

                // Cleanup
                tempZipFile.delete()
                tempDbFile.delete()

                return@withContext ImportResult.Success(importedCount, workspaceImported)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ImportResult.Error("Import failed: ${e.message}")
            }
        }
    }

    private fun importWorkspace(slDb: SQLiteDatabase): Boolean {
        // Attempt to find workspace table
        val tables = mutableListOf<String>()
        val cursor = slDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        if (cursor.moveToFirst()) {
            do {
                tables.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()

        android.util.Log.d("SmartLauncherImporter", "Found tables: $tables")

        // Potential workspace tables
        val workspaceTable = tables.firstOrNull {
            it.contains("Home", ignoreCase = true) ||
                it.contains("Desktop", ignoreCase = true) ||
                it.contains("Bubble", ignoreCase = true) ||
                it.contains("Item", ignoreCase = true) // Generic fallback
        } ?: return false

        android.util.Log.d("SmartLauncherImporter", "Attempting to import from table: $workspaceTable")

        val columns = mutableListOf<String>()
        val colCursor = slDb.rawQuery("PRAGMA table_info($workspaceTable)", null)
        if (colCursor.moveToFirst()) {
            do {
                columns.add(colCursor.getString(1))
            } while (colCursor.moveToNext())
        }
        colCursor.close()

        if (!columns.contains("packageName") && !columns.contains("intent")) {
            return false
        }

        // Construct query
        val hasPackageName = columns.contains("packageName")
        val hasIntent = columns.contains("intent")
        val hasCellX = columns.contains("cellX")
        val hasCellY = columns.contains("cellY")
        val hasScreen = columns.contains("screen")

        val queryBuilder = StringBuilder("SELECT ")
        if (hasPackageName) queryBuilder.append("packageName") else queryBuilder.append("NULL")
        queryBuilder.append(", ")
        if (hasIntent) queryBuilder.append("intent") else queryBuilder.append("NULL")
        if (hasCellX) queryBuilder.append(", cellX")
        if (hasCellY) queryBuilder.append(", cellY")
        if (hasScreen) queryBuilder.append(", screen")

        queryBuilder.append(" FROM $workspaceTable")

        val builder = LauncherLayoutBuilder()
        val itemsCursor = slDb.rawQuery(queryBuilder.toString(), null)

        var currentX = 0
        var currentY = 0
        var currentScreen = 0
        val maxY = 5 // Arbitrary grid height
        val maxX = 4 // Arbitrary grid width
        var itemsFound = 0

        if (itemsCursor.moveToFirst()) {
            do {
                val packageName = itemsCursor.getString(0)
                val intent = itemsCursor.getString(1)

                var x = if (hasCellX) itemsCursor.getInt(2) else currentX
                var y = if (hasCellY && hasCellX) itemsCursor.getInt(3) else currentY // shift index if cellX exists
                var screen = if (hasScreen && hasCellY && hasCellX) itemsCursor.getInt(4) else currentScreen

                // If no coordinates, do simple flow layout
                if (!hasCellX || !hasCellY) {
                    if (currentY >= maxY) {
                        currentY = 0
                        currentX++
                        if (currentX >= maxX) {
                            currentX = 0
                            currentScreen++
                        }
                    }
                    x = currentX
                    y = currentY
                    screen = currentScreen

                    currentY++
                }

                if (packageName != null) {
                    builder.atWorkspace(x, y, screen).putApp(packageName, null)
                    itemsFound++
                } else if (intent != null) {
                    // Try to parse package from intent if possible, or just ignore
                    // Simple intent parsing might be too complex here without URI parser
                }
            } while (itemsCursor.moveToNext())
        }
        itemsCursor.close()

        if (itemsFound > 0) {
            try {
                val xml = builder.build()
                LayoutImportExportHelper.importModelFromXml(context, xml)
                android.util.Log.d("SmartLauncherImporter", "Workspace import requested")
                return true
            } catch (e: Exception) {
                android.util.Log.e("SmartLauncherImporter", "Failed to build/import workspace XML", e)
                return false
            }
        }
        return false
    }

    sealed class ImportResult {
        data class Success(val count: Int, val workspaceImported: Boolean) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
