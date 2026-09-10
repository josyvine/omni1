package com.vineyard.omnicam.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Landing : Screen("landing", "Welcome", Icons.Filled.Videocam, Icons.Outlined.Videocam)
    data object LiveDashboard : Screen("live_dashboard", "Live Feed", Icons.Filled.Videocam, Icons.Outlined.Videocam)
    data object DriveEvents : Screen("drive_events", "Drive Cloud", Icons.Filled.Cloud, Icons.Outlined.Cloud)
    data object ShareHub : Screen("share_hub", "Share Hub", Icons.Filled.People, Icons.Outlined.People)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                LiveDashboard,
                DriveEvents,
                ShareHub,
                Settings
            )
    }
}
