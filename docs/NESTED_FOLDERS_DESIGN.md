# Nested Folders Design Document

**Issue**: #22
**Feature**: Support for nested folders (folders within folders) up to 2-3 levels deep
**Status**: Design Phase
**Last Updated**: 2025-12-27

## Overview

Add support for nested folders to allow deeper organization hierarchy. Users can create folders inside folders (e.g., Gaming → Strategy → Turn-based).

## Current State

- Apps can be organized in folders
- Folders exist within tabs
- All folders are flat (single level only)
- No parent-child folder relationships
- Tab names support hierarchy (`"Games > Puzzle"`) but folders don't reflect this

## Proposed Architecture

### 1. Database Schema Changes

#### 1.1 FolderInfoEntity (Migration 4 → 5)

Add parent folder reference to support hierarchy:

```kotlin
@Entity(
    tableName = "Folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.SET_NULL,  // When parent deleted, make child top-level
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["parentFolderId"])],
)
data class FolderInfoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    @ColumnInfo(defaultValue = "NULL") val parentFolderId: Int? = null,  // NEW FIELD
    val hide: Boolean = false,
    val rank: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL") val icon: String? = null,
    @ColumnInfo(defaultValue = "0") val coverMode: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val coverAppComponent: String? = null,
)
```

**Migration SQL**:
```sql
ALTER TABLE Folders ADD COLUMN parentFolderId INTEGER DEFAULT NULL
    REFERENCES Folders(id) ON DELETE SET NULL ON UPDATE CASCADE;
CREATE INDEX IF NOT EXISTS index_Folders_parentFolderId ON Folders(parentFolderId);
```

#### 1.2 FolderItemEntity Enhancement

Support both apps and nested folders as items:

```kotlin
@Entity(
    tableName = "FolderItems",
    foreignKeys = [
        ForeignKey(
            entity = FolderInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(  // NEW: Reference nested folder
            entity = FolderInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["nestedFolderId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["nestedFolderId"]),  // NEW INDEX
    ],
)
data class FolderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int,  // Parent folder containing this item
    val rank: Int = 0,
    @ColumnInfo(name = "item_component_key") val componentKey: String? = null,  // App
    @ColumnInfo(name = "nested_folder_id", defaultValue = "NULL") val nestedFolderId: Int? = null,  // NEW: Nested folder
    val timestamp: Long = System.currentTimeMillis(),
)
```

**Constraint**: Either `componentKey` OR `nestedFolderId` must be non-null (not both)

**Migration SQL**:
```sql
ALTER TABLE FolderItems ADD COLUMN nested_folder_id INTEGER DEFAULT NULL
    REFERENCES Folders(id) ON DELETE CASCADE ON UPDATE CASCADE;
CREATE INDEX IF NOT EXISTS index_FolderItems_nestedFolderId ON FolderItems(nested_folder_id);
```

### 2. Business Logic Changes

#### 2.1 FolderDao Updates

Add queries for nested folder operations:

```kotlin
@Dao
interface FolderDao {
    // Existing methods...

    // NEW: Get child folders of a parent
    @Query("SELECT * FROM Folders WHERE parentFolderId = :parentId ORDER BY rank")
    suspend fun getChildFolders(parentId: Int): List<FolderInfoEntity>

    // NEW: Get folder depth (for max depth validation)
    @Query("""
        WITH RECURSIVE folder_hierarchy AS (
            SELECT id, parentFolderId, 0 as depth
            FROM Folders
            WHERE id = :folderId
            UNION ALL
            SELECT f.id, f.parentFolderId, fh.depth + 1
            FROM Folders f
            INNER JOIN folder_hierarchy fh ON f.id = fh.parentFolderId
        )
        SELECT MAX(depth) FROM folder_hierarchy
    """)
    suspend fun getFolderDepth(folderId: Int): Int

    // NEW: Get all ancestor folder IDs (for circular dependency check)
    @Query("""
        WITH RECURSIVE folder_ancestors AS (
            SELECT parentFolderId as id
            FROM Folders
            WHERE id = :folderId AND parentFolderId IS NOT NULL
            UNION ALL
            SELECT f.parentFolderId as id
            FROM Folders f
            INNER JOIN folder_ancestors fa ON f.id = fa.id
            WHERE f.parentFolderId IS NOT NULL
        )
        SELECT id FROM folder_ancestors
    """)
    suspend fun getAncestorFolderIds(folderId: Int): List<Int>

    // NEW: Update parent folder
    @Query("UPDATE Folders SET parentFolderId = :parentId WHERE id = :folderId")
    suspend fun updateParentFolder(folderId: Int, parentId: Int?)
}
```

