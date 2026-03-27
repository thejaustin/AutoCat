# Claude Code Configuration for AutoCat

This file contains configuration and context for Claude Code when working on the AutoCat Android launcher project.

## Project Overview
AutoCat is a custom Android launcher based on Lawnchair, featuring advanced customization and performance optimizations.

## Development Commands
- **Build**: `./gradlew assembleDebug`
- **Test**: `./gradlew test`
- **Lint**: `./gradlew lint`
- **Clean**: `./gradlew clean`
- **Install Debug**: `./gradlew installDebug`

## Key Directories
- `lawnchair/src/` - Main launcher source code
- `quickstep/src/` - Quickstep/recents integration
- `systemUI/` - System UI components
- `src/` - Core launcher components
- `docs/` - Documentation
- `tools/` - Development tools and scripts

## Important Files
- `build.gradle` - Main build configuration
- `gradle.properties` - Build properties
- `gradle/libs.versions.toml` - Dependency versions
- `codex-inspect.init.gradle` - Codex development tools

## Code Style
- Follow Android/Kotlin conventions
- Use existing patterns and utilities
- Maintain compatibility with AOSP Launcher3
- Test changes thoroughly before committing

## Architecture Notes
- Built on top of AOSP Launcher3
- Uses Dagger for dependency injection
- Implements custom preferences system
- Integrates with Android's quickstep system

## Complete Migration Status ✅
This project has been fully migrated from GitHub AutoCat with:
- ✅ Claude Code configuration (.claude/settings.local.json)
- ✅ GitHub workflows and CI/CD (.github/)
- ✅ Development tools and scripts (tools/)
- ✅ Build configurations and lint settings
- ✅ IDE configurations (.idea/)
- ✅ Fastlane deployment setup
- ✅ Localization configuration (crowdin.yml)
- ✅ Development utilities (ci.py, fill_screens.py, flowerpot/)

## Available Scripts & Tools
- `./tools/dev-build.ps1` - Build debug APK
- `./tools/dev-install.ps1` - Install to device/emulator
- `./tools/dev-android.ps1` - Android development helper
- `./tools/claude-setup.ps1` - Complete environment setup
- `./tools/claude-launch.bat` - Start Claude Code
- `ci.py` - Continuous integration script
- `fill_screens.py` - Screen filling utility
- `flowerpot/` - Release management tools

## Common Tasks
When making changes:
1. Always check existing implementations first
2. Use the project's existing libraries and patterns
3. Test on multiple Android versions if possible
4. Update documentation as needed
5. Follow the existing commit message style
6. Use the provided development scripts for consistency

## Git Workflow
- Work on feature branches
- Use descriptive commit messages
- Test before pushing
- Create PRs for code review

## Performance Considerations
- Monitor app startup time
- Optimize UI rendering
- Consider memory usage
- Profile critical paths

## Security
- Never commit API keys or secrets
- Follow Android security best practices
- Validate user inputs
- Use secure storage when needed