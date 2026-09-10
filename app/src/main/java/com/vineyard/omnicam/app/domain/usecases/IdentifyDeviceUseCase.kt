package com.vineyard.omnicam.app.domain.usecases

import com.vineyard.omnicam.app.core.utils.MacAddressResolver
import com.vineyard.omnicam.app.data.models.BrandProfile
import com.vineyard.omnicam.app.data.sources.DynamicBrandRemoteSource

class IdentifyDeviceUseCase(
    private val brandRemoteSource: DynamicBrandRemoteSource
) {
    suspend fun identify(ip: String, mac: String? = null): Pair<String, String> {
        val resolvedMac = mac ?: MacAddressResolver.getMacForIp(ip)
        val brandFromOUI = MacAddressResolver.identifyBrandFromMac(resolvedMac)

        if (brandFromOUI != null) {
            return Pair(brandFromOUI, "Auto-detected via IEEE OUI ($resolvedMac)")
        }

        val remoteProfiles = brandRemoteSource.fetchBrandProfiles()
        val matchedProfile = remoteProfiles.firstOrNull { it.brandName.contains("Universal", ignoreCase = true) }
            ?: remoteProfiles.firstOrNull()

        return Pair(
            matchedProfile?.brandName ?: "Universal ONVIF",
            "Auto-detected IP Stream ($ip)"
        )
    }

    suspend fun getBrandProfiles(): List<BrandProfile> {
        return brandRemoteSource.fetchBrandProfiles()
    }
}
