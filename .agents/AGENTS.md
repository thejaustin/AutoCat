# Workspace Rules & Guidelines for AutoCat Development

These rules apply to any agent working on the AutoCat project inside this workspace.

## 🚨 Build Policy
- **Build exclusively via GitHub Actions**: Never perform release builds or final verification locally.
- **Local checking**: Only run `./gradlew spotlessCheck` and `./gradlew spotlessApply` for formatting verification before pushing code.

## 🎨 Branding & Naming Standards
- Keep the branding consistent: use `AutoCat` in all class names, file names, assets, and text logs rather than the upstream name `Lawnchair`.

## 🏗️ Architecture & Style Guidelines
- **Kotlin First**: All new features and modifications must be implemented in Kotlin.
- **Coroutines**: Use Kotlin coroutines for all asynchronous tasks, performing any I/O strictly on `Dispatchers.IO`.
- **Dagger/Hilt**: Respect the dependency injection graph. Keep DI annotations aligned with `LauncherAppComponent` and `LauncherAppSingleton`.
- **Database Modifying**: Room database changes must be accompanied by proper migrations or schema updates. Never bypass schema verification.
- **API Keys**: Keys must never be hardcoded. Load keys securely via `PreferenceManager2`.
