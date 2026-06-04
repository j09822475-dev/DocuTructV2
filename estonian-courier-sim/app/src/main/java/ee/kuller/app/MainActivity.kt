package ee.kuller.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ee.kuller.app.ui.screens.HomeScreen
import ee.kuller.app.ui.screens.OrderScreen
import ee.kuller.app.ui.screens.PhrasebookScreen
import ee.kuller.app.ui.screens.ProfileScreen
import ee.kuller.app.ui.screens.VocabularyScreen
import ee.kuller.app.ui.theme.KullerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KullerTheme {
                KullerApp()
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Смена", Icons.Filled.TwoWheeler),
    Tab("vocab", "Слова", Icons.Filled.Storefront),
    Tab("phrasebook", "Разговорник", Icons.AutoMirrored.Filled.MenuBook),
    Tab("profile", "Профиль", Icons.Filled.Person),
)

@Composable
fun KullerApp(vm: GameViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBars = currentRoute != "order"

    Scaffold(
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    val dest = backStack?.destination
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = dest?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(inner)
        ) {
            composable("home") {
                HomeScreen(vm, onAccept = { order ->
                    vm.startOrder(order)
                    navController.navigate("order")
                })
            }
            composable("vocab") { VocabularyScreen(vm) }
            composable("phrasebook") { PhrasebookScreen() }
            composable("profile") { ProfileScreen(vm) }
            composable("order") {
                OrderScreen(vm, onExit = {
                    if (navController.currentBackStackEntry?.destination?.route == "order") {
                        navController.popBackStack()
                    }
                })
            }
        }
    }
}
