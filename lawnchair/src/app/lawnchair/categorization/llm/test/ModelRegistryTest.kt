package app.lawnchair.categorization.llm.test

import app.lawnchair.categorization.llm.ModelRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRegistryTest {

    @Test
    fun `getAvailableModels filters correctly`() {
        val googleModels = ModelRegistry.getAvailableModels("google_ai")
        assertNotNull(googleModels)
        assertTrue(googleModels.all { it.provider == "google_ai" })
        assertTrue(googleModels.all { it.isAvailable })
    }

    @Test
    fun `getDefaultModel returns appropriate model`() {
        val googleDefault = ModelRegistry.getDefaultModel("google_ai")
        assertNotNull(googleDefault)
        assertEquals("google_ai", googleDefault?.provider)
        assertTrue(googleDefault?.isAvailable == true)

        val invalidProvider = ModelRegistry.getDefaultModel("non_existent")
        assertNull(invalidProvider)
    }

    @Test
    fun `isModelAvailable returns true for valid models`() {
        val googleDefault = ModelRegistry.getDefaultModel("google_ai")
        assertNotNull(googleDefault)
        assertTrue(ModelRegistry.isModelAvailable("google_ai", googleDefault!!.id))
    }

    @Test
    fun `isModelAvailable returns false for invalid or unavailable models`() {
        assertFalse(ModelRegistry.isModelAvailable("google_ai", "non-existent-id"))
        assertFalse(ModelRegistry.isModelAvailable("invalid_provider", "some-id"))
    }

    @Test
    fun `registry contains all supported providers`() {
        val providers = listOf("google_ai", "claude", "openai", "perplexity")
        providers.forEach { provider ->
            val models = ModelRegistry.getAvailableModels(provider)
            assertFalse("Provider $provider should have available models", models.isEmpty())
        }
    }
}
