package com.vineyard.omnicam.app.core.utils

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

data class OnvifDeviceInfo(
    val manufacturer: String = "Generic ONVIF",
    val model: String = "IP Camera",
    val firmwareVersion: String = "",
    val serialNumber: String = "",
    val hardwareId: String = ""
)

object OnvifXmlParser {

    fun createWsDiscoveryProbe(): String {
        val uuid = java.util.UUID.randomUUID().toString()
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <Envelope xmlns:dn="http://www.onvif.org/ver10/network/wsdl"
                      xmlns="http://www.w3.org/2003/05/soap-envelope">
                <Header>
                    <wsa:MessageID xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">uuid:$uuid</wsa:MessageID>
                    <wsa:To xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To>
                    <wsa:Action xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>
                </Header>
                <Body>
                    <Probe xmlns="http://schemas.xmlsoap.org/ws/2005/04/discovery">
                        <Types>dn:NetworkVideoTransmitter</Types>
                    </Probe>
                </Body>
            </Envelope>
        """.trimIndent()
    }

    fun parseWsDiscoveryXAddrs(xmlResponse: String): List<String> {
        val urls = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlResponse))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("XAddrs", ignoreCase = true)) {
                    val rawAddrs = parser.nextText()
                    rawAddrs.split("\\s+".toRegex()).forEach { addr ->
                        if (addr.isNotBlank() && (addr.startsWith("http://") || addr.startsWith("https://"))) {
                            urls.add(addr.trim())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return urls
    }

    fun createGetDeviceInformationSoapEnvelope(): String {
        return """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
                <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                    <GetDeviceInformation xmlns="http://www.onvif.org/ver10/device/wsdl"/>
                </s:Body>
            </s:Envelope>
        """.trimIndent()
    }

    fun parseDeviceInformationResponse(xml: String): OnvifDeviceInfo {
        var manufacturer = ""
        var model = ""
        var firmware = ""
        var serial = ""
        var hardware = ""

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name.lowercase()) {
                        "manufacturer" -> manufacturer = parser.nextText().trim()
                        "model" -> model = parser.nextText().trim()
                        "firmwareversion" -> firmware = parser.nextText().trim()
                        "serialnumber" -> serial = parser.nextText().trim()
                        "hardwareid" -> hardware = parser.nextText().trim()
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}

        return OnvifDeviceInfo(
            manufacturer = manufacturer.ifEmpty { "Generic ONVIF" },
            model = model.ifEmpty { "IP Camera" },
            firmwareVersion = firmware,
            serialNumber = serial,
            hardwareId = hardware
        )
    }

    fun createGetStreamUriSoapEnvelope(profileToken: String = "Profile_1"): String {
        return """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
                <s:Body>
                    <GetStreamUri xmlns="http://www.onvif.org/ver10/media/wsdl">
                        <StreamSetup>
                            <Stream xmlns="http://www.onvif.org/ver10/schema">RTP-Unicast</Stream>
                            <Transport xmlns="http://www.onvif.org/ver10/schema">
                                <Protocol>RTSP</Protocol>
                            </Transport>
                        </StreamSetup>
                        <ProfileToken>$profileToken</ProfileToken>
                    </GetStreamUri>
                </s:Body>
            </s:Envelope>
        """.trimIndent()
    }

    fun parseStreamUriResponse(xml: String): String? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("Uri", ignoreCase = true)) {
                    val uri = parser.nextText().trim()
                    if (uri.startsWith("rtsp://")) return uri
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return null
    }
}
