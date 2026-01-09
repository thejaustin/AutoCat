package app.lawnchair.categorization.importer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
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

                val query = StringBuilder("SELECT id, label")
                if (hasParentId) query.append(", parentId")
                if (hasIcon) query.append(", icon")
                query.append(" FROM DrawerItem WHERE packageName IS NULL")

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

                // 6. Load Apps and Map them
                val appsToImport = mutableListOf<AppTab>()
                val appCursor = slDb.rawQuery("SELECT parentId, categoryId, packageName FROM DrawerItem WHERE packageName IS NOT NULL", null)

                var importedCount = 0

                val provider = app.lawnchair.categorization.AutoCatAppProvider.getInstance(context)
                val processedIcons = mutableSetOf<String>()

                if (appCursor.moveToFirst()) {
                    do {
                        val parentId = appCursor.getInt(0)
                        val categoryId = appCursor.getString(1)
                        val packageName = appCursor.getString(2)

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
                slDb.close()

                val categoryDao = TabDatabase.getInstance(context).categoryDao()

                // 6.5 Ensure all used tabs exist in CustomTab table
                val uniqueTabNames = appsToImport.map { it.tabName }.distinct()
                val existingCategories = categoryDao.getAllCustomCategories()
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
                        categoryDao.insertCustomCategory(newCategory)
                        android.util.Log.d("SmartLauncherImporter", "Created new custom tab: $tabName")
                    }
                }

                // 7. Batch Insert into AutoCat Database
                if (appsToImport.isNotEmpty()) {
                    appsToImport.forEach { categoryDao.insertAppCategory(it) }
                }

                // Cleanup
                tempZipFile.delete()
                tempDbFile.delete()

                return@withContext ImportResult.Success(importedCount)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ImportResult.Error("Import failed: ${e.message}")
            }
        }
    }

    sealed class ImportResult {
        data class Success(val count: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
