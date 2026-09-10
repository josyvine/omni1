package com.vineyard.omnicam.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vineyard.omnicam.app.core.theme.OmniCamTheme
import com.vineyard.omnicam.app.core.theme.ThemeMode
import com.vineyard.omnicam.app.di.AppModule
import com.vineyard.omnicam.app.domain.usecases.ProcessScannedQrUseCase
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appModule: AppModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appModule = (application as OmniCamApplication).appModule
        handleOAuthDeepLink(intent)

        setContent {
            val currentTheme by appModule.settingsRepository.themeMode.collectAsState(initial = ThemeMode.DARK)

            OmniCamTheme(themeMode = currentTheme) {
                MainAppNavigation(appModule = appModule)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthDeepLink(intent)
    }

    /**
     * Intercepts Google OAuth 2.0 PKCE deep-link redirects returned by Chrome Custom Tab:
     * com.vineyard.omnicam.app://oauth2redirect?code=AUTHORIZATION_CODE
     * or omnicam://oauth2redirect?code=AUTHORIZATION_CODE
     */
    private fun handleOAuthDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        val scheme = data.scheme ?: ""
        val host = data.host ?: ""
        val path = data.path ?: ""

        val isMatchingScheme = scheme == "com.vineyard.omnicam.app" || scheme == "omnicam"
        val isMatchingHost = host == "oauth2redirect" || path.contains("oauth2redirect")

        if (isMatchingScheme && isMatchingHost) {
            val authCode = data.getQueryParameter("code")
            if (!authCode.isNullOrBlank()) {
                appModule.authRepository.handleOAuthCode(authCode)
            }
        }
    }
}

@Composable
fun MainAppNavigation(appModule: AppModule) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val coroutineScope = rememberCoroutineScope()

    // Determine initial destination: Launch directly into LiveDashboard if user has an active session
    val currentUser by appModule.authRepository.currentUser.collectAsState()
    val activeGuestToken = appModule.settingsRepository.getActiveGuestShareToken()
    val hasActiveSession = currentUser != null || !activeGuestToken.isNullOrBlank()

    val initialRoute = if (hasActiveSession) Screen.LiveDashboard.route else Screen.Landing.route
    val showBottomBar = currentRoute != null && currentRoute != Screen.Landing.route

    // Instantiate ProcessScannedQrUseCase with AuthRepository wired for automatic Firestore sync
    val processScannedQrUseCase = remember {
        ProcessScannedQrUseCase(
            cryptoManager = appModule.cryptoManager,
            settingsRepository = appModule.settingsRepository,
            firebaseModule = appModule.firebaseModule,
            authRepository = appModule.authRepository
        )
    }

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
                startDestination = initialRoute
            ) {
                // Landing / Onboarding Screen
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
                        },
                        onGuestQrScanned = { rawPayload ->
                            coroutineScope.launch {
                                processScannedQrUseCase(rawPayload)
                            }
                        }
                    )
                }

                // Live Camera Dashboard Screen (Home)
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

                // Google Drive Events & Cloud History Screen (Wired to AuthRepository for PKCE auth)
                composable(Screen.DriveEvents.route) {
                    val driveVm: DriveEventsViewModel = viewModel {
                        DriveEventsViewModel(
                            appModule.driveRepository,
                            appModule.cameraRepository
                        )
                    }
                    DriveEventsScreen(
                        viewModel = driveVm,
                        authRepository = appModule.authRepository
                    )
                }

                // Family & Guest Sharing Hub Screen
                composable(Screen.ShareHub.route) {
                    ShareHubScreen(
                        cameraRepository = appModule.cameraRepository,
                        generateShareQrUseCase = appModule.generateShareQrUseCase,
                        processScannedQrUseCase = processScannedQrUseCase
                    )
                }

                // Settings & System Control Screen (Wired to AuthRepository and navigation)
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        settingsRepository = appModule.settingsRepository,
                        authRepository = appModule.authRepository,
                        onNavigateToLanding = {
                            navController.navigate(Screen.Landing.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}