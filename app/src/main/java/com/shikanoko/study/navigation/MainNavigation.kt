package com.shikanoko.study.navigation

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
import com.shikanoko.study.DBScreen
import com.shikanoko.study.MainScreen
import com.shikanoko.study.R
import com.shikanoko.study.TestingScreen
import com.shikanoko.study.components.TestingSettingsDialog
import com.shikanoko.study.data.TestingSettings
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(navController: NavHostController){
    val testingSettings = remember { mutableStateOf(TestingSettings()) }

    val composableScope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val openSettingsDialog = remember { mutableStateOf(false) }

    if(openSettingsDialog.value){
        TestingSettingsDialog( { openSettingsDialog.value = false}, {it ->
            testingSettings.value = it
            openSettingsDialog.value = false
            navController.navigate(TestingScreen.route)
        })
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
                        openSettingsDialog.value = true
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = stringResource(id = R.string.menu_db_name)) },
                    selected = false,
                    onClick = { navController.navigate(DBScreen.route)
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
                    com.shikanoko.study.screens.MainScreen()
                }
                composable (route = TestingScreen.route ) {
                    com.shikanoko.study.screens.TestingScreen(navController, testingSettings)
                }
                composable (route = DBScreen.route) {
                    com.shikanoko.study.screens.DBScreen()
                }
            }
        }
    }
}
