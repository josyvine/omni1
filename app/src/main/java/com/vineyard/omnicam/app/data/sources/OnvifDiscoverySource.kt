package com.vineyard.omnicam.app.data.sources

import com.vineyard.omnicam.app.core.utils.MacAddressResolver
import com.vineyard.omnicam.app.core.utils.NetworkScanner
import com.vineyard.omnicam.app.core.utils.OnvifXmlParser
import com.vineyard.omnicam.app.data.models.CameraEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class OnvifDiscoverySource(
    private val networkScanner: NetworkScanner,
    private val okHttpClient: OkHttpClient
) {

    suspend fun discoverLocalCameras(): List<CameraEntity> = withContext(Dispatchers.IO) {
        val discoveredCameras = mutableListOf<CameraEntity>()

        // 1. WS-Discovery Multicast Probe
        val rawDevices = networkScanner.sendOnvifMulticastProbe()

        for (device in rawDevices) {
            val ip = device.ipAddress
            val mac = MacAddressResolver.getMacForIp(ip)
            val brandFromMac = MacAddressResolver.identifyBrandFromMac(mac)

            var manufacturer = brandFromMac ?: "Generic ONVIF"
            var model = "Smart Camera"
            var rtspUri: String? = null

            // 2. Query ONVIF SOAP GetDeviceInformation if XAddrs is present
            device.onvifXAddr?.let { xAddr ->
                try {
                    val soapBody = OnvifXmlParser.createGetDeviceInformationSoapEnvelope()
                    val request = Request.Builder()
                        .url(xAddr)
                        .post(soapBody.toRequestBody("application/soap+xml; charset=utf-8".toMediaType()))
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val info = OnvifXmlParser.parseDeviceInformationResponse(body)
                        if (info.manufacturer.isNotBlank() && info.manufacturer != "Generic ONVIF") {
                            manufacturer = info.manufacturer
                        }
                        if (info.model.isNotBlank()) {
                            model = info.model
                        }
                    }
                } catch (_: Exception) {}

                // Try GetStreamUri
                try {
                    val streamSoap = OnvifXmlParser.createGetStreamUriSoapEnvelope()
                    val request = Request.Builder()
                        .url(xAddr)
                        .post(streamSoap.toRequestBody("application/soap+xml; charset=utf-8".toMediaType()))
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        rtspUri = OnvifXmlParser.parseStreamUriResponse(body)
                    }
                } catch (_: Exception) {}
            }

            val defaultPath = when {
                manufacturer.contains("Tapo", ignoreCase = true) || manufacturer.contains("TP-Link", ignoreCase = true) -> "/stream1"
                manufacturer.contains("Hikvision", ignoreCase = true) -> "/Streaming/Channels/101"
                manufacturer.contains("Dahua", ignoreCase = true) -> "/cam/realmonitor?channel=1&subtype=0"
                manufacturer.contains("Reolink", ignoreCase = true) -> "/h264Preview_01_main"
                else -> "/stream1"
            }

            discoveredCameras.add(
                CameraEntity(
                    id = UUID.randomUUID().toString(),
                    name = "$manufacturer $model",
                    ipAddress = ip,
                    port = 554,
                    onvifPort = 2020,
                    macAddress = mac,
                    brand = manufacturer,
                    model = model,
                    rtspPath = rtspUri?.substringAfter("$ip:554") ?: defaultPath,
                    mode = "RTSP_ONVIF",
                    hasPtz = true,
                    isOnline = true
                )
            )
        }

        discoveredCameras
    }
}
