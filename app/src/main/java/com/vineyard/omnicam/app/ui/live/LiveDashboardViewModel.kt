package com.vineyard.omnicam.app.ui.live

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vineyard.omnicam.app.data.models.BrandProfile
import com.vineyard.omnicam.app.data.models.CameraEntity
import com.vineyard.omnicam.app.data.repository.CameraRepository
import com.vineyard.omnicam.app.data.repository.DriveRepository
import com.vineyard.omnicam.app.domain.usecases.AuthenticateBrandUseCase
import com.vineyard.omnicam.app.domain.usecases.DiscoverCamerasUseCase
import com.vineyard.omnicam.app.domain.usecases.IdentifyDeviceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DashboardLayoutMode {
    QUAD_2X2,
    SINGLE_FOCUS,
    LIST
}

class LiveDashboardViewModel(
    private val cameraRepository: CameraRepository,
    private val driveRepository: DriveRepository,
    private val discoverCamerasUseCase: DiscoverCamerasUseCase,
    private val identifyDeviceUseCase: IdentifyDeviceUseCase,
    private val authenticateBrandUseCase: AuthenticateBrandUseCase
) : ViewModel() {

    private val _layoutMode = MutableStateFlow(DashboardLayoutMode.QUAD_2X2)
    val layoutMode: StateFlow<DashboardLayoutMode> = _layoutMode.asStateFlow()

    private val _currentHome = MutableStateFlow("my_home")
    val currentHome: StateFlow<String> = _currentHome.asStateFlow()

    val cameras: StateFlow<List<CameraEntity>> = combine(
        cameraRepository.cameras,
        _currentHome
    ) { list, home ->
        list.filter { it.homeId == home }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusedCamera = MutableStateFlow<CameraEntity?>(null)
    val focusedCamera: StateFlow<CameraEntity?> = _focusedCamera.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredCameras = MutableStateFlow<List<CameraEntity>>(emptyList())
    val discoveredCameras: StateFlow<List<CameraEntity>> = _discoveredCameras.asStateFlow()

    private val _brandProfiles = MutableStateFlow<List<BrandProfile>>(emptyList())
    val brandProfiles: StateFlow<List<BrandProfile>> = _brandProfiles.asStateFlow()

    private val _showAddBottomSheet = MutableStateFlow(false)
    val showAddBottomSheet: StateFlow<Boolean> = _showAddBottomSheet.asStateFlow()

    init {
        loadBrandProfiles()
    }

    private fun loadBrandProfiles() {
        viewModelScope.launch {
            _brandProfiles.value = identifyDeviceUseCase.getBrandProfiles()
        }
    }

    fun setLayoutMode(mode: DashboardLayoutMode) {
        _layoutMode.value = mode
    }

    fun switchHome(homeId: String) {
        _currentHome.value = homeId
        cameraRepository.setHome(homeId)
    }

    fun focusCamera(camera: CameraEntity?) {
        _focusedCamera.value = camera
    }

    fun openAddCameraSheet() {
        _showAddBottomSheet.value = true
        startLocalDiscovery()
    }

    fun dismissAddCameraSheet() {
        _showAddBottomSheet.value = false
    }

    fun startLocalDiscovery() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val results = discoverCamerasUseCase()
                _discoveredCameras.value = results
            } catch (_: Exception) {
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun addCamera(camera: CameraEntity) {
        val updated = camera.copy(homeId = _currentHome.value)
        cameraRepository.addCamera(updated)
    }

    fun removeCamera(cameraId: String) {
        cameraRepository.removeCamera(cameraId)
    }

    fun sendPtzCommand(direction: String) {
        // Dispatches ONVIF ContinuousMove SOAP envelope or P2P socket command
    }

    fun triggerSnapshot(camera: CameraEntity) {
        driveRepository.addSimulatedClip(camera.id, camera.name, "Manual Snapshot", 5)
    }

    fun triggerRecordToggle(camera: CameraEntity, isRecording: Boolean) {
        if (!isRecording) {
            driveRepository.addSimulatedClip(camera.id, camera.name, "Manual Recording", 15)
        }
    }

    fun generateTuyaQr(ssid: String, pass: String): Bitmap {
        return authenticateBrandUseCase.generateTuyaPairingQr(ssid, pass)
    }
}
