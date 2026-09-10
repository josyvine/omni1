package com.vineyard.omnicam.app.core.utils

import com.vineyard.omnicam.app.core.constants.MacOuiDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader

object MacAddressResolver {

    suspend fun getMacForIp(ipAddress: String): String? = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            // Skip header line: IP address HW type Flags HW address Mask Device
            reader.readLine()
            while (reader.readLine().also { line = it } != null) {
                val tokens = line?.split("\\s+".toRegex()) ?: continue
                if (tokens.size >= 4) {
                    val ip = tokens[0]
                    val mac = tokens[3]
                    if (ip.equals(ipAddress, ignoreCase = true) && mac != "00:00:00:00:00:00") {
                        reader.close()
                        return@withContext mac.uppercase()
                    }
                }
            }
            reader.close()
        } catch (_: Exception) {}
        null
    }

    suspend fun getAllArpEntries(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        try {
            val reader = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            reader.readLine()
            while (reader.readLine().also { line = it } != null) {
                val tokens = line?.split("\\s+".toRegex()) ?: continue
                if (tokens.size >= 4) {
                    val ip = tokens[0]
                    val mac = tokens[3]
                    if (mac.length >= 17 && mac != "00:00:00:00:00:00") {
                        map[ip] = mac.uppercase()
                    }
                }
            }
            reader.close()
        } catch (_: Exception) {}
        map
    }

    fun identifyBrandFromMac(mac: String?): String? {
        return MacOuiDatabase.lookup(mac)
    }
}
