package com.vineyard.omnicam.app.data.sources

import com.vineyard.omnicam.app.data.models.CameraEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class RtspStreamSource {

    suspend fun verifyRtspConnection(camera: CameraEntity, timeoutMs: Int = 1200): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(camera.ipAddress, camera.port), timeoutMs)
                val latency = System.currentTimeMillis() - startTime
                Pair(true, latency)
            }
        } catch (_: Exception) {
            Pair(false, -1L)
        }
    }

    fun buildMediaItemUrl(camera: CameraEntity): String {
        return camera.getFullRtspUrl()
    }
}
