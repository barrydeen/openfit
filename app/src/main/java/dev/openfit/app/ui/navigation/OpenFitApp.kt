package dev.openfit.app.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.coach.CoachScreen
import dev.openfit.app.ui.history.HistoryScreen
import dev.openfit.app.ui.home.HomeScreen
import dev.openfit.app.ui.macros.CaptureScreen
import dev.openfit.app.ui.macros.GalleryScreen
import dev.openfit.app.ui.macros.MacrosScreen
import dev.openfit.app.ui.macros.MacrosViewModel
import dev.openfit.app.ui.macros.ReviewScreen
import dev.openfit.app.ui.picker.ExercisePickerScreen
import dev.openfit.app.ui.progress.ProgressScreen
import dev.openfit.app.ui.settings.SettingsScreen
import dev.openfit.app.ui.summary.SummaryScreen
import dev.openfit.app.ui.theme.OpenFitTheme
import dev.openfit.app.ui.workout.WorkoutScreen

private data class TabItem(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun OpenFitApp() {
    val container = appContainer()
    val dynamicColor by container.settingsRepository.dynamicColor.collectAsState(initial = false)

    OpenFitTheme(dynamicColor = dynamicColor) {
        OpenFitAppContent()
    }
}

@Composable
private fun OpenFitAppContent() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.tabRoutes

    val container = appContainer()
    val context = LocalContext.current.applicationContext
    val macrosVm: MacrosViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MacrosViewModel(context as Application, container.mealRepository, container.settingsRepository) }
        }
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(currentRoute = currentRoute, navController = navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        ) {
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(Routes.COACH) { CoachScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.MACROS) { MacrosTab(navController, macrosVm) }
            composable(Routes.HISTORY) { HistoryScreen(navController) }
            composable(Routes.PROGRESS) { ProgressScreen(navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }

            composable(Routes.LOG_MEAL) {
                CaptureScreen(
                    createUri = macrosVm::createImageUri,
                    onCaptured = { success ->
                        macrosVm.onPhotoCaptured(success)
                        navController.navigate(Routes.REVIEW)
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(Routes.REVIEW) {
                val state by macrosVm.state.collectAsState()
                val meals by macrosVm.meals.collectAsState()
                ReviewScreen(
                    imagePath = state.imagePath,
                    draft = state.draft,
                    isAnalyzing = state.isAnalyzing,
                    error = state.error,
                    onRetry = { state.imagePath?.let(macrosVm::analyze) },
                    onSave = { dish, cal, p, c, f ->
                        macrosVm.saveDraft(dish, cal, p, c, f)
                        navController.popBackStack(Routes.MACROS, inclusive = false)
                    },
                    onUseRecent = macrosVm::useRecent,
                    onCancel = { navController.popBackStack(Routes.MACROS, inclusive = false) },
                    recent = meals
                )
            }

            composable(Routes.GALLERY) {
                val meals by macrosVm.meals.collectAsState()
                GalleryScreen(
                    meals = meals,
                    onBack = { navController.popBackStack() },
                    onDeleteMeal = macrosVm::deleteMeal
                )
            }

            composable(
                route = Routes.WORKOUT,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { entry ->
                val workoutId = entry.arguments?.getLong("workoutId") ?: return@composable
                WorkoutScreen(navController, workoutId)
            }

            composable(
                route = Routes.PICK_EXERCISE,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { entry ->
                val workoutId = entry.arguments?.getLong("workoutId") ?: return@composable
                ExercisePickerScreen(navController, workoutId)
            }

            composable(
                route = Routes.SUMMARY,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { entry ->
                val workoutId = entry.arguments?.getLong("workoutId") ?: return@composable
                SummaryScreen(navController, workoutId)
            }
        }
    }
}

@Composable
private fun MacrosTab(navController: NavHostController, vm: MacrosViewModel) {
    val meals by vm.meals.collectAsState()
    val settings by vm.macroSettings.collectAsState()
    val totals = vm.todayTotals(meals)
    MacrosScreen(
        settings = settings,
        totals = totals,
        formatTime = vm::formatTime,
        onLogMeal = { navController.navigate(Routes.LOG_MEAL) },
        onGallery = { navController.navigate(Routes.GALLERY) },
        onDeleteMeal = vm::deleteMeal
    )
}

@Composable
private fun AppBottomBar(currentRoute: String?, navController: NavHostController) {
    val tabs = listOf(
        TabItem(Routes.HOME, "Home", { Icon(Icons.Filled.FitnessCenter, contentDescription = null) }),
        TabItem(Routes.MACROS, "Macros", { Icon(Icons.Filled.Restaurant, contentDescription = null) }),
        TabItem(Routes.HISTORY, "History", { Icon(Icons.Filled.History, contentDescription = null) }),
        TabItem(Routes.PROGRESS, "Progress", { Icon(Icons.Filled.BarChart, contentDescription = null) })
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                icon = tab.icon,
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
