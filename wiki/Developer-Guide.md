# Developer Guide

Complete guide for contributing to AutoCat development.

## 🚀 Getting Started

### Prerequisites

- **Git** with submodule support
- **Android Studio** (latest stable version)
- **Java 21 JDK**
- **GitHub account**
- **GitHub CLI** (`gh`) for releases (optional)

### Clone Repository

```bash
git clone --recursive https://github.com/thejaustin/AutoCat.git
cd AutoCat
```

If you forget `--recursive`, initialize submodules:

```bash
git submodule update --init --recursive
```

### Open in Android Studio

1. Open Android Studio
2. File → Open → Select AutoCat directory
3. Select build variant: `lawnWithQuickstepGithubDebug`
4. Wait for Gradle sync

### Fix Submodule Errors

If you see errors with `iconloaderlib` or `searchuilib`:

```bash
git submodule update --init --recursive
```

---

## 🚨 CRITICAL: Build Policy

### NEVER BUILD LOCALLY

**ALL builds are handled exclusively by GitHub Actions.**

### Why?

Building Android apps on Termux/mobile devices is:

- **Resource-Intensive**: Requires 6GB+ RAM, takes 30+ minutes, drains battery
- **Unreliable**: Frequent OOM crashes, Gradle daemon crashes, corrupted artifacts
- **Unnecessary**: GitHub Actions provides free, consistent, reproducible builds

### How to Get a Build

#### Step 1: Make Your Changes
```bash
# Edit code as needed
# Run code formatting (this is safe)
./gradlew spotlessApply
```

#### Step 2: Commit and Push
```bash
git add .
git commit -m "feat: your changes"
git push
```

#### Step 3: Let GitHub Actions Build
- GitHub Actions will automatically build on push
- View progress: `gh run list` or check GitHub website
- Build takes ~10-15 minutes on GitHub servers

#### Step 4: Download the APK
```bash
# Download from latest release
gh release download dev-latest

# Or from GitHub web interface
# https://github.com/thejaustin/AutoCat/releases/tag/dev-latest
```

### Safe Commands (Run Locally)

✅ **Safe to run**:
```bash
# Code formatting
./gradlew spotlessApply
./gradlew spotlessCheck

# Gradle info
./gradlew tasks
./gradlew help
./gradlew projects

# Git operations
git status
git add .
git commit -m "message"
git push

# GitHub CLI
gh run list
gh run watch
gh release list
gh release download
```

### Forbidden Commands (NEVER Run)

❌ **NEVER run these**:
```bash
./gradlew build               # ❌ WILL FAIL
./gradlew assembleDebug       # ❌ WILL FAIL
./gradlew assembleRelease     # ❌ WILL FAIL
./gradlew installDebug        # ❌ WILL FAIL
./gradlew bundleDebug         # ❌ WILL FAIL
./gradlew clean build         # ❌ WILL FAIL
```

---

## 📝 Development Workflow

### Tiered Workflow

We use a tiered workflow to balance development speed with stability. All PRs target the `15-dev` branch.

| Tier | Definition | Examples | Protocol |
|------|-----------|----------|----------|
| **Trivial changes** | Zero risk of functional regression | Fixing typos, simple code style fixes | Commit **directly** to 15-dev |
| **Simple, self-contained work** | Functionally isolated, very low risk | Single-file bug fixes, minor UX polish | 1. Create PR<br>2. Assign reviewer<br>3. Enable Auto-merge |
| **Medium complexity features** | Multiple components, not deeply architectural | New settings screen, new search provider | 1. Create detailed PR<br>2. Assign core team |
| **Major architectural changes** | High-risk, complex changes | Android version rebase | 1. Very detailed PR<br>2. **Mandatory Review** before merge |

### Commit Message Convention

We follow **[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)**:

**Format**: `type(scope): subject`

**Example**: `feat(settings): Add toggle for new feature`

**Allowed Types**:
- `feat` - New feature
- `fix` - Bug fix
- `style` - Code style changes (formatting)
- `refactor` - Code refactoring
- `perf` - Performance improvements
- `docs` - Documentation changes
- `test` - Test additions/changes
- `chore` - Build/tooling changes

**AutoCat-Specific Convention**:

All commits automatically include attribution footer:

