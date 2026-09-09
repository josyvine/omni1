package com.vineyard.omnicam.app.data.sources

import com.google.firebase.firestore.FirebaseFirestore
import com.vineyard.omnicam.app.core.constants.ApiEndpoints
import com.vineyard.omnicam.app.data.models.BrandProfile
import kotlinx.coroutines.tasks.await

class DynamicBrandRemoteSource(
    private val firestore: FirebaseFirestore?
) {

    private val defaultBrandProfiles = listOf(
        BrandProfile(
            brandId = "tplink_tapo",
            brandName = "TP-Link Tapo",
            defaultRtspPort = 554,
            defaultOnvifPort = 2020,
            streamPathTemplates = listOf("/stream1", "/stream2"),
            defaultUsername = "admin",
            authType = "DIGEST",
            supportsPtz = true,
            description = "Tapo C100, C200, C310, C500 series RTSP streams"
        ),
        BrandProfile(
            brandId = "hikvision",
            brandName = "Hikvision / Ezviz",
            defaultRtspPort = 554,
            defaultOnvifPort = 80,
            streamPathTemplates = listOf("/Streaming/Channels/101", "/Streaming/Channels/102", "/h264/ch1/main/av_stream"),
            defaultUsername = "admin",
            authType = "BASIC",
            supportsPtz = true,
            description = "Hikvision IP Cameras and NVR channels"
        ),
        BrandProfile(
            brandId = "dahua_imou",
            brandName = "Dahua / Imou",
            defaultRtspPort = 554,
            defaultOnvifPort = 80,
            streamPathTemplates = listOf("/cam/realmonitor?channel=1&subtype=0", "/cam/realmonitor?channel=1&subtype=1"),
            defaultUsername = "admin",
            authType = "DIGEST",
            supportsPtz = true,
            description = "Dahua IPC and Imou Life Smart Cameras"
        ),
        BrandProfile(
            brandId = "reolink",
            brandName = "Reolink",
            defaultRtspPort = 554,
            defaultOnvifPort = 8000,
            streamPathTemplates = listOf("/h264Preview_01_main", "/h264Preview_01_sub"),
            defaultUsername = "admin",
            authType = "ONVIF_AUTH",
            supportsPtz = true,
            description = "Reolink E1, Argus, RLC series"
        ),
        BrandProfile(
            brandId = "tuya_smart",
            brandName = "Tuya / Smart Life / Bulbs",
            defaultRtspPort = 554,
            defaultOnvifPort = 80,
            streamPathTemplates = listOf("/live/ch0", "/onvif1"),
            defaultUsername = "admin",
            authType = "TUYA_P2P",
            supportsPtz = true,
            description = "Smart Light Bulb Cameras, Pan-Tilt E27 sockets"
        ),
        BrandProfile(
            brandId = "xiongmai_icsee",
            brandName = "Xiongmai / iCSee",
            defaultRtspPort = 554,
            defaultOnvifPort = 8899,
            streamPathTemplates = listOf("/user=admin_password=_channel=1_stream=0.sdp", "/onvif1"),
            defaultUsername = "admin",
            authType = "BASIC",
            supportsPtz = true,
            description = "iCSee and XM NetIP generic IP cameras"
        ),
        BrandProfile(
            brandId = "wyze_v380",
            brandName = "Wyze / V380 Pro",
            defaultRtspPort = 554,
            defaultOnvifPort = 554,
            streamPathTemplates = listOf("/live", "/v380"),
            defaultUsername = "admin",
            authType = "BRAND_CLOUD",
            supportsPtz = true,
            description = "Wyze RTSP firmware and V380 Pro cloud bridges"
        )
    )

    suspend fun fetchBrandProfiles(): List<BrandProfile> {
        val fs = firestore ?: return defaultBrandProfiles
        return try {
            val snapshot = fs.collection(ApiEndpoints.FIRESTORE_COLLECTION_PROVIDERS).get().await()
            if (snapshot.isEmpty) {
                defaultBrandProfiles
            } else {
                snapshot.documents.mapNotNull { doc ->
                    val brandId = doc.id
                    val name = doc.getString("brandName") ?: "Unknown"
                    val rtspPort = doc.getLong("defaultRtspPort")?.toInt() ?: 554
                    val onvifPort = doc.getLong("defaultOnvifPort")?.toInt() ?: 2020
                    @Suppress("UNCHECKED_CAST")
                    val paths = (doc.get("streamPathTemplates") as? List<String>) ?: listOf("/stream1")
                    val authType = doc.getString("authType") ?: "ONVIF_AUTH"
                    BrandProfile(
                        brandId = brandId,
                        brandName = name,
                        defaultRtspPort = rtspPort,
                        defaultOnvifPort = onvifPort,
                        streamPathTemplates = paths,
                        authType = authType
                    )
                }
            }
        } catch (_: Exception) {
            defaultBrandProfiles
        }
    }
}
