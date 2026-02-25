# GitHub Issues Creation Guide - Performance Optimizations

## Quick Start

Copy the content from each section below and create the corresponding GitHub issue. Issues are numbered #84-#89 for tracking purposes.

---

## Issue #84 - Main Epic Issue

**Title:** `[EPIC] Performance Optimization Tracker - v16.0 Development`

**Labels:** `epic`, `performance`, `optimization`, `v16-dev`

**Milestone:** `v16.0 Development`

**Assignee:** `@thejaustin`

**Content:** (See content in `.github/ISSUE_TEMPLATES/performance_optimization_tracker.md`)

**Important:** This is the parent issue. All other issues should link to this one.

---

## Issue #85 - App Drawer Performance

**Title:** `App Drawer Performance Improvements`

**Labels:** `performance`, `app-drawer`, `optimization`, `v16-dev`

**Parent Issue:** #84

**Commits to Reference:**
- `5c29373b03` - Add performance utilities and M3E theme helpers
- `11636f883a` - Integrate AppDrawerCache initialization

**Content:** Create using template from previous section

---

## Issue #86 - Search Optimization

**Title:** `Search Responsiveness Optimization`

**Labels:** `performance`, `search`, `optimization`, `v16-dev`

**Parent Issue:** #84

**Commits to Reference:**
- `5c29373b03` - Add performance utilities and M3E theme helpers

**Content:** Create using template from previous section

---

## Issue #87 - M3E Design

**Title:** `M3E Design System Integration`

**Labels:** `design`, `m3e`, `ui`, `v16-dev`

**Parent Issue:** #84

**Commits to Reference:**
- `5c29373b03` - Add performance utilities and M3E theme helpers

**Content:** Create using template from previous section

---

## Issue #88 - Baseline Profiles

**Title:** `Baseline Profile Generation & Testing`

**Labels:** `performance`, `baseline-profile`, `testing`, `v16-dev`

**Parent Issue:** #84

**Commits to Reference:**
- `968189e4ef` - Optimize app startup and baseline profile
- `28b07b0bae` - Enable PGO and improve startup benchmarks

**Content:** Create using template from previous section

---

## Issue #89 - Android Vitals

**Title:** `Android Vitals Monitoring Setup`

**Labels:** `monitoring`, `android-vitals`, `v16-dev`

**Parent Issue:** #84

**Commits to Reference:**
- `28b07b0bae` - Enable PGO and improve startup benchmarks

**Content:** Create using template from previous section

---

## Cross-Referencing Guide

### In Each Issue Description

Add this section to link issues together:

```markdown
## Related Issues

- Parent: #84 - Performance Optimization Tracker
- Sibling: #85 - App Drawer Performance Improvements
- Sibling: #86 - Search Responsiveness Optimization
- Sibling: #87 - M3E Design System Integration
- Sibling: #88 - Baseline Profile Generation & Testing
- Sibling: #89 - Android Vitals Monitoring Setup
```

### In Commit Messages (Already Done)

All commits already reference issues in their messages. When creating issues, update the commit references to use GitHub's auto-linking:

```markdown
### Commits

- [x] 968189e4ef - Optimize app startup and baseline profile (linked in release notes)
- [x] 28b07b0bae - Enable PGO and improve startup benchmarks
- [x] 5c29373b03 - Add performance utilities and M3E theme helpers
- [x] 11636f883a - Integrate AppDrawerCache initialization
```

### In Release Notes