```
🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

---

## 🏗️ Project Structure

### Package Organization

```
lawnchair/
├── src/app/lawnchair/          # AutoCat-specific code (ADD NEW FILES HERE)
│   ├── categorization/         # Categorization system
│   │   ├── llm/                # LLM providers
│   │   ├── stages/             # Categorization pipeline
│   │   └── learning/           # User correction learning
│   ├── data/tab/               # Database entities & DAOs
│   ├── ui/preferences/         # Settings screens
│   └── preferences/            # Preference definitions
│
src/                            # Launcher3 codebase (MINIMIZE CHANGES)
└── com/android/launcher3/      # Upstream Launcher3 code
```

**Rule**: Always add new files to `lawnchair/src/app/lawnchair/`, not `src/`.

### AutoCat-Specific Files

**42 AutoCat Files** across 5 packages:

```
app.lawnchair.categorization/ (19 files)
├── AccuracyTracker.kt
├── AdaptiveModelSelector.kt
├── CategorizationManager.kt
├── TabFolderSyncService.kt
├── llm/
│   ├── GoogleAIProvider.kt
│   ├── ClaudeProvider.kt
│   ├── OpenAIProvider.kt
│   ├── PerplexityProvider.kt
│   ├── LLMProvider.kt
│   ├── ProviderCircuitBreaker.kt
│   └── ConfidenceCalibrator.kt
└── stages/
    ├── BuiltInCategorizer.kt
    └── LLMCategorizer.kt

app.lawnchair.data.tab/ (7 files)
├── TabDatabase.kt
├── TabDao.kt
├── AccuracyDao.kt
└── entities/
    ├── AppTab.kt
    ├── CustomTab.kt
    └── ModelAccuracy.kt

