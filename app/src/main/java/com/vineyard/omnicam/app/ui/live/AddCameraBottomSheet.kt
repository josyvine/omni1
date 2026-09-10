package com.vineyard.omnicam.app.ui.live

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.EmeraldLive
import com.vineyard.omnicam.app.data.models.BrandProfile
import com.vineyard.omnicam.app.data.models.CameraEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCameraBottomSheet(
    onDismiss: () -> Unit,
    onAddCamera: (CameraEntity) -> Unit,
    brandProfiles: List<BrandProfile>,
    discoveredCameras: List<CameraEntity>,
    isScanning: Boolean,
    onTriggerScan: () -> Unit,
    onGenerateTuyaQr: (String, String) -> Bitmap?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_camera_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add Security Camera",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyanAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("ONVIF Discovery") },
                    icon = { Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_onvif_discovery")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Smart Bulb QR") },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_smart_bulb_qr")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Manual RTSP") },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_manual_rtsp")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> OnvifDiscoveryTab(
                    discoveredCameras = discoveredCameras,
                    isScanning = isScanning,
                    onTriggerScan = onTriggerScan,
                    onAddCamera = { camera ->
                        onAddCamera(camera)
                        onDismiss()
                    }
                )
                1 -> SmartBulbQrTab(
                    onGenerateQr = onGenerateTuyaQr,
                    onAddCamera = { camera ->
                        onAddCamera(camera)
                        onDismiss()
                    }
                )
                2 -> ManualRtspTab(
                    brandProfiles = brandProfiles,
                    onAddCamera = { camera ->
                        onAddCamera(camera)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun OnvifDiscoveryTab(
    discoveredCameras: List<CameraEntity>,
    isScanning: Boolean,
    onTriggerScan: () -> Unit,
    onAddCamera: (CameraEntity) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Local WiFi Subnet Discovery",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onTriggerScan,
                modifier = Modifier.testTag("btn_refresh_discovery")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyanAccent)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan Subnet", tint = CyanAccent)
                }
            }
        }

        Text(
            text = "Sending WS-Discovery UDP probes and reading ARP cache to detect TP-Link Tapo, Hikvision, Dahua, Reolink & ONVIF hardware.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (discoveredCameras.isEmpty() && !isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No new cameras detected yet. Tap refresh or connect manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                items(discoveredCameras) { cam ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("discovered_cam_${cam.ipAddress}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cam.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${cam.ipAddress}:${cam.port} • MAC: ${cam.macAddress ?: "ARP Cached"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { onAddCamera(cam) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier.testTag("add_discovered_${cam.ipAddress}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartBulbQrTab(
    onGenerateQr: (String, String) -> Bitmap?,
    onAddCamera: (CameraEntity) -> Unit
) {
    var ssid by remember { mutableStateOf("Home_WiFi_2.4G") }
    var password by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var deviceAdded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Smart Bulb / Tuya / Generic IoT Camera Pairing",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Generate a pairing QR code on your phone screen and hold it 6-8 inches in front of your camera bulb lens until you hear a chime.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("WiFi Network (2.4GHz)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("WiFi Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                qrBitmap = onGenerateQr(ssid, password)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_generate_tuya_qr")
        ) {
            Text("Generate Pairing QR Code", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        qrBitmap?.let { bitmap ->
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Pairing QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    deviceAdded = true
                    val newCam = CameraEntity(
                        name = "Smart Bulb Cam",
                        ipAddress = "192.168.1.118",
                        port = 554,
                        brand = "Tuya Smart Bulb",
                        model = "E27 Smart Light",
                        rtspPath = "/live/ch0",
                        mode = "TUYA_P2P",
                        hasPtz = true,
                        isOnline = true
                    )
                    onAddCamera(newCam)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldLive),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_confirm_tuya_paired")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera Chimed & Connected", color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualRtspTab(
    brandProfiles: List<BrandProfile>,
    onAddCamera: (CameraEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf(brandProfiles.firstOrNull() ?: BrandProfile("tplink_tapo", "TP-Link Tapo")) }
    var cameraName by remember { mutableStateOf("Porch Tapo C200") }
    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("554") }
    var rtspPath by remember { mutableStateOf(selectedBrand.streamPathTemplates.firstOrNull() ?: "/stream1") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Camera Brand Template",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedBrand.brandName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Brand Provider") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("dropdown_brand_template")
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                brandProfiles.forEach { profile ->
                    DropdownMenuItem(
                        text = { Text(profile.brandName) },
                        onClick = {
                            selectedBrand = profile
                            rtspPath = profile.streamPathTemplates.firstOrNull() ?: "/stream1"
                            port = profile.defaultRtspPort.toString()
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = cameraName,
            onValueChange = { cameraName = it },
            label = { Text("Camera Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("IP Address") },
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = rtspPath,
            onValueChange = { rtspPath = it },
            label = { Text("RTSP Stream Path") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                val entity = CameraEntity(
                    name = cameraName,
                    ipAddress = ipAddress,
                    port = port.toIntOrNull() ?: 554,
                    brand = selectedBrand.brandName,
                    model = "IP Camera",
                    rtspPath = rtspPath,
                    username = username,
                    password = password,
                    mode = "RTSP_ONVIF",
                    hasPtz = selectedBrand.supportsPtz,
                    isOnline = true
                )
                onAddCamera(entity)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_save_manual_camera")
        ) {
            Text("Save & Connect Stream", color = Color.Black)
        }
    }
}
