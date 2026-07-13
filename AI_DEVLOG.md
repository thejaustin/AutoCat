# AutoCat — AI Session Devlog

AI-Powered App Categorization for Lawnchair 16 (16-dev branch).
Project at `/data/data/com.termux/files/home/AutoCat/`. Builds via GitHub Actions only.

Living document — update at start of every AI session.

---

## Open Backlog

### Build / CI
- [ ] **Release APK workflow** — `build_release_apk.yml` added (Jul 4); verify signing works end-to-end in CI
- [ ] **ktlint** — spotlessCheck passes after `0362ed5562`; confirm clean on next PR

### Features
- [ ] **PWA / Web shortcut categorization** — implemented in `6e610bf35d`; test on device with real shortcuts
- [ ] **Smart Dock + GenAI folder naming** — `223e635450`; test GenAI provider key flow
- [ ] **Zen Mode** — `8e430edc66`; test Dynamic Focus Categories toggling
- [ ] **Submodule `platform_frameworks_libs_systemui`** — bumped to latest 16-dev (`1da701515f`); watch for upstream breaking changes

### Planned (not started)
- [ ] **PWA settings toggle in onboarding setup screens** — Antigravity session noted this as next priority
- [ ] **LLM provider selection** — ensure all 4 providers (Google AI, OpenAI, Anthropic, etc.) surface correctly in settings

---

## Session History (newest first)

### 2026-07-12 — Claude Code (Fable 5) — dev-environment setup
- Added `CLAUDE.md` (build commands, branch/submodule/signing rules) and `scripts/dev/` (`ci-status.sh`, `ci-build.sh`, `fetch-apk.sh` — all GH-Actions-based, no local SDK). Uncommitted — review and commit.

### 2026-07-03 22:45 → 2026-07-04 00:25 — Antigravity CLI [session 52080eab]

**Commits:** `8e430edc66` through `1da701515f` (9 commits)

**Done:**
- **Zen Mode** (`8e430edc66`) — Dynamic Focus Categories feature implemented
- **Smart Dock + GenAI Folder Naming** (`223e635450`) — settings UI included
- **Seamless PWA and Web Shortcut Categorization** (`6e610bf35d`) — apps that are shortcuts/PWAs now categorized
- **Revert android.newDsl + groovy syntax** (`f57dc44d8a`) — caused baselineprofile plugin failure; reverted
- **Missing DisplayLibBackground for KSP** (`764bad237e`) — added
- **ktlint violations resolved** (`0362ed5562`) — spotlessCheck now passes
- **Release APK workflow** (`840ba290cc`) — `build_release_apk.yml` added; auto GitHub Releases
- **Signing step YAML fix** (`9c009d1cde`)
- **platform_frameworks_libs_systemui submodule** (`1da701515f`) — bumped to latest 16-dev

**Notes:**
- Session started with "what github repo other than obtainium+ and shizuku+ should we work on?" → AutoCat
- Also checked Sentry + GitHub issues; commented and closed resolved ones as "— Claude"
- Session hit no spending limits; build status checked but not confirmed green at end

### 2026-06-14 — Gemini CLI [session-2026-06-14T06-46-f90a015a, 67MB]

**Commits:** `cca50dec05`, `ea68ed2668`

**Done:**
- **Fix: correctly apply PR 100 features** (`cca50dec05`) — PR 100 features were not fully applied; corrected
- **Home screen folder sync + Smartspace bug** (`ea68ed2668`) — `WorkspaceItemSpaceFinder` now respects Smartspace row on first screen; `CategoryFolderSyncService` syncs to both app drawer and home screen
- **Proot environment fix** — Gemini diagnosed why `claude` CLI was failing from within PRoot (proot nesting detection); documented fix in `GEMINI.md`
- **Build note** — documented "always build with GitHub Actions" in project files

### 2026-06-13 — Gemini CLI

**Commits:** `fdfd95e627`, `46fd54cd10`

**Done:**
- **Merge PR 39** (`46fd54cd10`) — Fix addFrameworkJar build logic
- **Merge PR 100** (`fdfd95e627`) — base integration

### Pre-2026-06 — Various sessions

**Commits:** up to `5024fa1196` (May 6) — critical crash fixes and robustness improvements
