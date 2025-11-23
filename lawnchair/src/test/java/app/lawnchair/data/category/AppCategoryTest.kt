package app.lawnchair.data.category

import app.lawnchair.data.category.entities.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppCategory entity.
 *
 * Tests entity creation, constants, and helper methods.
 */
class AppCategoryTest {

    @Test
    fun `create AppCategory with all fields`() {
        val appCategory = AppCategory(
            packageName = "com.example.app",
            category = "Games",
            confidence = 0.95f,
            source = AppCategory.SOURCE_BUILT_IN,
            isUserOverride = false,
            lastUpdated = 1234567890L,
        )

        assertEquals("com.example.app", appCategory.packageName)
        assertEquals("Games", appCategory.category)
        assertEquals(0.95f, appCategory.confidence)
        assertEquals(AppCategory.SOURCE_BUILT_IN, appCategory.source)
        assertFalse(appCategory.isUserOverride)
        assertEquals(1234567890L, appCategory.lastUpdated)
    }

    @Test
    fun `create AppCategory with defaults`() {
        val appCategory = AppCategory(
            packageName = "com.example.app",
            category = "Tools",
            confidence = 0.7f,
            source = AppCategory.SOURCE_RULE,
        )

        assertFalse(appCategory.isUserOverride)
        assertTrue(appCategory.lastUpdated > 0)
    }

    @Test
    fun `isReliable returns true for user override`() {
        val appCategory = AppCategory(
            packageName = "com.example.app",
            category = "Games",
            confidence = 0.5f,
            source = AppCategory.SOURCE_USER,
            isUserOverride = true,
        )

        assertTrue(appCategory.isReliable())
    }

    @Test
    fun `isReliable returns true for high confidence`() {
        val appCategory = AppCategory(
            packageName = "com.example.app",
            category = "Games",
            confidence = 0.9f,
            source = AppCategory.SOURCE_BUILT_IN,
        )

        assertTrue(appCategory.isReliable())
    }

    @Test
    fun `isReliable returns true for medium confidence at threshold`() {
        val appCategory = AppCategory(
            packageName = "com.example.app",
            category = "Tools",
            confidence = AppCategory.CONFIDENCE_RULE_MEDIUM,
            source = AppCategory.SOURCE_RULE,
        )

        assertTrue(appCategory.isReliable())
    }

    @Test
    fun `isReliable returns false for low confidence`() {
        val appCategory = AppCategory(
            packageName = "com.example.app",
            category = "Unknown",
            confidence = 0.5f,
            source = AppCategory.SOURCE_ML,
            isUserOverride = false,
        )

        assertFalse(appCategory.isReliable())
    }

    @Test
    fun `source constants are defined correctly`() {
        assertEquals("built-in", AppCategory.SOURCE_BUILT_IN)
        assertEquals("rule", AppCategory.SOURCE_RULE)
        assertEquals("ml", AppCategory.SOURCE_ML)
        assertEquals("user", AppCategory.SOURCE_USER)
    }

    @Test
    fun `confidence thresholds are defined correctly`() {
        assertEquals(0.95f, AppCategory.CONFIDENCE_BUILT_IN)
        assertEquals(0.80f, AppCategory.CONFIDENCE_RULE_HIGH)
        assertEquals(0.70f, AppCategory.CONFIDENCE_RULE_MEDIUM)
        assertEquals(0.60f, AppCategory.CONFIDENCE_ML_THRESHOLD)
    }
}
