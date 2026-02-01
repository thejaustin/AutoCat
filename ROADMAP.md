# AutoCat development roadmap

This document outlines our high-level strategic priorities. It's a living document, not a set of
unbreakable promises. For the status of individual tasks, see
our [GitHub Issue Tracker](https://github.com/thejaustin/AutoCat/issues).

## Vision

AutoCat's goal is to be:

* **Intelligent:** Use AI to make app organization effortless.
* **Simple:** Match the core Pixel Launcher experience while adding power.
* **Stable:** Provide a rock-solid, reliable foundation based on Lawnchair 16.

## Roadmap

### Recently completed (AutoCat Overhaul)

- **Versioning:** Migrated to clean 6-digit `160000+` versioning scheme for the fork.
- **Branding:** Full internal codebase refactor from Lawnchair to AutoCat (Classes, Styles, Log Tags).
- **LLM Reliability:** Implemented exponential backoff and rate-limit handling for AI providers.
- **Diagnostics:** Added a dedicated Developer Diagnostics UI for database and log inspection.
- **Parallelism:** Optimized LLM batch categorization with parallel coroutine processing.

### Current focus

This is our active development sprint. The goal is to address key bugs and deliver a highly polished
user experience.

- UI/UX overhaul of all Settings screens to Material 3 Expressive.
- Syncing AutoCat categories to Home Screen folders (Workspace support).

### Up next

Once the UX overhaul is stable, our focus will shift to delivering highly-requested features that
enhance
customization and control.

- Proper icon swipe gestures (AutoCat style)
- Folder "cover" mode

### The Android 16 Base

We have successfully rebased onto the latest Android 16 (AOSP) source. AutoCat is now tracking
the `16-dev` branch as its primary development target.

### Long term or blocked

Highly-requested features that are blocked by external dependencies or require significant research.

- **Widget Stacking:** A highly complex feature requiring deep architectural investigation.
- **On-Device ML:** Exploring local categorization models to reduce API dependency.

