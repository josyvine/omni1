package com.vineyard.omnicam.app.core.constants

object MacOuiDatabase {
    private val OUI_MAP: Map<String, String> = mapOf(
        // TP-Link / Tapo
        "50:C7:BF" to "TP-Link (Tapo)",
        "30:DE:4B" to "TP-Link (Tapo)",
        "70:4F:57" to "TP-Link (Kasa)",
        "98:DA:C4" to "TP-Link (Tapo)",
        "C0:C9:E3" to "TP-Link",

        // Tuya / Smart Life / Generic Smart Bulbs
        "18:69:D8" to "Tuya Smart Bulb / Cam",
        "D8:1F:12" to "Tuya Smart",
        "68:57:2D" to "Tuya / Smart Life",
        "7C:2C:67" to "Tuya IoT",
        "24:62:AB" to "Tuya Generic IP",

        // Hikvision / Ezviz
        "48:EA:63" to "Hikvision IP Cam",
        "BC:AD:28" to "Hikvision / Ezviz",
        "44:19:B6" to "Hikvision",
        "54:C4:15" to "Ezviz Cloud Cam",

        // Dahua / Imou / Lorex
        "3C:EF:8C" to "Dahua Technology",
        "E4:AA:EC" to "Dahua / Imou",
        "00:1A:61" to "Dahua IPC",
        "90:02:A9" to "Imou Life",

        // Reolink
        "EC:71:DB" to "Reolink Security",
        "48:0E:EC" to "Reolink IPC",

        // Xiongmai / iCSee / NetIP
        "70:B3:D5" to "Xiongmai / iCSee",
        "00:12:12" to "Xiongmai Tech",
        "00:12:17" to "Xiongmai IPC",

        // Wyze Labs
        "2C:AA:8E" to "Wyze Cam",
        "A4:DA:32" to "Wyze Labs",

        // V380 / Macro-video
        "00:27:04" to "V380 / Macro-video",
        "5C:02:72" to "V380 Pro Smart",

        // Eufy / Anker
        "8C:85:90" to "Eufy Security (Anker)",
        "04:78:63" to "EufyCam",

        // Ring & Blink
        "F0:81:74" to "Ring Video Doorbell",
        "3C:24:F0" to "Blink Home Monitor",

        // Ubiquiti / UniFi
        "74:83:C2" to "Ubiquiti UniFi Protect",
        "B4:FB:E4" to "Ubiquiti Protect"
    )

    fun lookup(macAddress: String?): String? {
        if (macAddress.isNullOrBlank()) return null
        val clean = macAddress.trim().uppercase().replace("-", ":")
        val parts = clean.split(":")
        if (parts.size < 3) return null
        val prefix = "${parts[0]}:${parts[1]}:${parts[2]}"
        return OUI_MAP[prefix]
    }
}
