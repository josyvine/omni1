package com.vineyard.omnicam.app.core.utils

import android.content.Context
import android.net.wifi.WifiManager
import com.vineyard.omnicam.app.core.constants.ApiEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

data class DiscoveredDeviceRaw(
    val ipAddress: String,
    val port: Int,
    val onvifXAddr: String? = null,
    val rawDiscoveryXml: String? = null
)

class NetworkScanner(private val context: Context) {

    suspend fun sendOnvifMulticastProbe(timeoutMs: Int = 3000): List<DiscoveredDeviceRaw> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DiscoveredDeviceRaw>()
        var socket: MulticastSocket? = null
        var multicastLock: WifiManager.MulticastLock? = null

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("OmniCamOnvifProbe")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            val group = InetAddress.getByName(ApiEndpoints.ONVIF_MULTICAST_IP)
            socket = MulticastSocket(ApiEndpoints.ONVIF_WS_DISCOVERY_PORT).apply {
                soTimeout = timeoutMs
                joinGroup(group)
            }

            val probeXml = OnvifXmlParser.createWsDiscoveryProbe()
            val probeBytes = probeXml.toByteArray(StandardCharsets.UTF_8)
            val packet = DatagramPacket(probeBytes, probeBytes.size, group, ApiEndpoints.ONVIF_WS_DISCOVERY_PORT)
            socket.send(packet)

            val receiveBuffer = ByteArray(4096)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)
                    val responseStr = String(receivePacket.data, 0, receivePacket.length, StandardCharsets.UTF_8)
                    val senderIp = receivePacket.address.hostAddress ?: ""
                    val xAddrs = OnvifXmlParser.parseWsDiscoveryXAddrs(responseStr)

                    discovered.add(
                        DiscoveredDeviceRaw(
                            ipAddress = senderIp,
                            port = receivePacket.port,
                            onvifXAddr = xAddrs.firstOrNull(),
                            rawDiscoveryXml = responseStr
                        )
                    )
                } catch (_: java.net.SocketTimeoutException) {
                    break
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
        } finally {
            try {
                socket?.close()
                multicastLock?.release()
            } catch (_: Exception) {}
        }
        discovered
    }

    suspend fun checkPortOpen(ip: String, port: Int, timeoutMs: Int = 500): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
