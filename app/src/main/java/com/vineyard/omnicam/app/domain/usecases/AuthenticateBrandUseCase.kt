package com.vineyard.omnicam.app.domain.usecases

import android.graphics.Bitmap
import com.vineyard.omnicam.app.data.models.CameraEntity
import com.vineyard.omnicam.app.data.sources.TuyaP2PSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthenticateBrandUseCase(
    private val tuyaP2PSource: TuyaP2PSource,
    private val generateShareQrUseCase: GenerateShareQrUseCase
) {

    fun generateTuyaPairingQr(ssid: String, password: String): Bitmap {
        val payload = tuyaP2PSource.generatePairingQrPayload(ssid, password)
        return generateShareQrUseCase.renderQrBitmap(payload, 512)
    }

    suspend fun authenticateBrandCloud(
        brand: String,
        email: String,
        password: String,
        twoFactorCode: String? = null
    ): Result<CameraEntity> = withContext(Dispatchers.IO) {
        // Simulates brand cloud authentication & returns bridged camera
        kotlinx.coroutines.delay(600)
        if (email.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password required"))
        }

        val dummyCamera = CameraEntity(
            id = "cam_cloud_${UUID.randomUUID()}",
            name = "$brand Cloud Cam",
            ipAddress = "192.168.1.180",
            port = 554,
            onvifPort = 2020,
            brand = brand,
            model = "$brand Pro Vision",
            rtspPath = "/live",
            mode = "BRAND_CLOUD",
            hasPtz = true,
            isOnline = true
        )
        Result.success(dummyCamera)
    }
}
