package com.vineyard.omnicam.app.data.models

data class DriveEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val cameraId: String,
    val cameraName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String = "Motion Detected", // "Motion Detected", "Person Detected", "Sound Trigger"
    val durationSeconds: Int = 15,
    val driveFileId: String = "",
    val driveThumbnailUrl: String? = null,
    val driveVideoUrl: String? = null,
    val fileSizeBytes: Long = 4_500_000L
)
