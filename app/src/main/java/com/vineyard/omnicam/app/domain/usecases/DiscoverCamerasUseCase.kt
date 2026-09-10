package com.vineyard.omnicam.app.domain.usecases

import com.vineyard.omnicam.app.data.models.CameraEntity
import com.vineyard.omnicam.app.data.sources.OnvifDiscoverySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiscoverCamerasUseCase(
    private val onvifDiscoverySource: OnvifDiscoverySource
) {
    suspend operator fun invoke(): List<CameraEntity> = withContext(Dispatchers.IO) {
        onvifDiscoverySource.discoverLocalCameras()
    }
}