app.lawnchair.ui.preferences.destinations/ (8 files)
├── AppTabAssignmentPreferences.kt       Phase 4 (renamed)
├── AppCategorizationPreferences.kt      Phase 20 (renamed)
├── CategorizationProgress.kt            Phase 6
├── CategorizationSettingsPreferences.kt Phase 20
├── TabManagementPreferences.kt          Phase 4 (renamed)
└── LLMSettingsPreferences.kt            Phase 5
```

See [AutoCat vs Upstream](AutoCat-vs-Upstream.md) for complete file list.

---

## 💻 Coding Guidelines

### Kotlin Conventions

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use camelCase for variables/functions
- Use PascalCase for classes
- 4 spaces for indentation
- Max line length: 120 characters
- Always use explicit types for public APIs

### Code Formatting

**Always run before committing**:

```bash
./gradlew spotlessApply
```

Check formatting:

```bash
./gradlew spotlessCheck
```

### String Naming

Strings in `strings.xml` follow this format:

| Type | Format | Example |
|------|--------|---------|
| Generic word | `$1` | `disagree_or_agree` |
| Action | `$1_action` | `apply_action` |
| Preference/popup label | `$1_label` | `folders_label` |
| Preference description | `$1_description` | `folders_description` |
| Preference choice | `$1_choice` | `off_choice` |
| Feature string | `(feature_name)_$1` | `colorpicker_hsb` |
| Launcher string | `$1_launcher` | `device_contacts_launcher` |

### Database Migrations

When changing database schema:

1. **Increment version number** in `TabDatabase.kt`
2. **Add migration logic** (or use `fallbackToDestructiveMigration()` for dev)
3. **Test migration** with existing data
4. **Document changes** in commit message

Example:
```kotlin
@Database(
    entities = [AppTab::class, CustomTab::class, ModelAccuracy::class],
    version = 7,  // Increment this
    exportSchema = false,
)
```

---

## 🧪 Testing

### Manual Testing Checklist

When testing a feature:

- [ ] Feature works as expected
- [ ] No crashes or errors
- [ ] Settings save and persist
- [ ] Works after app restart
- [ ] No performance degradation
- [ ] UI looks correct on different screen sizes
- [ ] Logs show expected output

### Reporting Bugs

Use GitHub Issues template. Include:

- **Build info**: Commit hash (e.g., `AutoCat-debug-abc1234.apk`)
- **Android version**: From Settings → About Phone
- **Device model**: Manufacturer and model
- **Steps to reproduce**: Exact steps
- **Expected vs actual**: What should happen vs what happens
- **Logs**: `adb logcat | grep AutoCat`

---

## 🔀 Version Control

### Branching Strategy

- **Main branch**: `15-dev`
- **Feature branches**: `feature/your-feature-name`
- **Bug fixes**: `fix/issue-number-description`

### Creating a Pull Request

1. **Create branch**:
   ```bash
   git checkout -b feature/new-feature
   ```

2. **Make changes** and commit:
   ```bash
   git add .
   git commit -m "feat: Add new feature"
   ```

3. **Push to GitHub**:
   ```bash
   git push origin feature/new-feature
   ```

4. **Create PR** on GitHub:
   - Base branch: `15-dev`
   - Include description of changes
   - Link related issues
   - Enable auto-merge (for simple PRs)

5. **Wait for CI**:
   - GitHub Actions will build and test
   - Fix any errors
   - Get review approval

6. **Merge**:
   - Auto-merge will merge when CI passes and reviewer approves
   - Or manually merge after review

---

## 🎨 UI Development

### Material 3 Expressive Design

AutoCat uses Material 3 Expressive design system:

- **Typography**: Material 3 type scale
- **Colors**: Material 3 dynamic colors
- **Spacing**: 8dp grid system
- **Components**: Material 3 components

### Preference Screens

Use existing preference components:

```kotlin
@Composable
fun MyPreferences() {
    PreferenceLayout(label = "My Settings") {
        PreferenceGroup(heading = "Section 1") {
            SwitchPreference(
                adapter = preferenceManager.mySetting.getAdapter(),
                label = "My Setting"
            )
        }
    }
}
```

See [Preference Components README](../lawnchair/src/app/lawnchair/ui/preferences/components/README.md) for details.

---

## 🔧 Common Tasks

### Adding a New LLM Provider

1. **Create provider class** in `app.lawnchair.categorization.llm/`:
   ```kotlin
   class MyLLMProvider(context: Context) : LLMProvider {
       override suspend fun categorizeApp(...): CategorizationResult {
           // Implementation
       }
   }
   ```

2. **Add to provider registry** in `CategorizationManager.kt`

3. **Add preferences** in `PreferenceManager.kt`:
   ```kotlin
   val llmMyProviderKey = StringPref("pref_llmMyProviderKey", "", {})
   val llmMyProviderModel = StringPref("pref_llmMyProviderModel", "default-model", {})
   ```

4. **Add UI** in `LLMSettingsPreferences.kt`

5. **Test** with real API key

### Adding a New Database Entity

1. **Create entity** in `app.lawnchair.data.tab.entities/`:
   ```kotlin
   @Entity(tableName = "my_table")
   data class MyEntity(
       @PrimaryKey val id: Int,
       @ColumnInfo(name = "field") val field: String
   )
   ```

2. **Add to database** in `TabDatabase.kt`:
   ```kotlin
   @Database(
       entities = [AppTab::class, CustomTab::class, MyEntity::class],
       version = 7,  // Increment
   )
   ```

3. **Create DAO** in `app.lawnchair.data.tab/`:
   ```kotlin
   @Dao
   interface MyDao {
       @Query("SELECT * FROM my_table")
       suspend fun getAll(): List<MyEntity>
   }
   ```

4. **Add DAO to database**:
   ```kotlin
   abstract fun myDao(): MyDao
   ```

---

## 📚 Additional Resources

### Documentation

- [Lawnchair Wiki](https://github.com/LawnchairLauncher/lawnchair/wiki)
- [Lawnchair Visual Guidelines](../docs/assets/README.md)
- [AutoCat Complete History](../AUTOCAT_COMPLETE_HISTORY.md)
- [AutoCat Knowledge Base](../AUTOCAT_KNOWLEDGE_BASE.md)

### Communication

- [Telegram](https://t.me/lccommunity) - Lawnchair community
- [Discord](https://discord.com/invite/3x8qNWxgGZ) - Lawnchair Discord

### Code of Conduct

Be civil and respectful. See [Code of Conduct](../CODE_OF_CONDUCT.md).

---

## 🆘 Getting Help

### For Development Questions

1. Check existing documentation
2. Search closed issues on GitHub
3. Ask in Telegram/Discord
4. Open a discussion on GitHub

### For Bug Reports

1. Check if issue already reported
2. Test with latest build
3. Create issue with all required information
4. Be patient and responsive to questions

---

*Last Updated: 2025-12-25*
