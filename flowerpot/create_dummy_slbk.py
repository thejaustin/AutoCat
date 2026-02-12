import sqlite3
import zipfile
import os
import shutil

DB_NAME = "ginlemon.flower.db"
ZIP_NAME = "dummy_backup.slbk"
ICONS_DIR = "icons"

def create_db():
    if os.path.exists(DB_NAME):
        os.remove(DB_NAME)
    
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    # Create Category Table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS Category (
            id TEXT PRIMARY KEY,
            customLabel TEXT,
            icon TEXT,
            sortOrder INTEGER
        )
    ''')

    # Create DrawerItem Table
    # Note: Smart Launcher schema might vary, but based on the importer:
    # id, label, parentId, icon, packageName, intent, categoryId, etc.
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS DrawerItem (
            id INTEGER PRIMARY KEY,
            label TEXT,
            parentId INTEGER,
            icon TEXT,
            packageName TEXT,
            intent TEXT,
            categoryId TEXT,
            iconPackage TEXT,
            iconName TEXT,
            cellX INTEGER,
            cellY INTEGER,
            screen INTEGER
        )
    ''')
    
    # Create Workspace Table (Home items)
    # Using 'HomeItem' as a guess, importer checks for 'Home', 'Desktop', 'Bubble', 'Item'
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS HomeItem (
            id INTEGER PRIMARY KEY,
            packageName TEXT,
            intent TEXT,
            cellX INTEGER,
            cellY INTEGER,
            screen INTEGER
        )
    ''')

    # --- Insert Data ---

    # Categories
    categories = [
        ("games", "My Games"),
        ("social", "Social Apps"),
        ("work", "Work Stuff"),
        ("tools", "Utilities")
    ]
    
    for i, (cat_id, label) in enumerate(categories):
        cursor.execute("INSERT INTO Category (id, customLabel, sortOrder) VALUES (?, ?, ?)", (cat_id, label, i))

    # Folders (parentId=0 means root, but folders usually have parentId=0 if they are top level in category? 
    # Actually, importer logic: 
    # "if (parentId != 0 && folderMap.containsKey(parentId)) ... targetSubCategory = folder.label"
    # So folders are items with parentId=0 (or whatever), and apps have parentId pointing to folder.
    
    # Let's create a folder "Chat" in "social" category context (though SL structure is hierarchical)
    # The importer maps:
    # - App's categoryId -> Tab Name
    # - App's parentId -> Folder Name (if parentId exists in folderMap)
    
    # So we need entries in DrawerItem that act as folders.
    # folderMap is populated by: SELECT id, label FROM DrawerItem WHERE packageName IS NULL
    
    # 1. Create a Folder "Favorites" (id=100)
    cursor.execute("INSERT INTO DrawerItem (id, label, parentId) VALUES (100, 'Favorites', 0)")
    
    # 2. Create a Folder "Messengers" (id=101)
    cursor.execute("INSERT INTO DrawerItem (id, label, parentId) VALUES (101, 'Messengers', 0)")

    # Apps
    apps = [
        # (id, label, parentId, pkg, categoryId)
        (1, "WhatsApp", 101, "com.whatsapp", "social"),       # Inside 'Messengers' folder, 'Social Apps' tab
        (2, "Telegram", 101, "org.telegram.messenger", "social"), # Inside 'Messengers' folder, 'Social Apps' tab
        (3, "Gmail", 0, "com.google.android.gm", "work"),    # No folder, 'Work Stuff' tab
        (4, "Candy Crush", 100, "com.king.candycrushsaga", "games"), # Inside 'Favorites', 'My Games' tab
        (5, "Calculator", 0, "com.android.calculator2", "tools") # No folder, 'Utilities' tab
    ]

    for app in apps:
        cursor.execute('''
            INSERT INTO DrawerItem (id, label, parentId, packageName, categoryId) 
            VALUES (?, ?, ?, ?, ?)
        ''', app)
        
    # Workspace Items
    home_apps = [
        (1, "com.whatsapp", 0, 0, 0),
        (2, "com.google.android.gm", 1, 0, 0),
        (3, "org.telegram.messenger", 0, 1, 0)
    ]
    
    for item in home_apps:
        cursor.execute('''
            INSERT INTO HomeItem (id, packageName, cellX, cellY, screen)
            VALUES (?, ?, ?, ?, ?)
        ''', item)

    conn.commit()
    conn.close()
    print(f"Created {DB_NAME}")

def create_zip():
    # Create icons dir just in case
    if not os.path.exists(ICONS_DIR):
        os.makedirs(ICONS_DIR)
        
    with zipfile.ZipFile(ZIP_NAME, 'w', zipfile.ZIP_DEFLATED) as zipf:
        zipf.write(DB_NAME)
        # Add a dummy icon
        # zipf.write(os.path.join(ICONS_DIR, "dummy.png")) 
    
    print(f"Created {ZIP_NAME}")
    
    # Cleanup
    if os.path.exists(DB_NAME):
        os.remove(DB_NAME)
    if os.path.exists(ICONS_DIR):
        shutil.rmtree(ICONS_DIR)

if __name__ == "__main__":
    create_db()
    create_zip()
