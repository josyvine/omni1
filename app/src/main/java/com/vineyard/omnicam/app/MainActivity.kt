package com.vineyard.omnicam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vineyard.omnicam.app.core.theme.OmniCamTheme
import com.vineyard.omnicam.app.core.theme.ThemeMode
import com.vineyard.omnicam.app.ui.components.BottomNavBar
import com.vineyard.omnicam.app.ui.drive.DriveEventsScreen
import com.vineyard.omnicam.app.ui.drive.DriveEventsViewModel
import com.vineyard.omnicam.app.ui.landing.LandingScreen
import com.vineyard.omnicam.app.ui.landing.LandingViewModel
import com.vineyard.omnicam.app.ui.live.LiveDashboardScreen
import com.vineyard.omnicam.app.ui.live.LiveDashboardViewModel
import com.vineyard.omnicam.app.ui.navigation.Screen
import com.vineyard.omnicam.app.ui.settings.SettingsScreen
import com.vineyard.omnicam.app.ui.share.ShareHubScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appModule = (application as OmniCamApplication).appModule

        setContent {
            val currentTheme by appModule.settingsRepository.themeMode.collectAsState(initial = ThemeMode.DARK)

            OmniCamTheme(themeMode = currentTheme) {
                MainAppNavigation(appModule = appModule)
            }
        }
    }
}

@Composable
fun MainAppNavigation(appModule: com.vineyard.omnicam.app.di.AppModule) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null && currentRoute != Screen.Landing.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.LiveDashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.LiveDashboard.route
            ) {
                composable(Screen.Landing.route) {
                    val landingVm: LandingViewModel = viewModel {
                        LandingViewModel(
                            appModule.authRepository,
                            appModule.settingsRepository,
                            appModule.firebaseModule
                        )
                    }
                    LandingScreen(
                        viewModel = landingVm,
                        onNavigateToDashboard = {
                            navController.navigate(Screen.LiveDashboard.route) {
                                popUpTo(Screen.Landing.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.LiveDashboard.route) {
                    val liveVm: LiveDashboardViewModel = viewModel {
                        LiveDashboardViewModel(
                            appModule.cameraRepository,
                            appModule.driveRepository,
                            appModule.discoverCamerasUseCase,
                            appModule.identifyDeviceUseCase,
                            appModule.authenticateBrandUseCase
                        )
                    }
                    LiveDashboardScreen(viewModel = liveVm)
                }

                composable(Screen.DriveEvents.route) {
                    val driveVm: DriveEventsViewModel = viewModel {
                        DriveEventsViewModel(
                            appModule.driveRepository,
                            appModule.cameraRepository
                        )
                    }
                    DriveEventsScreen(viewModel = driveVm)
                }

                composable(Screen.ShareHub.route) {
                    ShareHubScreen(
                        cameraRepository = appModule.cameraRepository,
                        generateShareQrUseCase = appModule.generateShareQrUseCase
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(settingsRepository = appModule.settingsRepository)
                }
            }
        }
    }
}
