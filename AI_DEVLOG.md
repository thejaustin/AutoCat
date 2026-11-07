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

## Backlog: drawer Gaussian blur option (2026-11-07, user-suggested)

Drawer already has `drawerOpacity` (FloatPref, AppDrawerPreferences slider) feeding the scrim. The app also already has an OpenGL Gaussian blur (HokoBlur, `SCHEME_OPENGL`) used for the home wallpaper via `enableWallpaperBlur`/`wallpaperBlur`/`wallpaperBlurFactorThreshold` in `LauncherRootView.java`. Idea: offer a drawer "Gaussian blur" option that reuses HokoBlur to blur the drawer backdrop as a fallback when the system cross-window blur is unavailable (battery saver, unsupported devices) — that's the ghosting case the scrim fix currently patches with opacity only. Lives alongside the drawer opacity setting.

## Backlog: settings icon design (2026-11-07, user-reported)

Settings icons are inconsistent. Survey of `lawnchair/src/app/lawnchair/ui/preferences`:
- 153 `Icons.Rounded.*` (the de-facto house style), 10 `Icons.Filled.*` (mostly drag handles / arrows in control components), 4 `Icons.Outlined.*` in visible destination screens (`Outlined.Backup`, `Outlined.Science`, `Outlined.Memory`, one in IconPackPreferences), 5 `AutoMirrored.Rounded.*`.
- A few screens use custom drawable vectors (`ic_about`, `ic_download`, search-provider / icon-pack brand icons) as leading icons, which read as "two-tone" next to the flat Material Rounded icons.
- No shape-morph system in code; "morph" refers to the visual mix of Material styles + custom drawables.
Plan: normalize visible `Outlined` destination icons to `Rounded`; decide a single house style (all Material Rounded vs a themed/duotone set) before a broader pass. Direction is the user's call.

## Research: recents and system integration without root (2026-11-07)

Deeper findings (disabling One UI Home / temp root), verified on-device:
- **SysUI binds QUICKSTEP_SERVICE only to the package in `config_recentsComponentName`.** Confirmed: `LauncherProxyService ... mRecentsComponentName=com.sec.android.app.launcher/...RecentsActivity`, `mQuickStepIntent pkg=com.sec.android.app.launcher`. Disabling One UI Home leaves the config pointing at a missing component → system recents breaks entirely; it does NOT transfer to another launcher.
- **The recents perms are `signature|privileged`, grantable only at package-scan time to a priv-app partition member with a privapp-permissions whitelist XML** (One UI Home has `/system/etc/permissions/privapp-permissions-com.sec.android.app.launcher.xml`). `pm grant` (even as shell/root) refuses: "START_TASKS_FROM_RECENTS ... is not a changeable permission type". AutoCat declares its own `TouchInteractionService` + requests the perms, but holds none (not priv-app, not whitelisted).
- **This is how root-based QuickSwitch works**: systemless-mount the launcher into `/system/priv-app` + add a privapp-permissions XML + repoint `config_recentsComponentName` via RRO. All three need PERSISTENT root (Magisk/KernelSU module) applied before package scan.
- **Temp root (per-boot, locked bootloader)** can't achieve it: privapp perms are parsed at boot by system_server before a post-boot exploit gains root, so an injected priv-app is scanned too late; and nothing persists across reboot. `cmd overlay fabricate` as root could repoint recents at runtime, but without the perms the provider still can't function.
- **Device reality (SM-S948U, US AT&T, Snapdragon):** `ro.boot.flash.locked=1`, `sys.oem_unlock_allowed` empty → bootloader can't be officially unlocked; patching boot.img on a locked BL bricks (EDL recovery only). So persistent root isn't available here, and temp-root is non-persistent, Knox-tripping (permanent), and firmware-version-specific.
- **Conclusion:** the Shizuku task switcher is the ceiling on this device. Disabling One UI Home is net-negative (kills system recents without transferring it). Home role is currently Smart Launcher (ginlemon.flowerfree), not One UI Home.

## Research: recents and system integration without root — initial (2026-11-07)

Tested on SM-S948U (AT&T, One UI 9, Android 17 / SDK 37, bootloader locked, verified boot green).

- **System recents can't be taken over without root.** SystemUI only binds `TouchInteractionService` for the app named in `config_recentsComponentName` (here `com.sec.android.app.launcher/com.android.quickstep.RecentsActivity`). Changing it needs an overlay:
  - Fabricated overlay from Shizuku/shell: AOSP `OverlayManagerService` throws "Non-root shell cannot fabricate overlays" (blocked since 12L), and `cmd overlay fabricate` requires root.
  - Installed overlay APK: `aapt2 dump overlayable framework-res.apk` shows 0 overlayable entries, so targeting `android` needs a system/signature policy we can't meet.
  - None of the 33 overlays on the device changes the recents component.
