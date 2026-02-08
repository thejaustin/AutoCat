package app.lawnchair.categorization

/**
 * Centralized constants for the categorization system.
 * Consolidates hardcoded values for easier maintenance and tuning.
 */
object CategorizationConstants {

    // ===== HTTP Timeouts (for LLM providers) =====
    const val HTTP_CONNECT_TIMEOUT_SEC = 30L
    const val HTTP_READ_TIMEOUT_SEC = 60L
    const val HTTP_WRITE_TIMEOUT_SEC = 60L

    // ===== Folder Operations =====
    const val FOLDER_OPERATION_TIMEOUT_MS = 10000L

    // ===== Batch Processing =====
    const val TOKENS_PER_APP = 50
    const val PROMPT_OVERHEAD = 200
    const val SAFETY_MARGIN = 0.3f
    const val DEFAULT_BATCH_SIZE = 20

    // ===== Retry Logic =====
    const val RATE_LIMIT_DELAY_MS = 1000L
    const val INITIAL_RETRY_DELAY_MS = 1000L
    const val MAX_RETRIES = 3

    // ===== Special Category Names =====
    const val UNCATEGORIZED_TAB = "Other"
    const val DEFAULT_CATEGORY_NAME = "Uncategorized"

    // ===== Cache & Logging =====
    const val MAX_LOG_ENTRIES = 200

    // ===== Helper Functions =====

    /**
     * Check if a tab name represents an uncategorized/default category.
     */
    fun isUncategorized(tabName: String?): Boolean = tabName.isNullOrEmpty() ||
        tabName == UNCATEGORIZED_TAB ||
        tabName == DEFAULT_CATEGORY_NAME

    /**
     * Check if a tab name represents a valid user-defined category.
     */
    fun isCategorized(tabName: String?): Boolean = !isUncategorized(tabName)
}
