package app.lawnchair.data.tab

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.data.tab.entities.ModelAccuracy

/**
 * Room database for app tabs (categorization).
 *
 * Stores:
 * - App categorizations (AppTab)
 * - Custom user-defined tabs (CustomTab)
 * - Model accuracy tracking (ModelAccuracy)
 */
@Database(
    entities = [AppTab::class, CustomTab::class, ModelAccuracy::class],
    version = 1,
    exportSchema = true,
)
abstract class TabDatabase : RoomDatabase() {

    abstract fun tabDao(): TabDao
    abstract fun accuracyDao(): AccuracyDao

    companion object {
        private const val DATABASE_NAME = "app_tabs.db"

        @Volatile
        private var instance: TabDatabase? = null

        fun getInstance(context: Context): TabDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    TabDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2) // Placeholder for future
                    .build()
                instance = newInstance
                newInstance
            }
        }

        // Example migration (not needed for version 1)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration logic here
            }
        }
    }
}
