package com.vineyard.omnicam.app.ui.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vineyard.omnicam.app.data.models.DriveEvent
import com.vineyard.omnicam.app.data.repository.CameraRepository
import com.vineyard.omnicam.app.data.repository.DriveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DriveEventsViewModel(
    private val driveRepository: DriveRepository,
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _selectedCameraId = MutableStateFlow<String?>(null)
    val selectedCameraId: StateFlow<String?> = _selectedCameraId.asStateFlow()

    val storageUsedBytes: StateFlow<Long> = driveRepository.storageUsedBytes
    val totalStorageBytes: Long = driveRepository.totalStorageBytes

    val cameras = cameraRepository.cameras

    val filteredEvents: StateFlow<List<DriveEvent>> = combine(
        driveRepository.events,
        _selectedCameraId
    ) { events, filterId ->
        if (filterId == null) events else events.filter { it.cameraId == filterId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePlayerEvent = MutableStateFlow<DriveEvent?>(null)
    val activePlayerEvent: StateFlow<DriveEvent?> = _activePlayerEvent.asStateFlow()

    fun selectCameraFilter(cameraId: String?) {
        _selectedCameraId.value = cameraId
    }

    fun openPlayer(event: DriveEvent) {
        _activePlayerEvent.value = event
    }

    fun closePlayer() {
        _activePlayerEvent.value = null
    }

    fun deleteEvent(event: DriveEvent) {
        driveRepository.deleteEvent(event.id)
    }

    fun simulateNewClip() {
        val cam = cameraRepository.cameras.value.firstOrNull()
        driveRepository.addSimulatedClip(
            cameraId = cam?.id ?: "cam_sim",
            cameraName = cam?.name ?: "Front Porch",
            eventType = "Motion Detected",
            durationSec = 15
        )
    }
}
