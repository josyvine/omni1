package com.vineyard.omnicam.app.data.repository

import com.vineyard.omnicam.app.data.models.DriveEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class DriveRepository {

    private val initialEvents = listOf(
        DriveEvent(
            id = "event_drive_001",
            cameraId = "cam_tapo_front",
            cameraName = "Front Porch (Tapo C200)",
            timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(12),
            eventType = "Person Detected",
            durationSeconds = 22,
            driveFileId = "drive_file_mp4_001",
            fileSizeBytes = 5_800_000L,
            driveThumbnailUrl = "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=600&auto=format&fit=crop&q=80",
            driveVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        ),
        DriveEvent(
            id = "event_drive_002",
            cameraId = "cam_tuya_backyard",
            cameraName = "Backyard Bulb Cam",
            timestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(45),
            eventType = "Motion Detected",
            durationSeconds = 18,
            driveFileId = "drive_file_mp4_002",
            fileSizeBytes = 4_200_000L,
            driveThumbnailUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80",
            driveVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
        ),
        DriveEvent(
            id = "event_drive_003",
            cameraId = "cam_hik_driveway",
            cameraName = "Driveway 4K",
            timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2),
            eventType = "Vehicle Trigger",
            durationSeconds = 28,
            driveFileId = "drive_file_mp4_003",
            fileSizeBytes = 8_900_000L,
            driveThumbnailUrl = "https://images.unsplash.com/photo-1506521781263-d8422e82f27a?w=600&auto=format&fit=crop&q=80",
            driveVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
        ),
        DriveEvent(
            id = "event_drive_004",
            cameraId = "cam_reolink_garage",
            cameraName = "Garage Doorbell",
            timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5),
            eventType = "Package Delivered",
            durationSeconds = 15,
            driveFileId = "drive_file_mp4_004",
            fileSizeBytes = 3_700_000L,
            driveThumbnailUrl = "https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?w=600&auto=format&fit=crop&q=80",
            driveVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4"
        ),
        DriveEvent(
            id = "event_drive_005",
            cameraId = "cam_tapo_front",
            cameraName = "Front Porch (Tapo C200)",
            timestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(14),
            eventType = "Sound Trigger",
            durationSeconds = 12,
            driveFileId = "drive_file_mp4_005",
            fileSizeBytes = 3_100_000L,
            driveThumbnailUrl = "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=600&auto=format&fit=crop&q=80",
            driveVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
        )
    )

    private val _events = MutableStateFlow<List<DriveEvent>>(initialEvents)
    val events: StateFlow<List<DriveEvent>> = _events.asStateFlow()

    private val _storageUsedBytes = MutableStateFlow(1_420_000_000L) // 1.42 GB
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    val totalStorageBytes: Long = 15L * 1024 * 1024 * 1024 // 15 GB Free

    fun deleteEvent(eventId: String) {
        val event = _events.value.find { it.id == eventId }
        if (event != null) {
            _storageUsedBytes.value = (_storageUsedBytes.value - event.fileSizeBytes).coerceAtLeast(0L)
            _events.value = _events.value.filter { it.id != eventId }
        }
    }

    fun purgeClipsOlderThan(days: Int): Int {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        val oldEvents = _events.value.filter { it.timestamp < cutoff }
        val reclaimedBytes = oldEvents.sumOf { it.fileSizeBytes }
        _storageUsedBytes.value = (_storageUsedBytes.value - reclaimedBytes).coerceAtLeast(0L)
        _events.value = _events.value.filter { it.timestamp >= cutoff }
        return oldEvents.size
    }

    fun addSimulatedClip(cameraId: String, cameraName: String, eventType: String, durationSec: Int = 15) {
        val newEvent = DriveEvent(
            id = "event_${System.currentTimeMillis()}",
            cameraId = cameraId,
            cameraName = cameraName,
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            durationSeconds = durationSec,
            driveFileId = "sim_file_${System.currentTimeMillis()}",
            fileSizeBytes = (durationSec * 250_000L),
            driveThumbnailUrl = "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=600&auto=format&fit=crop&q=80",
            driveVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        )
        _storageUsedBytes.value += newEvent.fileSizeBytes
        _events.value = listOf(newEvent) + _events.value
    }
}