The release notes file (`.github/RELEASE_NOTES/v16.0-dev-performance.md`) already contains all issue references. Once issues are created, update the placeholder issue numbers (#84-#89) with actual GitHub issue numbers.

---

## Future Enhancements Issues

Create these additional issues for future work:

### Issue #90: Room Database Migration

**Title:** `[FUTURE] Migrate to Room with Suspend DAOs`

**Labels:** `enhancement`, `database`, `performance`, `future`

**Parent Issue:** #84

**Description:**
Migrate database operations to Room with suspend functions for better performance and coroutines support.

**Tasks:**
- [ ] Add Room dependencies
- [ ] Create @Entity classes for apps, folders, settings
- [ ] Create @Dao interfaces with suspend functions
- [ ] Migrate existing queries
- [ ] Update repositories to use Room
- [ ] Add migration tests

---

### Issue #91: Paging 3 Implementation

**Title:** `[FUTURE] Implement Paging 3 for App Drawer`

**Labels:** `enhancement`, `app-drawer`, `performance`, `future`

**Parent Issue:** #84, #85

**Description:**
Implement Paging 3 library for efficient app list loading and scrolling.

**Tasks:**
- [ ] Add Paging 3 dependencies
- [ ] Create PagingSource for apps
- [ ] Update adapter to use PagingData
- [ ] Implement loading states
- [ ] Add refresh functionality
- [ ] Test with large app collections (1000+ apps)

---

### Issue #92: Complete M3E Settings UI

**Title:** `[FUTURE] Update All Settings Screens to M3E`

**Labels:** `enhancement`, `design`, `m3e`, `future`

**Parent Issue:** #84, #87

**Description:**
Update all 28 preference screens to use M3E design system consistently.

**Tasks:**
- [ ] GeneralPreferences.kt
- [ ] HomeScreenPreferences.kt
- [ ] AppDrawerPreferences.kt
- [ ] DockPreferences.kt
- [ ] FolderPreferences.kt
- [ ] ExperimentalFeaturesPreferences.kt
- [ ] ... (22 more files)

---

### Issue #93: Icon Loading with Coil

**Title:** `[FUTURE] Implement Coil for Async Icon Loading`

**Labels:** `enhancement`, `icons`, `performance`, `future`

**Parent Issue:** #84, #85

**Description:**
Replace synchronous icon loading with Coil for async loading and caching.

**Tasks:**
- [ ] Add Coil dependencies
- [ ] Create icon loading requests
- [ ] Implement memory and disk caching
- [ ] Add placeholder icons
- [ ] Handle loading states
- [ ] Test scroll performance

---

### Issue #94: Search FTS Implementation

**Title:** `[FUTURE] Implement Room FTS for Search`

**Labels:** `enhancement`, `search`, `performance`, `future`

**Parent Issue:** #84, #86

**Description:**
Implement Full Text Search (FTS) in Room for faster search queries.

**Tasks:**
- [ ] Add FTS table for apps
- [ ] Create FTS DAO
- [ ] Implement search with ranking
- [ ] Add fuzzy matching
- [ ] Test search performance
- [ ] Update debouncer integration

---

### Issue #95: In-App Performance Monitoring

**Title:** `[FUTURE] Add In-App Performance Monitoring`

**Labels:** `enhancement`, `monitoring`, `performance`, `future`

**Parent Issue:** #84, #89

**Description:**
Add in-app performance monitoring dashboard for debugging.

**Tasks:**
- [ ] Create performance metrics collector
- [ ] Build debug overlay UI
- [ ] Add cache statistics display
- [ ] Implement frame timing monitor
- [ ] Add export functionality
- [ ] Document usage for testers

---

## Post-Creation Checklist

After creating all issues:

1. **Link Issues Together**
   - Update parent/child relationships
   - Add cross-references in descriptions
   - Link to epic issue #84

2. **Update Release Notes**
   - Replace placeholder issue numbers
   - Add actual GitHub issue URLs
   - Verify all links work

3. **Update Commit References**
   - GitHub auto-links short commit hashes
   - Verify commit links in issues work
   - Add commit links to release notes

4. **Add to Project Board**
   - Create "v16.0 Performance" project
   - Add all issues to board
   - Organize by status (Todo, In Progress, Done)

5. **Notify Team**
   - @mention team members in issues
   - Share issue links in team chat
   - Schedule review meetings

---

## GitHub CLI Commands (Optional)

If you have GitHub CLI installed, you can create issues faster:

```bash
# Create epic issue
gh issue create \
  --title "[EPIC] Performance Optimization Tracker - v16.0 Development" \
  --body-file .github/ISSUE_TEMPLATES/performance_optimization_tracker.md \
  --label "epic,performance,optimization,v16-dev" \
  --milestone "v16.0 Development" \
  --assignee "thejaustin"

# Create child issues similarly
gh issue create --title "App Drawer Performance Improvements" --body-file ...
```

---

## Verification

After all issues are created:

- [ ] Issue #84 exists and is marked as epic
- [ ] Issues #85-#89 exist and link to #84
- [ ] All commits are referenced in at least one issue
- [ ] Release notes reference all issues
- [ ] Project board created and populated
- [ ] Team members notified

---

**Created:** 2026-02-24
**For:** AutoCat v16.0 Development Performance Optimizations
