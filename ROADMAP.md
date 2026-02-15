# AutoCat development roadmap

This document outlines our high-level strategic priorities. It's a living document, not a set of
unbreakable promises. For the status of individual tasks, see
our [GitHub Issue Tracker](https://github.com/thejaustin/AutoCat/issues).

## Vision

AutoCat's goal is to be:

* **Intelligent:** Use AI to make app organization effortless.
* **Simple:** Match the core Pixel Launcher experience while adding power.
* **Stable:** Provide a rock-solid, reliable foundation based on AutoCat 16.

## Roadmap

### Recently completed (Development 4 Milestone)

- **Full Branding Migration:** Completed the transition from Lawnchair to AutoCat across all internal classes, file structures, and log tags.
- **AI Semantic Search:** Implemented intent-based searching, allowing users to find apps by purpose (e.g., searching "Finance" for banking apps).
- **Dual Folder Sync:** Categories now sync perfectly to both the app drawer and the home screen workspace.
- **Multi-Language Intelligence:** Localized AI prompts and reasoning supporting 10+ global languages.
- **Settings UX Overhaul:** Reorganized settings into a logical, comfortable structure with a top-level AutoCat entry and integrated Discovery cards.
- **Performance & Stability:** Implemented non-blocking startup initialization and a font inflation cache to eliminate UI lag.
- **LLM Reliability:** Robust exponential backoff and "Smart Fallback" (Circuit Breaker) for AI providers.

### Current focus

This is our active development sprint. The goal is to polish the new AI features and gather community feedback on the Development 4 release.

- Optimizing database queries for very large app lists (>500 apps).
- Refining "Auto-Pilot" model selection based on real-world accuracy tracking.

### Up next

Once the Development 4 foundation is fully stable, our focus will shift to deeper launcher customization.

- Proper icon swipe gestures (AutoCat style).
- Folder "cover" mode.
- Fuzzy search sensitivity slider.

### The Android 16 Base

We have successfully rebased onto the latest Android 16 (AOSP) source. AutoCat is now tracking
the `16-dev` branch as its primary development target.

### Long term or blocked

Highly-requested features that are blocked by external dependencies or require significant research.

- **Widget Stacking:** A highly complex feature requiring deep architectural investigation.
- **On-Device ML:** Exploring local categorization models to reduce API dependency.
