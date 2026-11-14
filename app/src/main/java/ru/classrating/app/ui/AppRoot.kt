package ru.classrating.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import ru.classrating.app.ui.screens.*

private data class NavTab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    NavTab("home", "Главная", Icons.Filled.Home),
    NavTab("diary", "Дневник", Icons.Filled.MenuBook),
    NavTab("schedule", "Уроки", Icons.Filled.CalendarMonth),
    NavTab("rating", "Рейтинг", Icons.Filled.Leaderboard),
    NavTab("homework", "ДЗ", Icons.Filled.Assignment)
)

@Composable
fun AppRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (TABS.any { it.route == route }) NavigationBar {
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            nav, "home", Modifier.padding(bottom = inner.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(250)) }, popExitTransition = { fadeOut(tween(150)) }
        ) {
            composable("home") { HomeScreen(vm, nav) }
            composable("diary") { DiaryScreen(vm, nav) }
            composable("schedule") { ScheduleScreen(vm) }
            composable("rating") { RatingScreen(vm) }
            composable("homework") { HomeworkScreen(vm) }
            composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
            composable("subject/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                SubjectScreen(vm, it.arguments?.getLong("id") ?: 0L) { nav.popBackStack() }
            }
        }
    }
}
