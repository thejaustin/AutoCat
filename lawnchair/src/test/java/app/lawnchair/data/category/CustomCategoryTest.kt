package app.lawnchair.data.category

import app.lawnchair.data.category.entities.CustomCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for CustomCategory entity.
 *
 * Tests entity creation, default categories, and constants.
 */
class CustomCategoryTest {

    @Test
    fun `create CustomCategory with all fields`() {
        val customCategory = CustomCategory(
            id = 1,
            name = "My Category",
            colorHex = "#FF5722",
            sortOrder = 5,
            isVisible = true,
            createdAt = 1234567890L,
        )

        assertEquals(1, customCategory.id)
        assertEquals("My Category", customCategory.name)
        assertEquals("#FF5722", customCategory.colorHex)
        assertEquals(5, customCategory.sortOrder)
        assertTrue(customCategory.isVisible)
        assertEquals(1234567890L, customCategory.createdAt)
    }

    @Test
    fun `create CustomCategory with defaults`() {
        val customCategory = CustomCategory(
            name = "Test",
            colorHex = "#000000",
            sortOrder = 0,
        )

        assertEquals(0, customCategory.id)
        assertTrue(customCategory.isVisible)
        assertTrue(customCategory.createdAt > 0)
    }

    @Test
    fun `getDefaultCategories returns expected list`() {
        val categories = CustomCategory.getDefaultCategories()

        assertEquals(7, categories.size)

        assertEquals("Games", categories[0].name)
        assertEquals("Social", categories[1].name)
        assertEquals("Productivity", categories[2].name)
        assertEquals("Tools", categories[3].name)
        assertEquals("Entertainment", categories[4].name)
        assertEquals("Photography", categories[5].name)
        assertEquals("Communication", categories[6].name)
    }

    @Test
    fun `default categories have correct sort order`() {
        val categories = CustomCategory.getDefaultCategories()

        for ((index, category) in categories.withIndex()) {
            assertEquals(index, category.sortOrder)
        }
    }

    @Test
    fun `default categories have colors assigned`() {
        val categories = CustomCategory.getDefaultCategories()

        for (category in categories) {
            assertTrue(category.colorHex.startsWith("#"))
            assertEquals(7, category.colorHex.length)
        }
    }

    @Test
    fun `default categories are all visible`() {
        val categories = CustomCategory.getDefaultCategories()

        for (category in categories) {
            assertTrue(category.isVisible)
        }
    }

    @Test
    fun `color constants are defined`() {
        assertEquals("#4CAF50", CustomCategory.COLOR_GAMES)
        assertEquals("#2196F3", CustomCategory.COLOR_SOCIAL)
        assertEquals("#FF9800", CustomCategory.COLOR_PRODUCTIVITY)
        assertEquals("#9E9E9E", CustomCategory.COLOR_TOOLS)
        assertEquals("#E91E63", CustomCategory.COLOR_ENTERTAINMENT)
        assertEquals("#00BCD4", CustomCategory.COLOR_PHOTOGRAPHY)
        assertEquals("#3F51B5", CustomCategory.COLOR_COMMUNICATION)
    }
}
