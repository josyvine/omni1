package com.vineyard.omnicam.app.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vineyard.omnicam.app.core.theme.AmberWarning
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.EmeraldLive
import com.vineyard.omnicam.app.core.theme.RoseAlert
import com.vineyard.omnicam.app.data.models.CameraEntity
import com.vineyard.omnicam.app.data.models.ShareToken
import com.vineyard.omnicam.app.data.repository.CameraRepository
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import com.vineyard.omnicam.app.domain.usecases.GenerateShareQrUseCase
import com.vineyard.omnicam.app.domain.usecases.ProcessScannedQrUseCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShareHubScreen(
    cameraRepository: CameraRepository,
    generateShareQrUseCase: GenerateShareQrUseCase,
    processScannedQrUseCase: ProcessScannedQrUseCase? = null,
    settingsRepository: SettingsRepository? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val cameras by cameraRepository.cameras.collectAsState()
    val customFirebaseJson by (settingsRepository?.customFirebaseJson?.collectAsState(initial = null) 
        ?: remember { mutableStateOf<String?>(null) })

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var scanStatusMessage by remember { mutableStateOf<String?>(null) }
    var isScanSuccess by remember { mutableStateOf(true) }

    var activeTokens by remember {
        mutableStateOf(
            listOf(
                ShareToken(
                    token = "token_guest_1",
                    adminUserId = "admin_master",
                    adminEmail = "master@omnicam.local",
                    cameraIds = listOf("cam_1"),
                    permission = "VIEW_ONLY",
                    expiresAt = System.currentTimeMillis() + (24 * 3600 * 1000L),
                    createdAt = System.currentTimeMillis()
                ),
                ShareToken(
                    token = "token_family_2",
                    adminUserId = "admin_master",
                    adminEmail = "master@omnicam.local",
                    cameraIds = listOf("cam_1", "cam_2"),
                    permission = "FULL_CONTROL_PTZ",
                    expiresAt = 0L,
                    createdAt = System.currentTimeMillis()
                )
            )
        )
    }

    // Generate QR Code Dialog
    if (showGenerateDialog) {
        GenerateQrDialog(
            cameras = cameras,
            onDismiss = { showGenerateDialog = false },
            onGenerate = { ids, perm, hours ->
                val expires = if (hours > 0) System.currentTimeMillis() + (hours * 3600 * 1000L) else 0L
                val tokenObj = generateShareQrUseCase.generateShareToken(
                    adminEmail = "master@omnicam.local",
                    selectedCameraIds = ids,
                    permission = perm,
                    durationHours = hours
                )
                activeTokens = listOf(tokenObj) + activeTokens
                
                // Encodes ShareToken + minified fbConfig (under 250 bytes)
                val encrypted = generateShareQrUseCase.encodeToEncryptedPayload(tokenObj)
                
                // Renders high-contrast QR at ErrorCorrectionLevel.M
                generateShareQrUseCase.renderQrBitmap(encrypted)
            }
        )
    }

    // CameraX QR Code Scanner Dialog
    if (showScanDialog) {
        ScanQrDialog(
            onDismissRequest = { showScanDialog = false },
            onQrCodeScanned = { rawPayload ->
                showScanDialog = false
                if (processScannedQrUseCase != null) {
                    coroutineScope.launch {
                        val result = processScannedQrUseCase(rawPayload)
                        if (result.isSuccess) {
                            val token = result.getOrNull()
                            scanStatusMessage = "Connected to ${token?.adminEmail ?: "Admin"}'s Home!"
                            isScanSuccess = true
                        } else {
                            scanStatusMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to process QR code."
                            isScanSuccess = false
                        }
                    }
                } else {
                    scanStatusMessage = "QR Scanned: Ready to import configuration."
                    isScanSuccess = true
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header with Share and Scan Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Family & Guest Sharing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Zero-Cloud Peer-to-Peer Access via Encrypted QR",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanAccent
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Scan QR Button
                OutlinedButton(
                    onClick = { showScanDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_open_scan_qr")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan", color = CyanAccent)
                }

                // Share QR Button
                Button(
                    onClick = { showGenerateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_open_generate_qr")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = Color.Black)
                }
            }
        }

        // Status Feedback Banner (if a QR code was scanned)
        if (scanStatusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isScanSuccess) EmeraldLive.copy(alpha = 0.15f) else RoseAlert.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isScanSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isScanSuccess) EmeraldLive else RoseAlert,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = scanStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { scanStatusMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Information Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyanAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "End-to-End Encrypted Handshake",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Share temporary or permanent camera feeds directly with family members without creating third-party cloud accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Active Access Grants (${activeTokens.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(activeTokens, key = { it.token }) { token ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = token.adminEmail,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (token.permission == "VIEW_ONLY") CyanAccent.copy(alpha = 0.2f) else EmeraldLive.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = token.permission,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (token.permission == "VIEW_ONLY") CyanAccent else EmeraldLive
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cameras: ${token.cameraIds.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (token.expiresAt == 0L) {
                                    "Expires: Never"
                                } else {
                                    "Expires: " + SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(token.expiresAt))
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                activeTokens = activeTokens.filter { it.token != token.token }
                            },
                            modifier = Modifier.testTag("btn_revoke_${token.token}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Revoke Access",
                                tint = RoseAlert
                            )
                        }
                    }
                }
            }
        }
    }
}