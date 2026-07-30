package app.lawnchair.search.algorithms

import android.content.Context
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.search.adapter.SearchTargetCompat
import app.lawnchair.search.adapter.SearchTargetFactory
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherModel
import com.android.launcher3.allapps.BaseAllAppsAdapter
import com.android.launcher3.model.AllAppsList
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.ModelTaskController
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.search.SearchCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enhanced search algorithm that includes semantic results based on app categories.
 *
 * If a user searches for "Games", this algorithm will return apps assigned to
 * the "Games" tab, even if "Games" is not in their name.
 */
class AutoCatSemanticSearchAlgorithm(context: Context) : AutoCatSearchAlgorithm(context) {

    private val appState = LauncherAppState.getInstance(context)
    private val searchTargetFactory = SearchTargetFactory(context)
    private val tabDao = TabDatabase.getInstance(context).tabDao()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun doSearch(query: String, callback: SearchCallback<BaseAllAppsAdapter.AdapterItem>) {
        if (query.length < 2) {
            callback.clearSearchResult()
            return
        }

        appState.model.enqueueModelUpdateTask(object : LauncherModel.ModelUpdateTask {
            override fun execute(app: ModelTaskController, dataModel: BgDataModel, apps: AllAppsList) {
                coroutineScope.launch {
                    val semanticResults = getSemanticResults(apps.data, query)

                    withContext(Dispatchers.Main) {
                        callback.onSearchResult(query, semanticResults)
                    }
                }
            }
        })
    }

    private suspend fun getSemanticResults(
        allApps: List<AppInfo>,
        query: String,
    ): ArrayList<BaseAllAppsAdapter.AdapterItem> {
        val searchTargets = mutableListOf<SearchTargetCompat>()

        // 1. Regular name-based search (High priority)
        val normalResults = SearchUtils.normalSearch(
            allApps.toMutableList(),
            query,
            10,
            emptySet(),
            "none",
        )
        normalResults.forEach { searchTargets.add(searchTargetFactory.createAppSearchTarget(it)) }

        // 2. Semantic/Category-based search (Medium priority)
        // Find apps whose category matches the query directly or via keyword/synonym expansion
        val queryLower = query.lowercase(java.util.Locale.getDefault())
        val mappedCategories = app.lawnchair.categorization.stages.MLCategorizer.KEYWORD_RULES
            .filter { (category, keywords) ->
                category.contains(queryLower, ignoreCase = true) ||
                    keywords.any { it.contains(queryLower) || queryLower.contains(it) } ||
                    (category.equals("Finance", ignoreCase = true) && (queryLower == "money" || queryLower == "cash" || queryLower == "wallet")) ||
                    (category.equals("Health", ignoreCase = true) && (queryLower == "relax" || queryLower == "sleep" || queryLower == "exercise" || queryLower == "run" || queryLower == "heart")) ||
                    (category.equals("Games", ignoreCase = true) && (queryLower == "play" || queryLower == "fun" || queryLower == "toy")) ||
                    (category.equals("Music", ignoreCase = true) && (queryLower == "song" || queryLower == "audio" || queryLower == "sing" || queryLower == "tune")) ||
                    (category.equals("Shopping", ignoreCase = true) && (queryLower == "buy" || queryLower == "store" || queryLower == "deal"))
            }.keys

        val appTabs = tabDao.getAllAppTabs()
        val semanticPackages = appTabs.filter {
            it.tabName.contains(query, ignoreCase = true) ||
                it.subCategory?.contains(query, ignoreCase = true) == true ||
                it.reasoning?.contains(query, ignoreCase = true) == true ||
                mappedCategories.any { cat -> it.tabName.contains(cat, ignoreCase = true) }
        }.map { it.packageName }.toSet()

        if (semanticPackages.isNotEmpty()) {
            val normalPackageNames = normalResults.mapNotNull { it.componentName?.packageName }.toSet()
            val extraApps = allApps.filter {
                it.componentName != null &&
                    semanticPackages.contains(it.componentName!!.packageName) &&
                    !normalPackageNames.contains(it.componentName!!.packageName)
            }
            if (extraApps.isNotEmpty()) {
                // Add a header for semantic results if we have normal results
                if (searchTargets.isNotEmpty()) {
                    searchTargets.add(searchTargetFactory.createHeaderTarget("Suggested by Category"))
                }
                extraApps.take(5).forEach {
                    searchTargets.add(searchTargetFactory.createAppSearchTarget(it))
                }
            }
        }

        // 3. Market search (Low priority)
        searchTargetFactory.createMarketSearchTarget(query)?.let { searchTargets.add(it) }

        setFirstItemQuickLaunch(searchTargets)
        val adapterItems = transformSearchResults(searchTargets)
        return ArrayList(adapterItems)
    }

    override fun cancel(interruptActiveRequests: Boolean) {
        // No-op for now as we use coroutines
    }
}
