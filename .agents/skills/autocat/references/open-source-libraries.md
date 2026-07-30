# AutoCat Open Source Library Integrations

This guide details the integration details, safety protocols, and extension patterns for the primary open-source libraries used in AutoCat.

---

## 1. Shizuku & libsu (System & Root Execution)

AutoCat uses **Shizuku** (for non-root system API proxying) and **libsu** (for root execution fallback) to execute administrative shell commands (e.g. app archiving, package enabling/disabling).

### Key Files
- `ShizukuManager.kt`: Orchestrator for querying Shizuku status, binding the background `UserService` daemon, and running shell commands.
- `UserService.kt` & `IUserService.aidl`: The privileged daemon process executed in Shizuku's context.

### Safety & Security Policies
- **Input Sanitization**: Command execution can be vulnerable to shell injection. Always validate and sanitize input. For example, package names are checked against `PACKAGE_NAME_REGEX = Regex("^[a-zA-Z0-9_.]+$")` before execution.
- **Binder Lifecycle**: Always verify binder presence via `Shizuku.pingBinder()` and check permissions via `Shizuku.checkSelfPermission()` before attempting to bind services.
- **Root Fallback**: If Shizuku is disabled or unavailable, check root availability using `Shell.getShell().isRoot` and execute commands via `Shell.cmd()`.

### ShizukuPlus Native Support
- AutoCat natively supports ShizukuPlus (`af.shizuku.plus.api`).
- To allow direct connection without requiring the Compat Hub companion app, AutoCat declares the custom ShizukuPlus permission in its manifest: `<uses-permission android:name="af.shizuku.plus.permission.API_V23" />`.
- When ShizukuPlus service runs, it automatically binds to AutoCat's `${applicationId}.shizuku` provider endpoint and supplies the binder. AutoCat uses the standard `rikka.shizuku.Shizuku` API wrapper to transact with it seamlessly.

---

## 2. Room Database (Data Persistence)

Persistence for categories, tab mappings, and model accuracy is handled by **Room**.

### Key Files
- `TabDatabase.kt`: The main abstract `RoomDatabase` class.
- `TabDao.kt` & `AccuracyDao.kt`: Data Access Objects (DAOs).
- `entities/AppTab.kt`, `entities/CustomTab.kt`, `entities/ModelAccuracy.kt`.

### Migration Strategy
- **Development vs. Production**: During active development, `fallbackToDestructiveMigration(dropAllTables = true)` is used to auto-rebuild tables on schema changes. For production upgrades, formal migrations are required.
- **Writing Migrations**: Add migrations inside `TabDatabase.buildDatabase`. Example migrating from version 6 to 7:
  ```kotlin
  val migration67 = object : androidx.room.migration.Migration(6, 7) {
      override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE custom_categories ADD COLUMN hide_in_zen_mode INTEGER NOT NULL DEFAULT 0")
      }
  }
  ```
  Ensure all migrations are registered using `.addMigrations(migration67)`.

---

## 3. Opto (Preference Management)

Instead of raw `SharedPreferences` or standard `DataStore`, AutoCat uses **Opto**, a modern preference management framework designed specifically for Kotlin and Jetpack Compose.

### Key Files
- `PreferenceManager.kt` & `PreferenceManager2.kt`: Registries where settings items are instantiated as Opto preferences.

### Key Usage Patterns
- **Retrieve Values**: Use `.get()` to retrieve an flow representing preference updates, or `.firstBlocking()` to read synchronously.
- **Dagger Injection**: Keep Dagger constructors aligned. Inject `PreferenceManager` and `PreferenceManager2` instead of instantiating them on-demand where possible.

---

## 4. FuzzyWuzzy (Fuzzy String Matching)

Fuzzy matching is used in AutoCat's app search to provide resilient search results.

### Key Files
- `SearchUtils.kt` & `AppMatcher.kt`.

### Implementation Patterns
- **Ratio Calculations**: Uses `FuzzySearch.ratio(app, query)` for standard levenshtein-distance matches.
- **Initials & Token Matchers**: Custom logic tokenizes search strings by whitespace and evaluates prefix/initial matches first (e.g. "G" or "GM" matches "Google Maps") before falling back to Fuzzy Levenshtein matching.
