package com.android.launcher3.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.launcher3.model.data.LauncherAppWidgetInfo
import com.android.launcher3.model.data.WidgetStackInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetStackInfoTest {

    @Test
    fun testAddWidget() {
        val stack = WidgetStackInfo()
        val widget1 = LauncherAppWidgetInfo(1, null)
        val widget2 = LauncherAppWidgetInfo(2, null)
        
        stack.add(widget1)
        stack.add(widget2)
        
        assertEquals(2, stack.getContents().size)
        assertEquals(widget1, stack.getContents()[0])
        assertEquals(widget2, stack.getContents()[1])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddInvalidItem() {
        val stack = WidgetStackInfo()
        val notAWidget = com.android.launcher3.model.data.WorkspaceItemInfo()
        
        stack.add(notAWidget)
    }
}
