package com.vineyard.omnicam.app.data.models

/**
 * Defines the user roles and associated capabilities within OmniCam.
 * 
 * Accurately aligns with Firestore security rules and repository string values ("admin", "guest")
 * while preventing typo-based privilege escalation or role misidentification.
 */
enum class UserRole(val roleKey: String, val displayName: String) {
    ADMIN("admin", "House Admin"),
    GUEST("guest", "House Member");

    /**
     * Determines if the role is allowed to modify system configurations,
     * upload/reset google-services.json, or alter project settings.
     */
    fun canManageHubConfiguration(): Boolean = this == ADMIN

    /**
     * Determines if the role has authority to add, edit, or delete camera hardware.
     */
    fun canManageHardware(): Boolean = this == ADMIN

    /**
     * Determines if the role can generate, view, or revoke access QR tokens.
     */
    fun canManageSharingTokens(): Boolean = this == ADMIN

    /**
     * Determines if the user can view a given camera feed based on their permitted camera list.
     * Admins have universal access; Guests are restricted to explicitly permitted cameras.
     */
    fun canAccessCamera(cameraId: String, permittedCameras: List<String>?): Boolean {
        return when (this) {
            ADMIN -> true
            GUEST -> permittedCameras?.contains(cameraId) == true
        }
    }

    companion object {
        /**
         * Safely resolves a string value from Firestore/Auth into a strongly-typed UserRole.
         * Defaults to GUEST if unspecified or unrecognized to enforce least-privilege security.
         */
        fun fromString(value: String?): UserRole {
            return when (value?.trim()?.lowercase()) {
                ADMIN.roleKey -> ADMIN
                GUEST.roleKey -> GUEST
                else -> GUEST
            }
        }
    }
}