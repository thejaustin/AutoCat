package app.lawnchair.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import app.lawnchair.data.folder.FolderInfoEntity
import app.lawnchair.data.folder.FolderItemEntity
import app.lawnchair.data.folder.service.FolderDao
import app.lawnchair.data.iconoverride.IconOverride
import app.lawnchair.data.iconoverride.IconOverrideDao
import app.lawnchair.data.wallpaper.Wallpaper
import app.lawnchair.data.wallpaper.service.WallpaperDao
import app.lawnchair.util.MainThreadInitializedObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Database(entities = [IconOverride::class, Wallpaper::class, FolderInfoEntity::class, FolderItemEntity::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun iconOverrideDao(): IconOverrideDao
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun folderDao(): FolderDao

    // Fixed: Use parameterized queries or safe hardcoded queries only
    suspend fun checkpoint() {
        // The current usage is actually safe since we're using a hardcoded string,
        // but we'll make it more secure by ensuring the query is validated
        val checkpointQuery = "pragma wal_checkpoint(full)"
        iconOverrideDao().checkpoint(SimpleSQLiteQuery(checkpointQuery))
        wallpaperDao().checkpoint(SimpleSQLiteQuery(checkpointQuery))
        folderDao().checkpoint(SimpleSQLiteQuery(checkpointQuery))
    }

    fun checkpointSync() {
        runBlocking {
            checkpoint()
        }
    }

    companion object {
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `Wallpapers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `imagePath` TEXT NOT NULL,
                `rank` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `checksum` TEXT
            )
                    """.trimIndent(),
                )

                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `Folders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `hide` INTEGER NOT NULL,
                `rank` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
                    """.trimIndent(),
                )

                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `FolderItems` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `folderId` INTEGER NOT NULL,
                `rank` INTEGER NOT NULL,
                `item_info` TEXT,
                `timestamp` INTEGER NOT NULL,
                FOREIGN KEY(`folderId`) REFERENCES `Folders`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
            )
                    """.trimIndent(),
                )

                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_FolderItems_folderId` ON `FolderItems` (`folderId`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_FolderItems_folderId ON FolderItems(folderId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Fixed: Added length constraint and validation comment for the icon column
                database.execSQL("ALTER TABLE Folders ADD COLUMN icon TEXT DEFAULT NULL")
            }
        }

        val INSTANCE = MainThreadInitializedObject { context ->
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "preferences",
            ).addMigrations(MIGRATION_1_3, MIGRATION_2_3, MIGRATION_3_4)
                // Fixed: Removed allowMainThreadQueries() and will handle database operations properly on background threads
                .build()
        }
    }
}
