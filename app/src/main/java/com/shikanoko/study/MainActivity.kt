package com.shikanoko.study

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shikanoko.study.screens.MainScreen
import com.shikanoko.study.screens.TestingScreen
import com.shikanoko.study.screens.DBScreen
import com.shikanoko.study.ui.theme.ShikanokoTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShikanokoTheme {
                val navController = rememberNavController()
                var currentScreen: NokoDestination by remember { mutableStateOf(MainScreen) }
                ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            Text(
                                stringResource(id = R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp))
                            HorizontalDivider()
                            NavigationDrawerItem(
                                label = { Text(text = stringResource(id = R.string.menu_main_name)) },
                                selected = false,
                                onClick = { navController.navigate(MainScreen.route)
                                    currentScreen = MainScreen}
                            )
                            NavigationDrawerItem(
                                label = { Text(text = stringResource(id = R.string.menu_testing_name)) },
                                selected = false,
                                onClick = { navController.navigate(TestingScreen.route)
                                    currentScreen = TestingScreen}
                            )
                            NavigationDrawerItem(
                                label = { Text(text = stringResource(id = R.string.menu_db_name)) },
                                selected = false,
                                onClick = { navController.navigate(DBScreen.route)
                                    currentScreen = DBScreen}
                            )
                            // ...other drawer items
                        }
                    }
                ) {
                    NavHost(navController = navController, startDestination = MainScreen.route) {
                        composable (route = MainScreen.route ) {
                            MainScreen()
                        }
                        composable (route = TestingScreen.route ) {
                            TestingScreen()
                        }
                        composable (route = DBScreen.route) {
                            DBScreen()
                        }
                    }

                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun GreetingPreview() {
    ShikanokoTheme {
    }
}