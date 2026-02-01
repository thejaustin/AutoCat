package app.lawnchair.search

import android.content.SearchRecentSuggestionsProvider
import com.android.launcher3.BuildConfig

class AutoCatRecentSuggestionProvider : SearchRecentSuggestionsProvider() {
    companion object {
        const val AUTHORITY = BuildConfig.APPLICATION_ID + ".search.AutoCatRecentSuggestionProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }

    init {
        setupSuggestions(AUTHORITY, MODE)
    }
}
