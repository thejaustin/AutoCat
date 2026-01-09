package app.lawnchair.data.tab

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.data.tab.entities.ModelAccuracy

/**
 * Room database for app tab assignments and custom tab management.
 *
 * This database stores:
 * - App tab assignments (which apps belong to which tabs)
 * - Custom tab definitions (user-created tabs with colors and ordering)
 * - Model accuracy tracking (LLM prediction performance metrics)
 *
 * Database version: 6
 * - v6: Added ModelAccuracy entity for accuracy tracking
 * - v5: Added llm_provider and llm_model fields to AppTab entity
 * - v4: Previous schema
 * Export schema: false (disabled for development, will enable for production)
 */
@Database(
    entities = [
        AppTab::class,
        CustomTab::class,
        ModelAccuracy::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class TabDatabase : RoomDatabase() {

    /**
     * Provides access to tab-related data operations.
     */
    abstract fun categoryDao(): TabDao

    /**
     * Provides access to model accuracy tracking operations.
     */
    abstract fun accuracyDao(): AccuracyDao

    companion object {
        private const val DATABASE_NAME = "category_database"

        @Volatile
        private var instance: TabDatabase? = null

        /**
         * Gets the singleton instance of TabDatabase.
         *
         * Uses double-checked locking to ensure thread-safe singleton creation.
         * The database is created with fallback to destructive migration for
         * development purposes.
         *
         * @param context Application context
         * @return Singleton TabDatabase instance
         */
        fun getInstance(context: Context): TabDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        /**
         * Builds the Room database instance with appropriate configuration.
         */
        private fun buildDatabase(context: Context): TabDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TabDatabase::class.java,
                DATABASE_NAME,
            )
                // For development: destroy and rebuild on schema changes
                // TODO: Replace with proper migrations before production release
                // dropAllTables = true: all tables will be dropped on migration failure
                .fallbackToDestructiveMigration(dropAllTables = true)
                // Temporarily allow main thread queries to prevent startup crashes during DB initialization
                .allowMainThreadQueries()
                // Initialize default tabs on first run
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Default tabs will be initialized on first DAO access
                        // via TabDao.initializeDefaultCategoriesIfNeeded()
                    }
                })
                .build()
        }

        /**
         * Clears the singleton instance. Useful for testing.
         * Should not be called in production code.
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            instance?.close()
            instance = null
        }
    }
}
