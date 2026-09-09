package com.vineyard.omnicam.app.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.vineyard.omnicam.app.data.models.CameraEntity

@Composable
fun GenerateQrDialog(
    cameras: List<CameraEntity>,
    onDismiss: () -> Unit,
    onGenerate: (selectedCameraIds: List<String>, permission: String, durationHours: Int) -> Bitmap
) {
    val selectedIds = remember { mutableStateListOf<String>().apply { addAll(cameras.map { it.id }) } }
    var selectedPermission by remember { mutableStateOf("VIEW_ONLY") } // "VIEW_ONLY", "FULL_CONTROL_PTZ"
    var durationHours by remember { mutableIntStateOf(24) } // 1, 8, 24, 0
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Generate Family / Guest Access QR",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (generatedBitmap == null) {
                    Text(
                        text = "Select Cameras to Share",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent
                    )

                    cameras.forEach { cam ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(cam.id),
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(cam.id) else selectedIds.remove(cam.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = CyanAccent),
                                modifier = Modifier.testTag("checkbox_share_${cam.id}")
                            )
                            Text(
                                text = cam.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Access Permissions",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedPermission == "VIEW_ONLY",
                            onClick = { selectedPermission = "VIEW_ONLY" },
                            label = { Text("View Only") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("perm_view_only")
                        )
                        FilterChip(
                            selected = selectedPermission == "FULL_CONTROL_PTZ",
                            onClick = { selectedPermission = "FULL_CONTROL_PTZ" },
                            label = { Text("Full Control + PTZ") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("perm_full_ptz")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Access Expiration Timer",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1 to "1 Hour", 8 to "8 Hours", 24 to "24 Hours", 0 to "Never").forEach { (h, label) ->
                            FilterChip(
                                selected = durationHours == h,
                                onClick = { durationHours = h },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("timer_$h")
                            )
                        }
                    }
                } else {
                    // Show generated AES-256 encrypted QR bitmap
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AES-256 Encrypted Share QR",
                            style = MaterialTheme.typography.titleSmall,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = generatedBitmap!!.asImageBitmap(),
                                contentDescription = "Generated QR",
                                modifier = Modifier.size(224.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Have the guest scan this QR from their OmniCam Vision app to receive instant P2P stream access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (generatedBitmap == null) {
                Button(
                    onClick = {
                        generatedBitmap = onGenerate(selectedIds.toList(), selectedPermission, durationHours)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    modifier = Modifier.testTag("btn_confirm_generate_qr")
                ) {
                    Text("Generate Encrypted QR", color = Color.Black)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    modifier = Modifier.testTag("btn_done_qr")
                ) {
                    Text("Done", color = Color.Black)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_qr")
            ) {
                Text("Close")
            }
        }
    )
}
