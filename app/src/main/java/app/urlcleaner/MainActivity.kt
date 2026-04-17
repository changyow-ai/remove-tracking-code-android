package app.urlcleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.urlcleaner.ui.HistoryScreen
import app.urlcleaner.ui.MainScreen
import app.urlcleaner.ui.MainViewModel
import app.urlcleaner.ui.SettingsScreen
import app.urlcleaner.ui.theme.UrlCleanerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UrlCleanerTheme {
                val vm: MainViewModel = viewModel()
                val nav = rememberNavController()
                NavHost(nav, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            viewModel = vm,
                            onOpenHistory = { nav.navigate("history") },
                            onOpenSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
                    composable("history") { HistoryScreen(vm) { nav.popBackStack() } }
                }
            }
        }
    }
}
