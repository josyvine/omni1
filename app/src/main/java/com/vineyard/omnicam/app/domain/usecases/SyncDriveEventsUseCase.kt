package com.vineyard.omnicam.app.domain.usecases

import com.vineyard.omnicam.app.data.models.DriveEvent
import com.vineyard.omnicam.app.data.repository.DriveRepository
import kotlinx.coroutines.flow.StateFlow

class SyncDriveEventsUseCase(
    private val driveRepository: DriveRepository
) {
    val events: StateFlow<List<DriveEvent>> = driveRepository.events
    val storageUsed: StateFlow<Long> = driveRepository.storageUsedBytes
    val totalStorage: Long = driveRepository.totalStorageBytes

    fun deleteClip(eventId: String) {
        driveRepository.deleteEvent(eventId)
    }

    fun purgeOldClips(days: Int): Int {
        return driveRepository.purgeClipsOlderThan(days)
    }

    fun triggerMotionSimulation(cameraId: String, cameraName: String, eventType: String = "Motion Detected") {
        driveRepository.addSimulatedClip(cameraId, cameraName, eventType)
    }
}
