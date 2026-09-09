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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.EmeraldLive
import com.vineyard.omnicam.app.core.theme.RoseAlert
import com.vineyard.omnicam.app.data.models.CameraEntity
import com.vineyard.omnicam.app.data.models.ShareToken
import com.vineyard.omnicam.app.data.repository.CameraRepository
import com.vineyard.omnicam.app.domain.usecases.GenerateShareQrUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShareHubScreen(
    cameraRepository: CameraRepository,
    generateShareQrUseCase: GenerateShareQrUseCase,
    modifier: Modifier = Modifier
) {
    val cameras by cameraRepository.cameras.collectAsState()
    var showGenerateDialog by remember { mutableStateOf(false) }
    var activeTokens by remember {
        mutableStateOf(
            listOf(
                ShareToken(
                    token = "token_guest_1",
                    cameraIds = listOf("cam_1"),
                    permission = "VIEW_ONLY",
                    expiresAt = System.currentTimeMillis() + (24 * 3600 * 1000L),
                    guestLabel = "Pet Sitter / Neighbor"
                ),
                ShareToken(
                    token = "token_family_2",
                    cameraIds = listOf("cam_1", "cam_2"),
                    permission = "FULL_CONTROL_PTZ",
                    expiresAt = 0L,
                    guestLabel = "Family Member"
                )
            )
        )
    }

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
                val encrypted = generateShareQrUseCase.encodeToEncryptedPayload(tokenObj)
                generateShareQrUseCase.renderQrBitmap(encrypted)
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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

            Button(
                onClick = { showGenerateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_open_generate_qr")
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share QR", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info Banner
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
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
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
                                    text = token.guestLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (token.permission == "VIEW_ONLY") CyanAccent.copy(alpha = 0.2f) else EmeraldLive.copy(alpha = 0.2f))
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
                                text = if (token.expiresAt == 0L) "Expires: Never" else "Expires: " + SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(token.expiresAt)),
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
                            Icon(Icons.Default.Delete, contentDescription = "Revoke Access", tint = RoseAlert)
                        }
                    }
                }
            }
        }
    }
}
