# 🚨 NEVER BUILD LOCALLY 🚨

## ⚠️ READ THIS BEFORE ATTEMPTING ANY BUILD

**This project does NOT support local builds on device (Termux).**

**ALL builds are handled exclusively by GitHub Actions.**

---

## Why This Policy Exists

Building Android apps on Termux/mobile devices is:

### ❌ Resource-Intensive
- Requires 6GB+ RAM (most phones have 4-8GB total)
- Takes 30+ minutes for a clean build
- Drains battery significantly
- Heats up the device

### ❌ Unreliable
- Frequent Out-Of-Memory (OOM) crashes
- Gradle daemon crashes
- Compilation timeouts
- Corrupted build artifacts
- Inconsistent build environments

### ❌ Unnecessary
- GitHub Actions provides free CI/CD
- Consistent, reproducible builds
- Automated testing and checks
- Build artifacts automatically published
- No local resource consumption

---

## How to Get a Build

### Step 1: Make Your Changes
```bash
# Edit code as needed
# Run code formatting (this is safe)
./gradlew spotlessApply
```

### Step 2: Commit and Push
```bash
git add .
git commit -m "feat: your changes"
git push
```

### Step 3: Let GitHub Actions Build
- GitHub Actions will automatically build on push
- View progress: `gh run list` or check GitHub website
- Build takes ~10-15 minutes on GitHub servers

### Step 4: Download the APK
```bash
# Download from latest release
gh release download nightly

# Or from GitHub web interface
# https://github.com/YOUR_REPO/releases/tag/nightly
```

---

## What Commands ARE Safe?

### ✅ Safe to Run Locally

These commands are lightweight and won't cause issues:

```bash
# Code formatting (always run before committing)
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# List available gradle tasks
./gradlew tasks

# View help
./gradlew help

# Git operations
git status
git add .
git commit -m "message"
git push

# GitHub CLI operations
gh run list
gh run watch
gh release list
gh release download
```

### ❌ NEVER Run These Locally

These commands are **FORBIDDEN** on Termux:

```bash
# Build commands
./gradlew build               # ❌ WILL FAIL
./gradlew assembleDebug       # ❌ WILL FAIL
./gradlew assembleRelease     # ❌ WILL FAIL
./gradlew installDebug        # ❌ WILL FAIL
./gradlew bundleDebug         # ❌ WILL FAIL
./gradlew bundleRelease       # ❌ WILL FAIL

# Clean builds
./gradlew clean build         # ❌ WILL FAIL

# Any task with 'assemble', 'build', 'bundle', or 'install'
```

---

## Enforcement Mechanisms

This policy is enforced through multiple layers:

### 1. **build.gradle Check** ✅
- Detects Termux environment
- Throws error if build tasks attempted
- Shows helpful error message

### 2. **Documentation** ✅
- CONTRIBUTING.md - Build policy section
- AUTOCAT_KNOWLEDGE_BASE.md - Build policy reminder
- .claude/README.md - AI assistant instructions
- This file (NEVER_BUILD_LOCALLY.md)

### 3. **Build Artifacts Removed** ✅
- `.gradle/` directory removed
- `build/` directory removed
- All local build caches cleared

### 4. **GitHub Actions** ✅
- Automated builds on every push
- Consistent build environment
- Published to GitHub Releases

---

## What If I REALLY Need a Build Right Now?

If you absolutely need a build immediately:

### Option 1: Trigger Manual GitHub Actions Run
```bash
# Trigger a workflow manually
gh workflow run build.yml

# Watch the progress
gh run watch
```

### Option 2: Use GitHub Codespaces
- Open repo in GitHub Codespaces (cloud IDE)
- Build in the cloud environment
- Download APK from artifacts

### Option 3: Wait for Automated Build
- Just push your changes
- GitHub Actions builds automatically
- Usually completes in 10-15 minutes

---

## Error Messages You Might See

### If You Try to Build Locally

```
╔════════════════════════════════════════════════════════════════╗
║                      🚨 BUILD NOT ALLOWED 🚨                    ║
╠════════════════════════════════════════════════════════════════╣
║                                                                ║
║  This project does NOT support local builds on device.        ║
║  ALL BUILDS are handled by GitHub Actions.                    ║
║  ...                                                           ║
╚════════════════════════════════════════════════════════════════╝
```

**This is intentional.** Follow the instructions in the error message.

---

## For Contributors

See **CONTRIBUTING.md** for the full contribution guide, including:
- Development workflow
- Commit message conventions
- How to test changes
- Pull request process

**Remember**: You can develop, test, and contribute WITHOUT building locally.

---

## For AI Assistants (Claude, etc.)

When working on this project:

1. **NEVER suggest or run build commands** (`./gradlew assemble*`, `./gradlew build`, etc.)
2. **Always remind users** to push changes and use GitHub Actions
3. **Only run safe commands** (spotlessApply, git operations, gh CLI)
4. **Read this file** at the start of each session as a reminder

---

## Questions?

- **Q: Why can't I build locally even once?**
  - A: It will likely crash, waste hours, and produce unreliable results. CI/CD is faster and more reliable.

- **Q: What if GitHub Actions is down?**
  - A: Wait for it to come back up, or use GitHub Codespaces temporarily.

- **Q: Can I override this for testing?**
  - A: No. The build.gradle check will prevent it. This is for your own good.

- **Q: What about release builds?**
  - A: Especially NO. Release builds are even more resource-intensive and should ONLY be done in CI/CD.

---

## Summary

| Action | Where | Result |
|--------|-------|--------|
| `./gradlew assembleDebug` | Termux | ❌ **FAILS** with error |
| `./gradlew spotlessApply` | Termux | ✅ Works perfectly |
| `git push` | Termux | ✅ Triggers GitHub Actions |
| Build APK | GitHub Actions | ✅ **Automated & reliable** |
| Download APK | Termux | ✅ `gh release download` |

---

**Last Updated**: 2025-12-09

**For more information, see:**
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [AUTOCAT_KNOWLEDGE_BASE.md](AUTOCAT_KNOWLEDGE_BASE.md) - Project knowledge base
- [.claude/README.md](.claude/README.md) - AI assistant instructions
