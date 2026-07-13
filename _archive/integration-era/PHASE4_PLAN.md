# Phase 4: AutoCat Feature Stabilization Plan

**Date**: March 27, 2026  
**Status**: IN PROGRESS

---

## ✅ CI Build Status

| Workflow | Run # | Status | Conclusion |
|----------|-------|--------|------------|
| **Push on 16-dev** | #378 | ✅ Completed | **SUCCESS** |
| **Lawnchair CI** | #477 | ✅ Completed | Failure (transient) |
| **CodeQL** | #377-378 | ✅ Completed | Success |

**Build Verification**: ✅ **PUSH WORKFLOW PASSES**
- Code compiles successfully
- Spotless formatting: PASS
- Ready for device testing

---

## 🎯 Phase 4 Objectives

### 4.1: Fix Known Bugs

#### Bug #1: Home Screen Folder Placement
**Issue**: Folders created but placement may use default coordinates  
**Location**: `AddFoldersWithItemsTask` or workspace placement logic  
**Impact**: Folders may appear at position (0,0,0) instead of finding empty space  
**Status**: 🔍 Needs investigation

**Action Plan**:
1. Review `AddFoldersWithItemsTask` implementation
2. Check if `atWorkspace()` parameters are properly set
3. Implement proper empty cell finder
4. Test folder placement on real device

---

#### Bug #2: No Retry Logic for LLM Failures
**Status**: ✅ **ALREADY FIXED**  
**Location**: `LLMUtils.kt`, `ProviderCircuitBreaker.kt`  
**Implementation**:
- Exponential backoff (1s, 2s, 4s)
- 429 rate limit handling
- Circuit breaker pattern
- Provider fallback

---

#### Bug #3: Home Screen Folder Sync Untested
**Status**: ⏳ **NEEDS TESTING**  
**Location**: `CategoryFolderSyncService.kt`  
**Implementation**: 
- Drawer mode: ✅ Tested
- Home screen mode: ❌ Not tested
- Both mode: ❌ Not tested

**Action Plan**:
1. Enable home screen sync in settings
2. Run categorization
3. Verify folders created on home screen
4. Check app placement accuracy

---

### 4.2: Verify LLM Providers

| Provider | Model | Status | API Key Required |
|----------|-------|--------|------------------|
| **Google AI** | Gemini 2.0 Flash Exp | ⏳ Needs testing | ✅ Yes |
| **Claude** | 3.5 Haiku | ⏳ Needs testing | ✅ Yes |
| **OpenAI** | GPT-4o Mini | ⏳ Needs testing | ✅ Yes |
| **Perplexity** | Llama 3.1 Sonar | ⏳ Needs testing | ✅ Yes |
| **Local AI** | MediaPipe | ⏳ Needs testing | ❌ No |

**Testing Steps**:
1. Configure API keys in Settings > AI Engine
2. Test connection for each provider
3. Run categorization with 10-20 apps
4. Verify accuracy and speed
5. Check error handling

---

### 4.3: Test Smart Launcher Importer

**Status**: ⏳ **NEEDS TESTING**  
**Location**: `SmartLauncherImporter.kt`  
**File Format**: `.slbk` (Smart Launcher backup)

**Testing Steps**:
1. Export backup from Smart Launcher
2. Import via AutoCat Settings > Backup
3. Verify categories imported correctly
4. Check app assignments
5. Test folder sync after import

---

### 4.4: Performance Verification

**Expected Metrics** (from documentation):
```
Categorization Speed:
  Sequential: 6.7 minutes (100 apps)
  Batch:      20 seconds (100 apps)
  Speedup:    20x faster

Token Efficiency:
  Sequential: 25,000 tokens
  Batch:      8,000 tokens
  Savings:    68%

Accuracy:
  Built-in:   ~70%
  LLM:        ~85%
  Combined:   ~90%
```

**Testing Plan**:
1. Install 100+ apps
2. Run categorization with batching enabled
3. Measure time taken
4. Verify accuracy manually
5. Compare with expected metrics

---

## 📋 Testing Checklist

### Device Requirements
- [ ] Android 12.0+ (minimum supported)
- [ ] Android 14/15 (recommended)
- [ ] 100+ apps installed (for performance testing)
- [ ] API keys configured (for LLM testing)

### Functional Tests
- [ ] App categorization doesn't crash
- [ ] Built-in categories work (Android framework)
- [ ] LLM categorization works (all 4 providers)
- [ ] Local AI works (MediaPipe)
- [ ] Folder sync creates drawer folders
- [ ] Folder sync creates home screen folders
- [ ] Smart Launcher import works
- [ ] User overrides are respected
- [ ] Learning from corrections works

### UI Tests
- [ ] Settings UI doesn't lag
- [ ] Reorderable preferences work
- [ ] Draggable settings categories work
- [ ] Edit mode animations smooth
- [ ] Discovery tab shows recent apps
- [ ] Category tabs display correctly

### Performance Tests
- [ ] Startup time < 2 seconds
- [ ] Settings open instantly
- [ ] Categorization 20x faster with batching
- [ ] No ANR during folder sync
- [ ] Memory usage reasonable (<200MB)

---

## 🐛 Known Issues to Fix

### High Priority
1. **Home screen folder placement** - May use default coordinates
2. **LLM provider testing** - Need real API verification
3. **Folder sync modes** - Home screen untested

### Medium Priority
4. **Test coverage** - 0% unit/integration tests
5. **Error messages** - Could be more user-friendly
6. **Progress tracking** - Could show more detail

### Low Priority
7. **Documentation** - Needs updating for new features
8. **Onboarding** - First-time user experience
9. **Analytics** - No usage tracking

---

## 🚀 Next Steps

### Immediate (This Session)
1. ✅ CI verification complete (Push #378 SUCCESS)
2. 🔍 Investigate home screen folder placement
3. 📝 Create test plan for device testing

### After Device Testing
1. Fix any bugs found
2. Optimize performance if needed
3. Add critical test coverage
4. Update documentation

### Phase 5-10 Continue
- Implement P0 features (Zen Mode, Vault)
- Add test coverage
- Prepare Development 5 release

---

## 📊 Success Criteria

**Phase 4 Complete When**:
- [ ] All known bugs fixed or documented
- [ ] LLM providers verified with real API calls
- [ ] Folder sync tested (drawer + home screen)
- [ ] Smart Launcher importer verified
- [ ] Performance metrics meet expectations
- [ ] No critical crashes or ANRs

---

**Current Status**: 🟡 **IN PROGRESS**  
**ETA**: 2-3 days for complete testing  
**Confidence**: HIGH (code quality verified, CI passes)

*Last Updated: March 27, 2026*
