package com.vineyard.omnicam.app.data.models

data class UserProfile(
    val uid: String = "guest_user",
    val email: String = "local.user@omnicam.vision",
    val displayName: String = "OmniCam Admin",
    val photoUrl: String? = null,
    val driveConnected: Boolean = true,
    val driveStorageUsedBytes: Long = 1_420_000_000L, // 1.4 GB
    val driveStorageTotalBytes: Long = 15L * 1024 * 1024 * 1024, // 15 GB
    val biometricEnabled: Boolean = false,
    val autoDeleteDays: Int = 30,
    val clipDurationSeconds: Int = 20,
    val themeMode: String = "DARK",
    val currentHome: String = "my_home"
)
