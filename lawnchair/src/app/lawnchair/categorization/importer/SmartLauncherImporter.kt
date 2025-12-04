package app.lawnchair.categorization.importer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.data.category.entities.AppCategory
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

                try {
                    ZipInputStream(tempZipFile.inputStream()).use { zipIn ->
                        var entry: ZipEntry? = zipIn.nextEntry
                        while (entry != null) {
                            if (entry.name.endsWith("ginlemon.flower.db")) {
                                // Extract this file
                                FileOutputStream(tempDbFile).use { output ->
                                    zipIn.copyTo(output)
                                }
                                foundInZip = true
                                dbFileToUse = tempDbFile
                                break
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

                // 5. Load Folders (id -> label)
                val folderMap = mutableMapOf<Int, String>()
                // Folders are DrawerItems with NULL packageName
                val folderCursor = slDb.rawQuery("SELECT id, label FROM DrawerItem WHERE packageName IS NULL", null)
                if (folderCursor.moveToFirst()) {
                    do {
                        val id = folderCursor.getInt(0)
                        val label = folderCursor.getString(1) ?: "Folder $id"
                        folderMap[id] = label
                    } while (folderCursor.moveToNext())
                }
                folderCursor.close()

                // 6. Load Apps and Map them
                val appsToImport = mutableListOf<AppCategory>()
                val appCursor = slDb.rawQuery("SELECT parentId, categoryId, packageName FROM DrawerItem WHERE packageName IS NOT NULL", null)

                var importedCount = 0

                if (appCursor.moveToFirst()) {
                    do {
                        val parentId = appCursor.getInt(0)
                        val categoryId = appCursor.getString(1)
                        val packageName = appCursor.getString(2)

                        // Determine Target Category Name
                        val targetCategory = if (folderMap.containsKey(parentId)) {
                            // It's in a folder -> Use Folder Name
                            folderMap[parentId]!!
                        } else {
                            // It's in the root of a category -> Use Category Name
                            categoryMap[categoryId] ?: "Uncategorized"
                        }

                        // Create AppCategory entity
                        // Source is 'manual' to take precedence over 'llm' or 'built-in'
                        // isUserOverride = true ensures it sticks
                        val appCategory = AppCategory(
                            packageName = packageName,
                            category = targetCategory,
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

                // 7. Batch Insert into AutoCat Database
                if (appsToImport.isNotEmpty()) {
                    val categoryDao = CategoryDatabase.getInstance(context).categoryDao()
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
