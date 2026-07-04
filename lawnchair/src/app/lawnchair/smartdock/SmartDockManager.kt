package app.lawnchair.smartdock

import android.content.Context
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherAppState
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.PredictedContainerInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.LauncherSettings.Favorites
import java.util.Calendar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.lifecycleScope

class SmartDockManager(private val launcher: Launcher) {
    private val prefs = PreferenceManager.getInstance(launcher)

    fun start() {
        prefs.autoCatSmartDockEnabled.getAdapter().onEach { enabled ->
            if (enabled) {
                updatePredictions()
            }
        }.launchIn(launcher.lifecycleScope)
    }

    fun updatePredictions() {
        if (!prefs.autoCatSmartDockEnabled.get()) return

        val appState = LauncherAppState.getInstance(launcher)
        val allApps = appState.model.bgDataModel.appsList.data
        if (allApps.isEmpty()) return

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Simple heuristic based on time of day
        val targetCategories = when {
            hour in 5..11 -> listOf("Productivity", "News", "Business")
            hour in 12..17 -> listOf("Social", "Communication", "Finance")
            else -> listOf("Entertainment", "Games", "Video")
        }

        // Just pick some apps matching categories or fallback
        val predictedAppInfos = allApps.shuffled().take(4) // Simulating prediction

        val predictedWorkspaceItems = predictedAppInfos.map { appInfo ->
            appInfo.makeWorkspaceItem(launcher)
        }

        val info = PredictedContainerInfo(Favorites.CONTAINER_HOTSEAT_PREDICTION, predictedWorkspaceItems)
        
        if (launcher is com.android.launcher3.uioverrides.QuickstepLauncher) {
            launcher.hotseatPredictionController?.setPredictedItems(info)
        }
    }
}
