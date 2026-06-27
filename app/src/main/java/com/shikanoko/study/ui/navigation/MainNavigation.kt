package com.shikanoko.study.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shikanoko.study.ui.destination.DBScreen
import com.shikanoko.study.ui.destination.MainScreen
import com.shikanoko.study.R
import com.shikanoko.study.ui.destination.MinnaScreen
import com.shikanoko.study.ui.destination.ReviewScreen
import com.shikanoko.study.ui.destination.SettingsScreen
import com.shikanoko.study.ui.destination.StatisticsDetailScreen
import com.shikanoko.study.ui.destination.StatisticsScreen
import com.shikanoko.study.ui.destination.TestingScreen
import com.shikanoko.study.ui.screens.TestingSettingsScreen
import com.shikanoko.study.ui.components.StopConfirmDialog
import com.shikanoko.study.ui.components.TestSession
import com.shikanoko.study.data.db.WordStat
import com.shikanoko.study.data.model.TestingSettings
import com.shikanoko.study.ui.destination.ReviewSettingsScreen
import com.shikanoko.study.ui.destination.TestingSettingsScreen
import com.shikanoko.study.ui.screens.WordStatDetailScreen
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(navController: NavHostController){
    val testingSettings = remember { mutableStateOf(TestingSettings()) }

    val composableScope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val openSettingsDialog = remember { mutableStateOf(false) }
    // The destination the settings dialog confirms into: practice testing or SRS review. Drives both
    // the navigation target and whether the dialog shows the (review-only) direction picker.
    val settingsTarget = remember { mutableStateOf(TestingScreen.route) }
    // Review "study more" path: when true, ReviewScreen ignores today's daily limit for a fresh batch.
    val reviewIgnoreLimit = remember { mutableStateOf(false) }

    // The word whose detail screen is showing; set when a Statistics row is tapped.
    val selectedStat = remember { mutableStateOf<WordStat?>(null) }

    // Shared with the test/review screens: lets the drawer warn before abandoning a running session
    // and pause it while the warning is shown.
    val testSession = remember { TestSession() }
    val showExitWarning = remember { mutableStateOf(false) }
    val pendingRoute = remember { mutableStateOf<String?>(null) }

    // Navigating from the drawer: if a session is live, pause it and warn before leaving; otherwise go.
    fun navigateFromDrawer(route: String) {
        composableScope.launch { drawerState.close() }
        if (testSession.active) {
            pendingRoute.value = route
            testSession.pushDialogPause()
            showExitWarning.value = true
        } else {
            navController.navigate(route)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet{
                Text(
                    stringResource(id = R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_main_name)) },
                    selected = false,
                    onClick = { navigateFromDrawer(MainScreen.route) }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_testing_name)) },
                    selected = false,
                    onClick = { navigateFromDrawer(TestingSettingsScreen.route) }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_review_name)) },
                    selected = false,
                    onClick = { navigateFromDrawer(ReviewSettingsScreen.route) }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_vocab_name)) },
                    selected = false,
                    onClick = { navigateFromDrawer(MinnaScreen.route) }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_stats_name)) },
                    selected = false,
                    onClick = { navigateFromDrawer(StatisticsScreen.route) }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_db_name)) },
                    selected = false,
                    onClick = { navigateFromDrawer(DBScreen.route) }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_settings_name)) },
                    selected = false,
                    onClick = { navigateFromDrawer(SettingsScreen.route) }
                )
                // ...other drawer items
            }
        }
    ) {
        Surface (color = MaterialTheme.colorScheme.surface) {
            NavHost(navController = navController, startDestination = MainScreen.route) {
                composable (route = MainScreen.route ) {
                    com.shikanoko.study.ui.screens.MainScreen()
                }
                composable (route = TestingSettingsScreen.route ) {
                    TestingSettingsScreen(
                        onConfirm = { settings ->
                            testingSettings.value = settings
                            reviewIgnoreLimit.value = false
                            openSettingsDialog.value = false
                            navController.navigate(TestingScreen.route)
                        },
                        onStudyMore = { settings ->
                            testingSettings.value = settings
                            reviewIgnoreLimit.value = true
                            openSettingsDialog.value = false
                            navController.navigate(ReviewScreen.route)
                        },
                        showDirections = false
                    )
                }
                composable (route = ReviewSettingsScreen.route ) {
                    TestingSettingsScreen(
                        onConfirm = { settings ->
                            testingSettings.value = settings
                            reviewIgnoreLimit.value = false
                            openSettingsDialog.value = false
                            navController.navigate(ReviewScreen.route)
                        },
                        onStudyMore = { settings ->
                            testingSettings.value = settings
                            reviewIgnoreLimit.value = true
                            openSettingsDialog.value = false
                            navController.navigate(ReviewScreen.route)
                        },
                        showDirections = true
                    )
                }
                composable (route = TestingScreen.route ) {
                    com.shikanoko.study.ui.screens.TestingScreen(navController, testingSettings, testSession)
                }
                composable (route = ReviewScreen.route ) {
                    com.shikanoko.study.ui.screens.ReviewScreen(
                        navController, testingSettings, reviewIgnoreLimit.value, testSession
                    )
                }
                composable (route = DBScreen.route) {
                    com.shikanoko.study.ui.screens.DBScreen()
                }
                composable (route = SettingsScreen.route) {
                    com.shikanoko.study.ui.screens.SettingsScreen()
                }
                composable (route = MinnaScreen.route) {
                    com.shikanoko.study.ui.screens.MinnaScreen()
                }
                composable (route = StatisticsScreen.route) {
                    com.shikanoko.study.ui.screens.StatisticsScreen(
                        onWordClick = { stat ->
                            selectedStat.value = stat
                            navController.navigate(StatisticsDetailScreen.route)
                        }
                    )
                }
                composable (route = StatisticsDetailScreen.route) {
                    // selectedStat is always set before navigating here; the null guard just avoids a
                    // blank screen if the back stack is restored without it.
                    selectedStat.value?.let { stat ->
                        WordStatDetailScreen(
                            stat = stat,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    // The drawer tried to leave a running session: confirm first (progress is saved). The session is
    // paused while this is shown (pushDialogPause in navigateFromDrawer); recording runs in
    // composableScope so it survives the navigation that disposes the test screen.
    if (showExitWarning.value) {
        StopConfirmDialog(
            onConfirm = {
                showExitWarning.value = false
                val target = pendingRoute.value
                pendingRoute.value = null
                composableScope.launch {
                    testSession.recorder?.invoke()
                    testSession.end()
                    if (target != null) navController.navigate(target)
                }
            },
            onDismiss = {
                showExitWarning.value = false
                pendingRoute.value = null
                testSession.popDialogPause()
            }
        )
    }
}
