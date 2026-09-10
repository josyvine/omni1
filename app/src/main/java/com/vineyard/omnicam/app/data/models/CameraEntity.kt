package com.vineyard.omnicam.app.data.models

data class CameraEntity(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Smart Camera",
    val ipAddress: String = "192.168.1.100",
    val port: Int = 554,
    val onvifPort: Int = 2020,
    val macAddress: String? = null,
    val brand: String = "Universal ONVIF",
    val model: String = "IP Camera",
    val rtspPath: String = "/stream1",
    val username: String = "admin",
    val password: String = "",
    val mode: String = "RTSP_ONVIF", // RTSP_ONVIF, TUYA_P2P, BRAND_CLOUD
    val hasPtz: Boolean = true,
    val isOnline: Boolean = true,
    val previewImageUrl: String? = null,
    val homeId: String = "my_home", // "my_home", "shared_home"
    val isShared: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
) {
    fun getFullRtspUrl(): String {
        val auth = if (username.isNotEmpty() || password.isNotEmpty()) {
            val encodedUser = java.net.URLEncoder.encode(username, "UTF-8")
            val encodedPass = java.net.URLEncoder.encode(password, "UTF-8")
            "$encodedUser:$encodedPass@"
        } else ""
        val cleanPath = if (rtspPath.startsWith("/")) rtspPath else "/$rtspPath"
        return "rtsp://$auth$ipAddress:$port$cleanPath"
    }
}
