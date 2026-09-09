package com.vineyard.omnicam.app.data.models

data class BrandProfile(
    val brandId: String,
    val brandName: String,
    val defaultRtspPort: Int = 554,
    val defaultOnvifPort: Int = 2020,
    val streamPathTemplates: List<String> = listOf("/stream1", "/live/ch0", "/h264Preview_01_main"),
    val defaultUsername: String = "admin",
    val authType: String = "ONVIF_AUTH", // ONVIF_AUTH, DIGEST, BASIC, TUYA_P2P, BRAND_CLOUD
    val supportsPtz: Boolean = true,
    val description: String = ""
)
