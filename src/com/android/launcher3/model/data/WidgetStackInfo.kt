package com.android.launcher3.model.data

import com.android.launcher3.LauncherSettings
import com.android.launcher3.util.ContentWriter

/**
 * Represents a stack of widgets in the Launcher.
 */
class WidgetStackInfo : CollectionInfo() {

    private val contents = mutableListOf<ItemInfo>()

    init {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_WIDGET_STACK
    }

    override fun add(item: ItemInfo) {
        if (item is LauncherAppWidgetInfo) {
            contents.add(item)
        } else {
            throw IllegalArgumentException("Only LauncherAppWidgetInfo can be added to WidgetStackInfo")
        }
    }

    override fun getContents(): List<ItemInfo> = contents

    override fun getAppContents(): List<WorkspaceItemInfo> = emptyList()

    override fun onAddToDatabase(writer: ContentWriter) {
        super.onAddToDatabase(writer)
        // Stacks don't have a single appWidgetId at the top level
    }

    override fun dumpProperties(): String {
        return super.dumpProperties() + " widgetCount=${contents.size}"
    }
}