- **Quickstep permissions are `signature|recents`** (START_TASKS_FROM_RECENTS, MANAGE_ACTIVITY_TASKS, STATUS_BAR_SERVICE, READ_FRAME_BUFFER...), granted only to the configured recents package.
- **Pretending to be One UI Home is blocked.** Its package lives in `/system/priv-app/TouchWizHome_2017`; installing another package under that name must match its signature even if it is uninstalled for the user. Disabling One UI Home breaks the bound gesture/recents provider; don't experiment with it on a locked phone.
- **Shell (Shizuku) holds** READ_FRAME_BUFFER, MANAGE_ACTIVITY_TASKS, START_TASKS_FROM_RECENTS, REMOVE_TASKS, REAL_GET_TASKS, REORDER_TASKS, CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS, INJECT_EVENTS, MANAGE_ROLE_HOLDERS, PACKAGE_USAGE_STATS, WRITE_SECURE_SETTINGS. Not MONITOR_INPUT, so system gestures can't be intercepted.
- **Feasible:** an in-launcher task switcher backed by a Shizuku user service (recent tasks, thumbnails, open, close). Android 17 framework has `TaskSnapshotManager`/`ITaskSnapshotManager`, `wrapToBitmap`, `getRecentTasks`, `startActivityFromRecents`, `removeTask`, `registerTaskStackListener`. Thumbnails must cross binder as `HardwareBuffer`, not `Bitmap`. Swiping up inside other apps still opens One UI Home's recents; One Hand Operation+ (installed) may be able to launch the switcher.
- **Rooted Android 17:** `QUICKSTEP_MAX_SDK=36` disables recents even with root. Upstream's Android 17 fix (LawnchairLauncher/lawnchair#7350) was closed unmerged; denson9874/ExpressiveLauncher#37 documents the Android 17 API changes (removed desktop flags, `IRecentsAnimationRunner`, snapshots, input consumer, `registerRemote`).
- **Local AI:** MediaPipe LLM Inference is maintenance-only; LiteRT-LM (`litertlm-android`, arm64-v8a + x86_64 only, Apache-2.0) replaces it. Ungated Apache models: Qwen3 0.6B (585 MB), Qwen2.5 1.5B q8 (1.5 GB), Gemma 4 E2B (2.4 GB). The categorizer never called the local provider before this.

---

## Session History (newest first)

### 2026-11-03 — Claude Code (Opus 5.5) — upstream sync + model refresh
Branch `feat/upstream-sync-2026-11`. Commits: merge `d665f5b810`, `462e8e1226` (models), `275e31128c` (LLM budgets).

- **Merged upstream Lawnchair 16-dev** (422 commits, merge base `00bb0d82`, 2026-04-09). Local clone is shallow and on FUSE (git can't rewrite `.git/shallow` there), so the merge base was supplied with a local-only graft: `git replace --graft 5024fa11 00bb0d82` (+ `2499c19b` grafted parentless) with `.git/shallow` moved aside during the merge. Replace refs are never pushed; keep them until the push lands, then `git replace -d`.
- **Duplicate classes consolidated.** The fork had live `AutoCat*` classes plus stale `Lawnchair*` copies that upstream keeps editing. Removed `LawnchairApp`/`LawnchairLauncher`/`LawnchairShortcut`/`LawnchairSearchAdapterProvider` and the unwired `AutoCatAlphabeticalAppsList`; ported upstream changes into the AutoCat copies. `LawnchairAlphabeticalAppsList` and both widget holders are still live — watch them on future syncs.
- `AutoCatApp` now extends `LauncherApplication` (upstream #6629). Quickstep code used to read `LawnchairApp.instance`, which was never initialised.
- Settings screens follow upstream's SegmentedListItem layout; AutoCat entries and leading icons were re-added (`~/autocat-tools/icons.py` re-applies icons by label).
- FolderService follows upstream's `FolderEntry`; AutoCat sync/auto-sort use `getAllFolders()` + the `AppInfo` overload of `updateFolderWithItems()` (folderInfoId 0 creates).
- Quickstep range is now API 35–36 (upstream choice); recents integration is gone on Android 10–14.
- LLM catalog: Claude Haiku/Sonnet/Opus 5.5, Gemini 3.5 Flash-Lite/3.8 Flash/4 Argon, GPT-6 Luna/6.1 Sol/6 Astra; saved retired IDs migrate automatically.

**Open:**
- [ ] CI was red for 100+ runs before this merge; expect several fix rounds on the feature branch.
- [ ] `FolderInfo` has no `coverMode`/`coverApp`/`icon`; the folder-cover feature is only half-implemented (DB columns + DAO exist).
- [ ] After green: bump TFLite (check LiteRT rename) and MediaPipe GenAI one CI round at a time.
- [ ] Verify Perplexity model ids (`sonar`, `sonar-pro`, `sonar-reasoning`) against current docs.


### 2026-07-19 — Antigravity CLI — AutoCat fixes & enhancements
- **Fixed ClassCastException in FloatingHeaderView (Issue #84)** — Updated `mTabLayout` and `getTabLayout()` in `FloatingHeaderView.java` to type `View` to safely support Jetpack Compose-based `AppTabsHeaderView`.
- **Implemented Battery Saver & Wi-Fi logic for LLM categorization (Issue #79)** — Created `llmMinBatteryLevel` preference, `LLMUtils.getBatteryLevel()`, and `LLMCategorizer.checkConstraints()` to enforce Wi-Fi, charging, and battery level thresholds before calling LLMs. Added battery threshold slider to `LLMSettingsPreferences.kt`.
- **Added PWA / Web Shortcut categorization toggle in onboarding** — Added PWA toggle control to `SmartFeaturesStep` in `SmartCategoriesOnboardingPreferences.kt`.
- **Spotless check** — Ran `./gradlew spotlessApply` and confirmed `./gradlew spotlessCheck` passes.

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