#### 2.2 FolderService Updates

Add validation and nesting logic:

```kotlin
class FolderService(context: Context) {
    companion object {
        const val MAX_NESTING_DEPTH = 3  // Recommended limit
    }

    // NEW: Move folder into another folder
    suspend fun moveFolderIntoFolder(
        folderId: Int,
        targetParentId: Int?,
    ): Result {
        // Validation 1: Prevent circular dependency
        if (targetParentId != null) {
            val ancestors = folderDao.getAncestorFolderIds(folderId)
            if (targetParentId in ancestors || targetParentId == folderId) {
                return Result.Error("Cannot create circular folder hierarchy")
            }
        }

        // Validation 2: Check max depth
        if (targetParentId != null) {
            val parentDepth = folderDao.getFolderDepth(targetParentId)
            if (parentDepth >= MAX_NESTING_DEPTH - 1) {
                return Result.Error("Maximum nesting depth ($MAX_NESTING_DEPTH) exceeded")
            }
        }

        // Update parent folder
        folderDao.updateParentFolder(folderId, targetParentId)

        return Result.Success
    }

    // NEW: Get folder breadcrumb path
    suspend fun getFolderBreadcrumb(folderId: Int): List<FolderInfo> {
        val breadcrumb = mutableListOf<FolderInfo>()
        var currentId: Int? = folderId

        while (currentId != null) {
            val folder = folderDao.getFolderById(currentId) ?: break
            breadcrumb.add(0, folder.toFolderInfo())
            currentId = folder.parentFolderId
        }

        return breadcrumb
    }
}
```

### 3. UI/UX Design

#### 3.1 Visual Hierarchy

**Indentation for nested folders**:
```
📁 Gaming
  📁 Strategy
    📁 Turn-based
      🎮 Civilization VI
      🎮 XCOM 2
    📁 Real-time
      🎮 StarCraft
  📁 Action
    🎮 Doom
```

#### 3.2 Breadcrumb Navigation

When inside a nested folder, show breadcrumb path:

```
Home > Gaming > Strategy > Turn-based
```

#### 3.3 Drag-and-Drop

- Drag folder onto another folder to nest it
- Drop folder on breadcrumb to move it to that level
- Long-press to drag folders

#### 3.4 Maximum Depth Indicator

Show warning when approaching max depth:
```
⚠️ Maximum folder nesting reached (3 levels)
```

### 4. CategoryFolderSyncService Integration

Support auto-creating nested folders from tab hierarchies:

```kotlin
// Current: "Games > Puzzle" → Creates flat folder "Puzzle"
// New: "Games > Puzzle" → Creates "Games" folder with "Puzzle" inside

private suspend fun createNestedFoldersFromTabPath(tabName: String): FolderInfo {
    val parts = tabName.split(" > ")
    var parentFolderId: Int? = null

    for (i in parts.indices) {
        val folderName = parts[i]

        // Check if folder exists at this level
        val existing = folderDao.getFolderByTitleAndParent(folderName, parentFolderId)

        if (existing != null) {
            parentFolderId = existing.id
        } else {
            // Create new folder with parent
            val newFolder = FolderInfoEntity(
                title = folderName,
                parentFolderId = parentFolderId,
            )
            parentFolderId = folderDao.insertFolder(newFolder).toInt()
        }
    }

    return folderDao.getFolderById(parentFolderId!!)!!.toFolderInfo()
}
```

### 5. Data Integrity Constraints

#### 5.1 Circular Dependency Prevention

