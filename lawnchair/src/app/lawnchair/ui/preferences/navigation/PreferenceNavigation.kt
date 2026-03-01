package app.lawnchair.ui.preferences.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.lawnchair.backup.ui.CreateBackupScreen
import app.lawnchair.backup.ui.restoreBackupGraph
import app.lawnchair.preferences.BasePreferenceManager
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.about.About
import app.lawnchair.ui.preferences.about.acknowledgements.Acknowledgements
import app.lawnchair.ui.preferences.components.colorpreference.ColorPreferenceModelList
import app.lawnchair.ui.preferences.components.colorpreference.ColorSelection
import app.lawnchair.ui.preferences.components.search.SearchProviderPreferenceScreen
import app.lawnchair.ui.preferences.destinations.AppCategorizationListPreferences
import app.lawnchair.ui.preferences.destinations.AppDrawerFoldersPreference
import app.lawnchair.ui.preferences.destinations.AppDrawerPreferences
import app.lawnchair.ui.preferences.destinations.BackupPreferences
import app.lawnchair.ui.preferences.destinations.CategorizationSettingsPreferences
import app.lawnchair.ui.preferences.destinations.CustomIconShapePreference
import app.lawnchair.ui.preferences.destinations.DebugMenuPreferences
import app.lawnchair.ui.preferences.destinations.DeveloperPreferences
import app.lawnchair.ui.preferences.destinations.DiagnosticsPreferences
import app.lawnchair.ui.preferences.destinations.DockPreferences
import app.lawnchair.ui.preferences.destinations.DummyPreference
import app.lawnchair.ui.preferences.destinations.ExperimentalFeaturesPreferences
import app.lawnchair.ui.preferences.destinations.FeatureFlagsPreference
import app.lawnchair.ui.preferences.destinations.FolderPreferences
import app.lawnchair.ui.preferences.destinations.FontSelection
import app.lawnchair.ui.preferences.destinations.GeneralPreferences
import app.lawnchair.ui.preferences.destinations.GesturePreferences
import app.lawnchair.ui.preferences.destinations.HiddenAppsPreferences
import app.lawnchair.ui.preferences.destinations.HomeScreenGridPreferences
import app.lawnchair.ui.preferences.destinations.HomeScreenPreferences
import app.lawnchair.ui.preferences.destinations.IconPackPreferences
import app.lawnchair.ui.preferences.destinations.IconPickerPreference
import app.lawnchair.ui.preferences.destinations.IconShapePreference
import app.lawnchair.ui.preferences.destinations.LLMSettingsPreferences
import app.lawnchair.ui.preferences.destinations.LauncherPopupPreference
import app.lawnchair.ui.preferences.destinations.PickAppForGesture
import app.lawnchair.ui.preferences.destinations.PreferencesDashboard
import app.lawnchair.ui.preferences.destinations.QuickstepPreferences
import app.lawnchair.ui.preferences.destinations.SearchPreferences
import app.lawnchair.ui.preferences.destinations.SearchProviderPreferences
import app.lawnchair.ui.preferences.destinations.SelectAppsForDrawerFolder
import app.lawnchair.ui.preferences.destinations.SelectIconPreference
import app.lawnchair.ui.preferences.destinations.ShapePreference
import app.lawnchair.ui.preferences.destinations.SmartCategoriesOnboardingPreferences
import app.lawnchair.ui.preferences.destinations.SmartspacePreferences
import app.lawnchair.ui.preferences.destinations.TabManagementPreferences
import com.android.launcher3.util.ComponentKey
import soup.compose.material.motion.animation.materialSharedAxisXIn
import soup.compose.material.motion.animation.materialSharedAxisXOut
import soup.compose.material.motion.animation.rememberSlideDistance

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
...
import androidx.compose.runtime.CompositionLocalProvider
import app.lawnchair.ui.preferences.LocalAnimatedVisibilityScope
import app.lawnchair.ui.preferences.LocalSharedTransitionScope
...
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PreferenceNavigation(
    navController: NavHostController,
    startDestination: PreferenceRoute,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val slideDistance = rememberSlideDistance()

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { materialSharedAxisXIn(!isRtl, slideDistance) },
            exitTransition = { materialSharedAxisXOut(!isRtl, slideDistance) },
            popEnterTransition = { materialSharedAxisXIn(isRtl, slideDistance) },
            popExitTransition = { materialSharedAxisXOut(isRtl, slideDistance) },
        ) {
            composable<Root> {
                val isExpandedScreen = LocalIsExpandedScreen.current

                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this@composable,
                ) {
                    PreferencesDashboard(
                        currentRoute = Root,
                        onNavigate = {
                            navController.navigate(it)
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                    )
                }
...
            LaunchedEffect(isExpandedScreen) {
                if (isExpandedScreen) {
                    navController.navigate(General) {
                        launchSingleTop = true
                        popUpTo(navController.graph.id)
                    }
                }
            }
        }
        composable<Dummy> {
            DummyPreference()
        }

        composable<General> { GeneralPreferences() }
        composable<GeneralFontSelection> { backStackEntry ->
            val route: GeneralFontSelection = backStackEntry.toRoute()
            val pref = preferenceManager().prefsMap[route.prefKey]
                as? BasePreferenceManager.FontPref ?: return@composable
            FontSelection(pref)
        }
        composable<GeneralIconPack> { IconPackPreferences() }
        composable<GeneralIconShape> { backStackEntry ->
            val route: GeneralIconShape = backStackEntry.toRoute()
            ShapePreference(currentTab = route.selectedId)
        }
        composable<GeneralCustomIconShapeCreator> { CustomIconShapePreference() }

        composable<HomeScreen> { HomeScreenPreferences() }
        composable<HomeScreenGrid> { HomeScreenGridPreferences() }
        composable<HomeScreenPopupEditor> { LauncherPopupPreference() }

        composable<Dock> { DockPreferences() }
        composable<DockSearchProvider> { SearchProviderPreferences() }

        composable<Smartspace> { SmartspacePreferences(fromWidget = false) }
        composable<SmartspaceWidget> { SmartspacePreferences(fromWidget = true) }

        composable<AppDrawer> { AppDrawerPreferences() }
        composable<AppDrawerHiddenApps> { HiddenAppsPreferences() }
        composable<AppDrawerCategorizationSettings> {
            CategorizationSettingsPreferences(
                onNavigate = { route: PreferenceRoute -> navController.navigate(route) },
            )
        }
        composable<AppDrawerTabManagement> { TabManagementPreferences() }
        composable<AppDrawerAppListToFolder> { backStackEntry ->
            val args = backStackEntry.arguments!!
            val folderInfoId = args.getInt("id")
            SelectAppsForDrawerFolder(folderInfoId)
        }
        composable<AppDrawerFolder> { AppDrawerFoldersPreference() }
        composable<AppDrawerAppCategorizations> { AppCategorizationListPreferences() }
        composable<AppDrawerLLMSettings> { LLMSettingsPreferences() }
        composable<AppDrawerDiagnostics> { DiagnosticsPreferences() }
        composable<SmartCategoriesOnboarding> {
            SmartCategoriesOnboardingPreferences(
                onFinish = { navController.popBackStack() },
            )
        }

        composable<Search> { backStackEntry ->
            val route: Search = backStackEntry.toRoute()
            SearchPreferences(currentTab = route.selectedId)
        }
        composable<SearchProviderPreference> { backStackEntry ->
            val route: SearchProviderPreference = backStackEntry.toRoute()
            SearchProviderPreferenceScreen(route.id)
        }

        composable<Folders> { FolderPreferences() }

        composable<Gestures> { GesturePreferences() }
        composable<GesturesPickApp> { PickAppForGesture() }

        composable<Quickstep> { QuickstepPreferences() }

        composable<About> { About() }
        composable<AboutLicenses> { Acknowledgements() }

        composable<DebugMenu> { DebugMenuPreferences() }
        composable<FeatureFlags> { FeatureFlagsPreference() }

        composable<BackupAndRestore> { BackupPreferences() }
        composable<Developer> { DeveloperPreferences() }

        composable<SelectIcon> { backStackEntry ->
            val args: SelectIcon = backStackEntry.toRoute()
            val componentKey = args.componentKey
            val key = ComponentKey.fromString(componentKey)!!
            SelectIconPreference(key)
        }
        composable<IconPicker> { backStackEntry ->
            val args: IconPicker = backStackEntry.toRoute()
            IconPickerPreference(packageName = args.packageName)
        }

        composable<ExperimentalFeatures> { ExperimentalFeaturesPreferences() }
        composable<ColorSelection> { backStackEntry ->
            val screen: ColorSelection = backStackEntry.toRoute()
            val modelList = ColorPreferenceModelList.INSTANCE.get(LocalContext.current)
            val model = modelList[screen.prefKey]
            ColorSelection(
                label = stringResource(id = model.labelRes),
                preference = model.prefObject,
                dynamicEntries = model.dynamicEntries,
            )
        }

        composable<CreateBackup> { CreateBackupScreen(viewModel()) }

        restoreBackupGraph()
    }
}
