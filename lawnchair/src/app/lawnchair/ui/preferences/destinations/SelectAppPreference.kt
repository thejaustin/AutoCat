package app.lawnchair.ui.preferences.destinations

import android.app.Activity
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.AppItem
import app.lawnchair.ui.preferences.components.layout.PreferenceLayoutLazyColumn
import app.lawnchair.ui.preferences.components.layout.preferenceGroupItems
import app.lawnchair.ui.util.OnResult
import app.lawnchair.util.appsState
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.ComponentKey

@Composable
fun SelectAppPreference(requestId: Int) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val apps by appsState() // Get all installed apps

    OnResult<ComponentKey> { selectedComponentKey ->
        // When an app is selected, send the ComponentName back to the caller
        (context as Activity).setResult(
            Activity.RESULT_OK,
            android.content.Intent().putExtra("component_name", selectedComponentKey.componentName.flattenToString()),
        )
        (context as Activity).finish()
    }

    PreferenceLayoutLazyColumn(label = "Select App") {
        preferenceGroupItems(
            items = apps,
            isFirstChild = true,
        ) { _, app ->
            AppItem(
                app = app,
                onClick = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("component_name_result_$requestId", ComponentKey(app.componentName, app.user))
                    navController.popBackStack()
                },
            )
        }
    }
}
