package ph.hakbang.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ph.hakbang.app.service.StepCounterService
import ph.hakbang.app.ui.AppViewModel
import ph.hakbang.app.ui.ViewModelFactory
import ph.hakbang.app.ui.navigation.HakbangDestination
import ph.hakbang.app.ui.screens.badges.BadgesScreen
import ph.hakbang.app.ui.screens.badges.BadgesViewModel
import ph.hakbang.app.ui.screens.history.HistoryScreen
import ph.hakbang.app.ui.screens.history.HistoryViewModel
import ph.hakbang.app.ui.screens.home.HomeScreen
import ph.hakbang.app.ui.screens.home.HomeViewModel
import ph.hakbang.app.ui.screens.permission.PermissionScreen
import ph.hakbang.app.ui.screens.settings.SettingsScreen
import ph.hakbang.app.ui.theme.HakbangTheme
import ph.hakbang.app.util.LocalAppStrings
import ph.hakbang.app.util.AppStrings
import ph.hakbang.app.util.StringKey

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        val app = application as HakbangApp
        ViewModelFactory(app.userPreferences, app.stepRepository)
    }

    private val appViewModel: AppViewModel by viewModels { viewModelFactory }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startStepCounterService()
        appViewModel.setPermissionAsked(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val profile by appViewModel.userProfile.collectAsState()
            val strings = AppStrings(profile.language)

            HakbangTheme {
                CompositionLocalProvider(LocalAppStrings provides strings) {
                    var permissionGranted by remember { mutableStateOf(hasActivityRecognitionPermission()) }

                    if (permissionGranted) {
                        HakbangAppScaffold(viewModelFactory = viewModelFactory, appViewModel = appViewModel)
                    } else {
                        PermissionScreen(onGrantClick = {
                            requestActivityRecognitionPermission()
                            permissionGranted = hasActivityRecognitionPermission()
                            if (permissionGranted) startStepCounterService()
                        })
                    }
                }
            }
        }

        if (hasActivityRecognitionPermission()) {
            startStepCounterService()
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun startStepCounterService() {
        val intent = Intent(this, StepCounterService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}

@androidx.compose.runtime.Composable
private fun HakbangAppScaffold(viewModelFactory: ViewModelFactory, appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val strings = LocalAppStrings.current

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == HakbangDestination.Home.route,
                    onClick = { navController.navigate(HakbangDestination.Home.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(strings[StringKey.HOME]) }
                )
                NavigationBarItem(
                    selected = currentRoute == HakbangDestination.History.route,
                    onClick = { navController.navigate(HakbangDestination.History.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null) },
                    label = { Text(strings[StringKey.HISTORY]) }
                )
                NavigationBarItem(
                    selected = currentRoute == HakbangDestination.Badges.route,
                    onClick = { navController.navigate(HakbangDestination.Badges.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                    label = { Text(strings[StringKey.BADGES]) }
                )
                NavigationBarItem(
                    selected = currentRoute == HakbangDestination.Settings.route,
                    onClick = { navController.navigate(HakbangDestination.Settings.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(strings[StringKey.SETTINGS]) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HakbangDestination.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(HakbangDestination.Home.route) {
                val vm: HomeViewModel = viewModel(factory = viewModelFactory)
                HomeScreen(viewModel = vm)
            }
            composable(HakbangDestination.History.route) {
                val vm: HistoryViewModel = viewModel(factory = viewModelFactory)
                HistoryScreen(viewModel = vm)
            }
            composable(HakbangDestination.Badges.route) {
                val vm: BadgesViewModel = viewModel(factory = viewModelFactory)
                BadgesScreen(viewModel = vm)
            }
            composable(HakbangDestination.Settings.route) {
                SettingsScreen(viewModel = appViewModel)
            }
        }
    }
}
