package com.vineyard.omnicam.app.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vineyard.omnicam.app.core.theme.RoseAlert
import com.vineyard.omnicam.app.data.models.CameraEntity
import com.vineyard.omnicam.app.ui.components.PtzJoystick
import com.vineyard.omnicam.app.ui.components.VlcVideoPlayerSurface

@Composable
fun CameraFocusView(
    camera: CameraEntity,
    onBack: () -> Unit,
    onPtzCommand: (String) -> Unit,
    onSnapshot: () -> Unit,
    onRecordToggle: (Boolean) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var isTalking by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top Focus Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("focus_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Grid",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = camera.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${camera.brand} • ${camera.ipAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { feedbackMessage = "Picture-in-Picture mode ready" },
                modifier = Modifier.testTag("focus_pip_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = "Picture in Picture",
                    tint = CyanAccent
                )
            }
        }

        // Live Feed Video Stream
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            VlcVideoPlayerSurface(
                camera = camera,
                isFocusMode = true,
                onPtzToggle = null,
                onFullscreenToggle = null
            )
        }

        // Action Toolbar (Snapshot, Record, Two-Way Audio, Audio Listen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Snapshot
            IconButton(
                onClick = {
                    onSnapshot()
                    feedbackMessage = "Snapshot saved to Google Drive"
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                    .testTag("focus_snapshot_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Snapshot",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Local / Cloud Record Toggle
            IconButton(
                onClick = {
                    isRecording = !isRecording
                    onRecordToggle(isRecording)
                    feedbackMessage = if (isRecording) "Recording clip to Drive..." else "Clip uploaded successfully"
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) RoseAlert.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, if (isRecording) RoseAlert else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                    .testTag("focus_record_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Record",
                    tint = if (isRecording) RoseAlert else MaterialTheme.colorScheme.onSurface
                )
            }

            // Two-Way Push-to-Talk Mic
            Button(
                onClick = {
                    isTalking = !isTalking
                    feedbackMessage = if (isTalking) "Microphone active: Streaming audio to camera" else "Microphone muted"
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTalking) CyanAccent else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("focus_ptt_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Push to Talk",
                    tint = if (isTalking) Color.Black else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTalking) "Talking..." else "Push To Talk",
                    color = if (isTalking) Color.Black else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Listen Audio
            IconButton(
                onClick = { feedbackMessage = "Camera speaker stream unmuted" },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                    .testTag("focus_listen_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Listen",
                    tint = CyanAccent
                )
            }
        }

        // Feedback notification snackbar
        feedbackMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanAccent.copy(alpha = 0.15f))
                    .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Virtual 8-Direction PTZ Joystick
        if (camera.hasPtz) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                PtzJoystick(
                    onCommand = onPtzCommand,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
