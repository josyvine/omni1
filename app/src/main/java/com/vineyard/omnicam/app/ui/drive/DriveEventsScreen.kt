package com.vineyard.omnicam.app.ui.drive

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vineyard.omnicam.app.core.theme.AmberWarning
import com.vineyard.omnicam.app.core.theme.CyanAccent
import com.vineyard.omnicam.app.core.theme.EmeraldLive
import com.vineyard.omnicam.app.core.theme.IndigoAccent
import com.vineyard.omnicam.app.data.models.DriveEvent
import com.vineyard.omnicam.app.ui.components.StorageProgressBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DriveEventsScreen(
    viewModel: DriveEventsViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.filteredEvents.collectAsState()
    val cameras by viewModel.cameras.collectAsState()
    val selectedCameraId by viewModel.selectedCameraId.collectAsState()
    val storageUsed by viewModel.storageUsedBytes.collectAsState()
    val activePlayerEvent by viewModel.activePlayerEvent.collectAsState()

    activePlayerEvent?.let { event ->
        DriveVideoPlayerDialog(
            event = event,
            onDismiss = { viewModel.closePlayer() },
            onDelete = { delEvent -> viewModel.deleteEvent(delEvent) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
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
                    text = "Cloud Recordings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Anti-Theft Backup • Google Drive",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanAccent
                )
            }

            IconButton(
                onClick = { viewModel.simulateNewClip() },
                modifier = Modifier.testTag("btn_simulate_clip")
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = "Simulate Motion",
                    tint = CyanAccent
                )
            }
        }

        // Google Drive Storage Meter
        StorageProgressBar(
            usedBytes = storageUsed,
            totalBytes = viewModel.totalStorageBytes
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Camera Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCameraId == null,
                    onClick = { viewModel.selectCameraFilter(null) },
                    label = { Text("All Cameras") },
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("filter_chip_all")
                )
            }
            items(cameras) { cam ->
                FilterChip(
                    selected = selectedCameraId == cam.id,
                    onClick = { viewModel.selectCameraFilter(cam.id) },
                    label = { Text(cam.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("filter_chip_${cam.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chronological Recording Events
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No motion events recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("drive_events_list")
            ) {
                items(events, key = { it.id }) { event ->
                    DriveEventCard(
                        event = event,
                        onClick = { viewModel.openPlayer(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveEventCard(
    event: DriveEvent,
    onClick: () -> Unit
) {
    val timeFormatted = remember(event.timestamp) {
        SimpleDateFormat("MMM dd • HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
    }

    val eventColor = when {
        event.eventType.contains("Person", ignoreCase = true) -> EmeraldLive
        event.eventType.contains("Vehicle", ignoreCase = true) -> IndigoAccent
        event.eventType.contains("Sound", ignoreCase = true) -> AmberWarning
        else -> CyanAccent
    }

    val eventIcon = when {
        event.eventType.contains("Person", ignoreCase = true) -> Icons.Default.DirectionsWalk
        event.eventType.contains("Sound", ignoreCase = true) -> Icons.Default.VolumeUp
        else -> Icons.Default.NotificationsActive
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("event_card_${event.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Snapshot Preview with Play Icon
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 75.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = event.driveThumbnailUrl,
                    contentDescription = "Event Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Play",
                        tint = CyanAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(eventColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = eventIcon, contentDescription = null, tint = eventColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = event.eventType, style = MaterialTheme.typography.labelSmall, color = eventColor)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${event.durationSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.cameraName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$timeFormatted • ${(event.fileSizeBytes / (1024 * 1024.0)).format(1)} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(Locale.US, this)
