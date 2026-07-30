package app.lawnchair.search.algorithms

import android.content.Context
import android.content.pm.ShortcutInfo
import app.lawnchair.autoCatLauncher
import app.lawnchair.ui.preferences.components.HiddenAppsInSearch
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.popup.PopupPopulator
import com.android.launcher3.search.StringMatcherUtility
import com.android.launcher3.shortcuts.ShortcutRequest
import java.util.Locale
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.algorithms.WeightedRatio

object SearchUtils {
    fun normalSearch(apps: List<AppInfo>, query: String, maxResultsCount: Int, hiddenApps: Set<String>, hiddenAppsInSearch: String): List<AppInfo> {
        val queryTextLower = query.lowercase(Locale.getDefault())
        val matcher = StringMatcherUtility.StringMatcher.getInstance()
        return apps.asSequence()
            .filter {
                StringMatcherUtility.matches(queryTextLower, it.title.toString(), matcher) ||
                    isAbbreviation(query, it.title.toString())
            }
            .filterHiddenApps(queryTextLower, hiddenApps, hiddenAppsInSearch)
            .take(maxResultsCount)
            .toList()
    }

    private fun isAbbreviation(query: String, title: String): Boolean {
        if (query.length < 2) return false
        val titleLower = title.lowercase(Locale.getDefault())
        val queryLower = query.lowercase(Locale.getDefault())

        val commonAbbrs = mapOf(
            "fb" to listOf("facebook"),
            "yt" to listOf("youtube"),
            "ig" to listOf("instagram"),
            "gm" to listOf("gmail"),
            "wa" to listOf("whatsapp"),
            "tg" to listOf("telegram"),
            "nf" to listOf("netflix"),
            "ps" to listOf("play store", "google play"),
            "chrome" to listOf("google chrome"),
            "maps" to listOf("google maps"),
            "gc" to listOf("google chrome"),
            "cal" to listOf("calendar"),
        )
        commonAbbrs[queryLower]?.forEach { abbrTarget ->
            if (titleLower.contains(abbrTarget)) return true
        }

        val titleNormalized = titleLower
            .replace(" iii", " 3")
            .replace(" iv", " 4")
            .replace(" v", " 5")
            .replace(" vi", " 6")
            .replace(" vii", " 7")
            .replace(" viii", " 8")
            .replace(" ix", " 9")
            .replace(" x", " 10")

        val words = titleNormalized.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotEmpty() }
        if (words.size >= queryLower.length) {
            val acronym = words.map { it[0] }.joinToString("")
            if (acronym.startsWith(queryLower)) {
                return true
            }
        }

        val titleCamelNormalized = title
            .replace(" III", " 3")
            .replace(" IV", " 4")
            .replace(" V", " 5")
            .replace(" VI", " 6")
            .replace(" VII", " 7")
            .replace(" VIII", " 8")
            .replace(" IX", " 9")
            .replace(" X", " 10")

        val camelWords = titleCamelNormalized.split(Regex("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])|[\\s_\\-\\.]+")).filter { it.isNotEmpty() }
        if (camelWords.size >= queryLower.length) {
            val acronym = camelWords.map { it[0].lowercaseChar() }.joinToString("")
            if (acronym.startsWith(queryLower)) {
                return true
            }
        }

        return false
    }

    fun fuzzySearch(apps: List<AppInfo>, query: String, maxResultsCount: Int, hiddenApps: Set<String>, hiddenAppsInSearch: String): List<AppInfo> {
        val queryTextLower = query.lowercase(Locale.getDefault())
        val filteredApps = apps.asSequence()
            .filterHiddenApps(queryTextLower, hiddenApps, hiddenAppsInSearch)
            .toList()
        val matches = FuzzySearch.extractSorted(
            queryTextLower,
            filteredApps,
            { it.sectionName + it.title },
            WeightedRatio(),
            65,
        )

        return matches.take(maxResultsCount)
            .map { it.referent }
    }

    fun getShortcuts(app: AppInfo, context: Context): List<ShortcutInfo> {
        val shortcuts = ShortcutRequest(context.autoCatLauncher, app.user)
            .withContainer(app.targetComponent)
            .query(ShortcutRequest.PUBLISHED)
        return PopupPopulator.sortAndFilterShortcuts(shortcuts)
    }
}

fun Sequence<AppInfo>.filterHiddenApps(
    query: String,
    hiddenApps: Set<String>,
    hiddenAppsInSearch: String,
): Sequence<AppInfo> {
    return when (hiddenAppsInSearch) {
        HiddenAppsInSearch.ALWAYS -> {
            this
        }

        HiddenAppsInSearch.IF_NAME_TYPED -> {
            filter {
                it.toComponentKey().toString() !in hiddenApps ||
                    it.title.toString().lowercase(Locale.getDefault()) == query
            }
        }

        else -> {
            filter { it.toComponentKey().toString() !in hiddenApps }
        }
    }
}
