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
import com.shikanoko.study.ui.components.TestingSettingsDialog
import com.shikanoko.study.data.db.WordStat
import com.shikanoko.study.data.model.TestingSettings
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

    if(openSettingsDialog.value){
        TestingSettingsDialog(
            onDismiss = { openSettingsDialog.value = false },
            onConfirm = { settings ->
                testingSettings.value = settings
                reviewIgnoreLimit.value = false
                openSettingsDialog.value = false
                navController.navigate(settingsTarget.value)
            },
            onStudyMore = { settings ->
                testingSettings.value = settings
                reviewIgnoreLimit.value = true
                openSettingsDialog.value = false
                navController.navigate(ReviewScreen.route)
            },
            showDirections = settingsTarget.value == ReviewScreen.route
        )
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
                    onClick = { navController.navigate(MainScreen.route)
                        composableScope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_testing_name)) },
                    selected = false,
                    onClick = {
                        composableScope.launch { drawerState.close() }
                        settingsTarget.value = TestingScreen.route
                        openSettingsDialog.value = true
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_review_name)) },
                    selected = false,
                    onClick = {
                        composableScope.launch { drawerState.close() }
                        settingsTarget.value = ReviewScreen.route
                        openSettingsDialog.value = true
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_vocab_name)) },
                    selected = false,
                    onClick = { navController.navigate(MinnaScreen.route)
                        composableScope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_stats_name)) },
                    selected = false,
                    onClick = { navController.navigate(StatisticsScreen.route)
                        composableScope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_db_name)) },
                    selected = false,
                    onClick = { navController.navigate(DBScreen.route)
                        composableScope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_settings_name)) },
                    selected = false,
                    onClick = { navController.navigate(SettingsScreen.route)
                        composableScope.launch { drawerState.close() }
                    }
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
                composable (route = TestingScreen.route ) {
                    com.shikanoko.study.ui.screens.TestingScreen(navController, testingSettings)
                }
                composable (route = ReviewScreen.route ) {
                    com.shikanoko.study.ui.screens.ReviewScreen(
                        navController, testingSettings, reviewIgnoreLimit.value
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
                        com.shikanoko.study.ui.screens.WordStatDetailScreen(
                            stat = stat,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
