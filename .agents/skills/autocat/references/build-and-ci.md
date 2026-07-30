# AutoCat Build & CI/CD Guide

This guide details code quality checks, local build limitations, CI configuration, and development environment setup.

## 🧹 Code Quality & Formatting

AutoCat uses Spotless for enforcing Kotlin and Java code styles.

- **Check Formatting**:
  Run this command to check if code meets style requirements:
  ```bash
  ./gradlew spotlessCheck
  ```
- **Apply Formatting**:
  Run this command to automatically format modifications to conform to the style guidelines:
  ```bash
  ./gradlew spotlessApply
  ```

## 🚨 Build Policy: GitHub Actions Only

> [!WARNING]
> **Do not build release APKs locally.** Always push changes and let GitHub Actions run the build pipeline.

- **Main Workflows**:
  - `ci.yml`: Runs on every PR/push to verify the code compile and passes spotless checking.
  - `build_release_apk.yml`: Handles compilation, signing, and exporting of release APKs.
- **Gradle Version**:
  The gradle wrapper version is strictly pinned to `9.2.1` for CI stability. Ensure you do not upgrade it without updating CI actions.

## 🛠️ Environment Configuration (Termux / Android)

### PRoot Nesting Workaround
If running gradle commands inside a nested PRoot container in Termux triggers an error stating `proot-distro should not be executed under PRoot`, follow these steps to disable the nested proot check:

1. Locate `cli.py` at `/usr/lib/python3.13/site-packages/proot_distro/cli.py` (or corresponding python version folder).
2. Open the file and locate the check function `_refuse_nested_proot`.
3. Comment out the contents of `_refuse_nested_proot` or make it return immediately:
   ```python
   def _refuse_nested_proot():
       return # Disable nested proot check
   ```
