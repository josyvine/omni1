package com.vineyard.omnicam.app.data.models

data class ShareToken(
    val token: String = java.util.UUID.randomUUID().toString(),
    val adminUserId: String = "",
    val adminEmail: String = "",
    val cameraIds: List<String> = emptyList(),
    val permission: String = "VIEW_ONLY", // "VIEW_ONLY", "FULL_CONTROL_PTZ"
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L, // 0 means Never
    val isRevoked: Boolean = false,
    val guestLabel: String = "Family Guest"
) {
    fun isExpired(): Boolean {
        if (expiresAt == 0L) return false
        return System.currentTimeMillis() > expiresAt
    }
}
