# AutoCat Changelog

All notable changes to AutoCat will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## Versioning Scheme

**Format**: `15.0.b1-autocat.{BUILD_NUMBER}`

- `15.0.b1` = Lawnchair 15.0 Beta 1 (base version we're forking from)
- `autocat` = Fork identifier
- `{BUILD_NUMBER}` = Incremental build number from GitHub Actions

Example: `v15.0.b1-autocat.5` = AutoCat build #5 based on Lawnchair 15.0 Beta 1

When Lawnchair releases Beta 2, we'll update to `15.0.b2-autocat.X`

---

## [Unreleased]

## [15.0.b1-autocat.6] - 2025-11-23 (Upcoming)

### Changed
- **Versioning Format Update**: Changed from `15.0-autocat.X` to `15.0.b1-autocat.X`
  - Now explicitly tracks Lawnchair beta version (b1 = Beta 1)
  - Makes it clearer which Lawnchair beta we're based on
  - When Lawnchair updates to Beta 2, we'll update to `15.0.b2-autocat.X`

## [15.0.b1-autocat.5] - 2025-11-23

### Added
- **AutoCat Versioning System**: Proper version tracking for AutoCat builds
  - Version format: `15.0.b1-autocat.{BUILD_NUMBER}`
  - Version code increments with each build
  - Build info accessible via `BuildConfig.AUTOCAT_VERSION`
- **Versioned Releases**: Each build creates a permanent versioned release
  - `dev-latest` = Rolling release (always latest)
  - `v15.0.b1-autocat.X` = Permanent versioned releases
- **Enhanced Release Notes**: Detailed build information in GitHub releases

### Changed
- `build.gradle`: Updated versioning to track AutoCat builds separately
- GitHub Actions workflow: Creates both versioned and rolling releases

## [15.0.b1-autocat.4] - 2025-11-23

### Added
- **APK Release Automation**: Automatic GitHub releases for easy testing
  - `dev-latest` release created on every push
  - Direct APK download links
  - Detailed release notes with commit info
- **TESTING.md**: Comprehensive testing documentation
  - Download and installation instructions
  - Troubleshooting guide
  - ADB and Termux testing methods

### Changed
- `README.md`: Added Download & Testing section

## [15.0.b1-autocat.3] - 2025-11-23

### Added
- **Development Logs System**: Conversation tracking for project transparency
  - `dev-logs/` directory for session transcripts
  - Full user/assistant conversations with technical context
  - Session summaries with decisions and next steps
- **dev-logs/README.md**: Documentation for dev-logs usage
- **dev-logs/2025-11-23-session-1.md**: First development session log

### Changed
- `README.md`: Updated to reflect AutoCat fork status and features

## [15.0.b1-autocat.2] - 2025-11-23

### Added
- **Room Database Foundation**: Complete database schema for app categorization
  - `AppCategory` entity: Stores app-to-category mappings with confidence scores
  - `CustomCategory` entity: User-defined categories with colors and ordering
  - `CategoryDao`: 30+ database operations with Flow-based reactive queries
  - `CategoryDatabase`: Singleton Room database with migration support
  - Unit tests for entity classes (`AppCategoryTest`, `CustomCategoryTest`)

### Technical Details
- **Database Version**: 1
- **Entities**: AppCategory, CustomCategory
- **Package**: `app.lawnchair.data.category`
- **Room Version**: 2.8.4
- **Confidence Scoring**: 0.0-1.0 scale for categorization reliability
- **Source Tracking**: built-in, rule, ml, user

### Future Pipeline
This database supports a planned 4-stage categorization pipeline:
1. Built-in (Android system categories) - Confidence: 0.95
2. Rule-based (package patterns, permissions) - Confidence: 0.70-0.80
3. ML inference (TensorFlow Lite) - Confidence: 0.60+
4. User overrides (manual categorization) - Always takes precedence

## [15.0.b1-autocat.1] - 2025-11-23

### Added
- **Initial Fork**: Forked from Lawnchair 15.0 Beta 1
- **Project Setup**:
  - Repository initialized at https://github.com/thejaustin/AutoCat
  - Git submodules initialized
  - GitHub Actions CI configured

### Infrastructure
- **Build System**: GitHub Actions (no local Gradle builds)
- **CI Workflow**: Automatic builds on push to `15-dev`
- **Architecture**: All new code in `lawnchair/` package (preserves upstream base)

---

## What's Next?

### Planned Features

**v15.0.b1-autocat.7+**:
- [ ] AppMetadataProvider wrapper (wraps PackageManager)
- [ ] Rule-based categorizer (package patterns, permissions, keywords)
- [ ] Integration with app drawer
- [ ] Basic categorized drawer UI (section headers)
- [ ] Settings UI (Jetpack Compose)

**Future**:
- [ ] ML categorization (TensorFlow Lite integration)
- [ ] Custom category creation UI
- [ ] Category editing and management
- [ ] App install/uninstall listeners for auto-categorization
- [ ] Export/import category configurations
- [ ] Category icons and themes

## Testing a Specific Version

Each version creates a permanent release on GitHub:

```bash
# Latest (rolling)
https://github.com/thejaustin/AutoCat/releases/tag/dev-latest

# Specific version
https://github.com/thejaustin/AutoCat/releases/tag/v15.0.b1-autocat.5
```

## Reporting Issues

When reporting issues, always include:
- **Version**: Check Settings → Apps → AutoCat → Version
- **Build number**: From APK filename or release notes
- **Commit hash**: 7-character hash from release notes

---

## Base Version

AutoCat is based on **Lawnchair 15.0 Beta 1** (versionCode: 15_00_02_00)

Upstream: https://github.com/LawnchairLauncher/lawnchair