Before setting parent folder, check:
```kotlin
if (targetParent.isDescendantOf(movingFolder)) {
    throw CircularDependencyException()
}
```

#### 5.2 Depth Limit Enforcement

```kotlin
fun validateDepth(folderId: Int): Boolean {
    var depth = 0
    var currentId: Int? = folderId

    while (currentId != null && depth <= MAX_NESTING_DEPTH) {
        currentId = getParentFolderId(currentId)
        depth++
    }

    return depth <= MAX_NESTING_DEPTH
}
```

#### 5.3 Orphan Handling

When parent folder deleted:
- Option 1: Delete all nested folders (CASCADE)
- Option 2: Move nested folders to top level (SET NULL) ← **RECOMMENDED**

### 6. Performance Considerations

#### 6.1 Database Indexes

```sql
CREATE INDEX idx_folders_parent ON Folders(parentFolderId);
CREATE INDEX idx_folder_items_nested ON FolderItems(nested_folder_id);
```

#### 6.2 Query Optimization

- Use recursive CTEs for ancestor/descendant queries
- Cache breadcrumb paths for active folders
- Lazy-load nested folder contents

### 7. Migration Strategy

#### Phase 1: Database Schema (v4 → v5)
1. Add `parentFolderId` column to `Folders`
2. Add `nested_folder_id` column to `FolderItems`
3. Create foreign keys and indexes
4. Set all existing folders to `parentFolderId = NULL`

#### Phase 2: Service Layer
1. Implement `moveFolderIntoFolder()`
2. Add validation logic (circular deps, max depth)
3. Update `CategoryFolderSyncService` for tab hierarchies
4. Add breadcrumb methods

#### Phase 3: UI
1. Add drag-and-drop for folder nesting
2. Implement breadcrumb navigation
3. Show folder hierarchy in settings
4. Add visual indicators for nested folders

### 8. Testing Requirements

#### Unit Tests
- Circular dependency detection
- Max depth validation
- Breadcrumb path generation
- Ancestor/descendant queries

#### Integration Tests
- Create nested folder structure
- Move folders between levels
- Delete parent folder (orphan handling)
- Sync from hierarchical tab names

#### UI Tests
- Drag-and-drop folder nesting
- Breadcrumb navigation
- Visual hierarchy display

### 9. Known Limitations

1. **Launcher3 Constraint**: Standard Launcher3 folders don't support nesting. This feature only works with:
   - App drawer folders (caddy system) ✅
   - Custom workspace folders (requires modification) ⚠️

2. **Performance**: Deep nesting may slow down:
   - Folder open animations
   - Breadcrumb queries
   - Sync operations

3. **UX Complexity**: Users may get lost in deep hierarchies
   - Solution: Enforce MAX_NESTING_DEPTH = 3

### 10. Future Enhancements

- **Smart Auto-Nesting**: LLM suggests optimal folder hierarchy
- **Folder Templates**: Pre-built nested structures (e.g., "Work Apps")
- **Collapse/Expand**: Show/hide nested folder contents
- **Search in Hierarchy**: Find apps across all nested levels

## Implementation Checklist

- [ ] Database migration (v4 → v5)
- [ ] Update `FolderInfoEntity` with `parentFolderId`
- [ ] Update `FolderItemEntity` with `nestedFolderId`
- [ ] Add DAO methods for hierarchy queries
- [ ] Implement validation logic in `FolderService`
- [ ] Update `CategoryFolderSyncService` for auto-nesting
- [ ] Design UI for drag-and-drop nesting
- [ ] Implement breadcrumb navigation
- [ ] Add unit tests for validation
- [ ] Add integration tests for sync
- [ ] Update documentation

## References

- Issue #22: Feature: Nested folders (folderception)
- `FolderEntity.kt`: Database model
- `FolderService.kt`: Business logic
- `CategoryFolderSyncService.kt`: Auto-sync from tabs
- Launcher3 `FolderInfo.java`: Domain model

---

**Author**: Claude Code
**Review Status**: Awaiting user approval
**Next Steps**: Review design, implement Phase 1 (database schema)
