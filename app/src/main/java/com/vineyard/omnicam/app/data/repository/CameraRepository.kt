package com.vineyard.omnicam.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.vineyard.omnicam.app.core.constants.ApiEndpoints
import com.vineyard.omnicam.app.data.models.CameraEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class CameraRepository(
    private val firestore: FirebaseFirestore?
) {

    private val initialCameras = listOf(
        CameraEntity(
            id = "cam_tapo_front",
            name = "Front Porch (Tapo C200)",
            ipAddress = "192.168.1.102",
            port = 554,
            onvifPort = 2020,
            macAddress = "50:C7:BF:14:8A:22",
            brand = "TP-Link (Tapo)",
            model = "Tapo C200 PTZ",
            rtspPath = "/stream1",
            username = "admin",
            password = "",
            mode = "RTSP_ONVIF",
            hasPtz = true,
            isOnline = true,
            homeId = "my_home",
            previewImageUrl = "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=600&auto=format&fit=crop&q=80"
        ),
        CameraEntity(
            id = "cam_tuya_backyard",
            name = "Backyard Bulb Cam",
            ipAddress = "192.168.1.115",
            port = 554,
            onvifPort = 80,
            macAddress = "18:69:D8:33:41:9F",
            brand = "Tuya Smart Bulb",
            model = "E27 Smart Light Cam",
            rtspPath = "/live/ch0",
            username = "admin",
            password = "",
            mode = "TUYA_P2P",
            hasPtz = true,
            isOnline = true,
            homeId = "my_home",
            previewImageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80"
        ),
        CameraEntity(
            id = "cam_hik_driveway",
            name = "Driveway 4K",
            ipAddress = "192.168.1.120",
            port = 554,
            onvifPort = 8000,
            macAddress = "48:EA:63:9B:21:40",
            brand = "Hikvision IP Cam",
            model = "DS-2CD2087G2-LU",
            rtspPath = "/Streaming/Channels/101",
            username = "admin",
            password = "",
            mode = "RTSP_ONVIF",
            hasPtz = false,
            isOnline = true,
            homeId = "my_home",
            previewImageUrl = "https://images.unsplash.com/photo-1506521781263-d8422e82f27a?w=600&auto=format&fit=crop&q=80"
        ),
        CameraEntity(
            id = "cam_reolink_garage",
            name = "Garage Doorbell",
            ipAddress = "192.168.1.135",
            port = 554,
            onvifPort = 8000,
            macAddress = "EC:71:DB:44:E2:10",
            brand = "Reolink Security",
            model = "Reolink Video Doorbell",
            rtspPath = "/h264Preview_01_main",
            username = "admin",
            password = "",
            mode = "RTSP_ONVIF",
            hasPtz = false,
            isOnline = true,
            homeId = "my_home",
            previewImageUrl = "https://images.unsplash.com/photo-1584438784894-089d6a62b8fa?w=600&auto=format&fit=crop&q=80"
        ),
        CameraEntity(
            id = "cam_shared_cabin",
            name = "Lake Cabin Deck",
            ipAddress = "10.0.0.45",
            port = 554,
            onvifPort = 2020,
            macAddress = "E4:AA:EC:66:33:11",
            brand = "Dahua / Imou",
            model = "Imou Cruiser 2",
            rtspPath = "/cam/realmonitor?channel=1&subtype=0",
            username = "guest",
            password = "",
            mode = "RTSP_ONVIF",
            hasPtz = true,
            isOnline = true,
            homeId = "shared_home",
            isShared = true,
            previewImageUrl = "https://images.unsplash.com/photo-1518780664697-55e3ad937233?w=600&auto=format&fit=crop&q=80"
        )
    )

    private val _cameras = MutableStateFlow<List<CameraEntity>>(initialCameras)
    val cameras: StateFlow<List<CameraEntity>> = _cameras.asStateFlow()

    private val _currentHome = MutableStateFlow("my_home")
    val currentHome: StateFlow<String> = _currentHome.asStateFlow()

    val filteredCameras = _cameras.map { list ->
        val home = _currentHome.value
        list.filter { it.homeId == home }
    }

    fun setHome(homeId: String) {
        _currentHome.value = homeId
    }

    fun addCamera(camera: CameraEntity) {
        val updated = _cameras.value.toMutableList()
        val index = updated.indexOfFirst { it.id == camera.id }
        if (index >= 0) {
            updated[index] = camera
        } else {
            updated.add(0, camera)
        }
        _cameras.value = updated

        // Sync with Firestore if configured
        try {
            firestore?.collection(ApiEndpoints.FIRESTORE_COLLECTION_CAMERAS)
                ?.document(camera.id)
                ?.set(camera)
        } catch (_: Exception) {}
    }

    fun removeCamera(cameraId: String) {
        _cameras.value = _cameras.value.filter { it.id != cameraId }
        try {
            firestore?.collection(ApiEndpoints.FIRESTORE_COLLECTION_CAMERAS)
                ?.document(cameraId)
                ?.delete()
        } catch (_: Exception) {}
    }

    fun updateCameraStatus(cameraId: String, isOnline: Boolean) {
        _cameras.value = _cameras.value.map {
            if (it.id == cameraId) it.copy(isOnline = isOnline, lastSeen = System.currentTimeMillis()) else it
        }
    }

    fun getCameraById(id: String): CameraEntity? {
        return _cameras.value.find { it.id == id }
    }
}
