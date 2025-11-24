package app.lawnchair.data.category

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.lawnchair.data.category.entities.AppCategory
import app.lawnchair.data.category.entities.CustomCategory

/**
 * Room database for app categorization and custom category management.
 *
 * This database stores:
 * - App categorization data (which apps belong to which categories)
 * - Custom category definitions (user-created categories with colors and ordering)
 *
 * Database version: 1
 * Export schema: false (disabled for development, will enable for production)
 */
@Database(
    entities = [
        AppCategory::class,
        CustomCategory::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CategoryDatabase : RoomDatabase() {

    /**
     * Provides access to category-related data operations.
     */
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DATABASE_NAME = "category_database"

        @Volatile
        private var instance: CategoryDatabase? = null

        /**
         * Gets the singleton instance of CategoryDatabase.
         *
         * Uses double-checked locking to ensure thread-safe singleton creation.
         * The database is created with fallback to destructive migration for
         * development purposes.
         *
         * @param context Application context
         * @return Singleton CategoryDatabase instance
         */
        fun getInstance(context: Context): CategoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        /**
         * Builds the Room database instance with appropriate configuration.
         */
        private fun buildDatabase(context: Context): CategoryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CategoryDatabase::class.java,
                DATABASE_NAME,
            )
                // For development: destroy and rebuild on schema changes
                // TODO: Replace with proper migrations before production release
                // dropAllTables = true: all tables will be dropped on migration failure
                .fallbackToDestructiveMigration(dropAllTables = true)
                // Initialize default categories on first run
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Default categories will be initialized on first DAO access
                        // via CategoryDao.initializeDefaultCategoriesIfNeeded()
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
