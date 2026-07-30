---
name: autocat
description: Guidelines and instructions for developing, testing, and troubleshooting AutoCat (an AI-powered app categorization launcher based on Lawnchair 16).
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-07-10'
  keywords:
  - AutoCat
  - Lawnchair
  - LLM
  - Android
  - Room
  - Gradle
---
# AutoCat Development Specialist

This skill provides comprehensive instructions for developing, testing, and debugging the AutoCat launcher (AI-powered app categorization launcher).

## Prerequisites & Environment
- Development is done in a Termux environment on Android.
- If proot nesting errors occur, disable the check by patching `usr/lib/python3.13/site-packages/proot_distro/cli.py` (comment out `_refuse_nested_proot`).

## Workflows
This skill enables the caller to work on the following aspects of AutoCat:
- *[Architecture & Logic](references/architecture.md)*: Details on the categorization pipeline (LLM & Built-In), database schemas, provider configurations, and folder sync strategies.
- *[Build & CI/CD](references/build-and-ci.md)*: Formatting checks with Spotless, dependency management, and running debugging builds. Remember the build policy: always build officially with GitHub Actions.
- *[Testing & Diagnostics](references/testing-diagnostics.md)*: How to verify LLM providers, test databases, troubleshoot proot environments, and run developer diagnostics.
- *[Open Source Library Integrations](references/open-source-libraries.md)*: In-depth technical guides for Shizuku, Room DB, Opto settings, and FuzzyWuzzy matching.

## Critical Constraints
- **GitHub Actions Build Policy**: NEVER perform official release builds locally. Always run local formatting checks (`./gradlew spotlessCheck` / `./gradlew spotlessApply`) and push to let GitHub Actions build the APK.
- **Kotlin-First**: Implement all new features in Kotlin.
- **Database Safety**: Respect Room schemas and run migration tests when modifying database entities.
- **API Key Security**: Never hardcode API keys. Retrieve them from `PreferenceManager2` or user configuration.
