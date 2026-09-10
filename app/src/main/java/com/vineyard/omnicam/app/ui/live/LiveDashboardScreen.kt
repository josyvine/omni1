package com.vineyard.omnicam.app.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.data.models.CameraEntity
import com.vineyard.omnicam.app.ui.components.CameraTileView

@Composable
fun LiveDashboardScreen(
    viewModel: LiveDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val layoutMode by viewModel.layoutMode.collectAsState()
    val currentHome by viewModel.currentHome.collectAsState()
    val cameras by viewModel.cameras.collectAsState()
    val focusedCamera by viewModel.focusedCamera.collectAsState()
    val showAddSheet by viewModel.showAddBottomSheet.collectAsState()
    val brandProfiles by viewModel.brandProfiles.collectAsState()
    val discoveredCameras by viewModel.discoveredCameras.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var homeMenuExpanded by remember { mutableStateOf(false) }

    // Lifecycle observer to immediately pause/detach camera decoders when switching tabs
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLiveScreenActive by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLiveScreenActive = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isLiveScreenActive = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            isLiveScreenActive = false
        }
    }

    if (focusedCamera != null) {
        CameraFocusView(
            camera = focusedCamera!!,
            onBack = { viewModel.focusCamera(null) },
            onPtzCommand = { dir -> viewModel.sendPtzCommand(dir) },
            onSnapshot = { viewModel.triggerSnapshot(focusedCamera!!) },
            onRecordToggle = { isRec -> viewModel.triggerRecordToggle(focusedCamera!!, isRec) }
        )
        return
    }

    if (showAddSheet) {
        AddCameraBottomSheet(
            onDismiss = { viewModel.dismissAddCameraSheet() },
            onAddCamera = { newCam -> viewModel.addCamera(newCam) },
            brandProfiles = brandProfiles,
            discoveredCameras = discoveredCameras,
            isScanning = isScanning,
            onTriggerScan = { viewModel.startLocalDiscovery() },
            onGenerateTuyaQr = { ssid, pass -> viewModel.generateTuyaQr(ssid, pass) }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddCameraSheet() },
                containerColor = CyanAccent,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_camera")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Camera", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Top App Bar with Multi-Home Switcher and Layout Mode Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multi-Home Switcher
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { homeMenuExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("home_selector_dropdown"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentHome == "my_home") "My Home" else "Shared Cabin (Guest)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = homeMenuExpanded,
                        onDismissRequest = { homeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("My Home (Primary)") },
                            onClick = {
                                viewModel.switchHome("my_home")
                                homeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shared Cabin (Guest Mode)") },
                            onClick = {
                                viewModel.switchHome("shared_home")
                                homeMenuExpanded = false
                            }
                        )
                    }
                }

                // Layout Mode Toggles (Quad 2x2, Single 1x1, List)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LayoutToggleButton(
                        icon = Icons.Default.GridView,
                        isSelected = layoutMode == DashboardLayoutMode.QUAD_2X2,
                        tag = "layout_quad_btn"
                    ) { viewModel.setLayoutMode(DashboardLayoutMode.QUAD_2X2) }

                    LayoutToggleButton(
                        icon = Icons.Default.CropSquare,
                        isSelected = layoutMode == DashboardLayoutMode.SINGLE_FOCUS,
                        tag = "layout_single_btn"
                    ) { viewModel.setLayoutMode(DashboardLayoutMode.SINGLE_FOCUS) }

                    LayoutToggleButton(
                        icon = Icons.Default.ViewAgenda,
                        isSelected = layoutMode == DashboardLayoutMode.LIST,
                        tag = "layout_list_btn"
                    ) { viewModel.setLayoutMode(DashboardLayoutMode.LIST) }
                }
            }

            // Camera Stream Layout Rendering (Only active when screen is in foreground)
            if (cameras.isEmpty()) {
                EmptyCamerasView(onAddClick = { viewModel.openAddCameraSheet() })
            } else if (!isLiveScreenActive) {
                // Solid placeholder while in background or transitioning, preventing CPU choke and surface ghosting
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            } else {
                when (layoutMode) {
                    DashboardLayoutMode.QUAD_2X2 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .testTag("grid_2x2_container")
                        ) {
                            items(cameras, key = { it.id }) { cam ->
                                CameraTileView(
                                    camera = cam,
                                    aspectRatio = 4f / 3f,
                                    onCameraClick = { viewModel.focusCamera(cam) },
                                    onPtzToggle = if (cam.hasPtz) { { viewModel.focusCamera(cam) } } else null,
                                    onFullscreenToggle = { viewModel.focusCamera(cam) }
                                )
                            }
                        }
                    }

                    DashboardLayoutMode.SINGLE_FOCUS -> {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .testTag("grid_single_container")
                        ) {
                            items(cameras, key = { it.id }) { cam ->
                                CameraTileView(
                                    camera = cam,
                                    aspectRatio = 16f / 9f,
                                    onCameraClick = { viewModel.focusCamera(cam) },
                                    onPtzToggle = if (cam.hasPtz) { { viewModel.focusCamera(cam) } } else null,
                                    onFullscreenToggle = { viewModel.focusCamera(cam) }
                                )
                            }
                        }
                    }

                    DashboardLayoutMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .testTag("grid_list_container")
                        ) {
                            items(cameras, key = { it.id }) { cam ->
                                CameraTileView(
                                    camera = cam,
                                    aspectRatio = 21f / 9f,
                                    onCameraClick = { viewModel.focusCamera(cam) },
                                    onPtzToggle = if (cam.hasPtz) { { viewModel.focusCamera(cam) } } else null,
                                    onFullscreenToggle = { viewModel.focusCamera(cam) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanAccent else Color.Transparent)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tag,
            tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyCamerasView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.GridView,
            contentDescription = null,
            tint = CyanAccent.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Cameras in this Home",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan local network via ONVIF or add smart bulb camera using the QR pairer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}