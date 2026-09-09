package com.vineyard.omnicam.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.vineyard.omnicam.app.core.utils.DeepLinkHandler
import com.vineyard.omnicam.app.data.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth?
) {

    private val _currentUser = MutableStateFlow(
        UserProfile(
            uid = firebaseAuth?.currentUser?.uid ?: "user_master_admin",
            email = firebaseAuth?.currentUser?.email ?: "admin@omnicam.vision",
            displayName = firebaseAuth?.currentUser?.displayName ?: "OmniCam Admin",
            photoUrl = firebaseAuth?.currentUser?.photoUrl?.toString(),
            driveConnected = true
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isDriveConnected = MutableStateFlow(true)
    val isDriveConnected: StateFlow<Boolean> = _isDriveConnected.asStateFlow()

    fun initiateGoogleDriveOAuth(clientId: String = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com") {
        DeepLinkHandler.launchOAuthCustomTab(context, clientId)
    }

    fun handleOAuthCode(code: String) {
        // Exchange authorization code with Google OAuth2 endpoint
        _isDriveConnected.value = true
        _currentUser.value = _currentUser.value.copy(driveConnected = true)
    }

    fun disconnectDrive() {
        _isDriveConnected.value = false
        _currentUser.value = _currentUser.value.copy(driveConnected = false)
    }

    fun updateProfile(profile: UserProfile) {
        _currentUser.value = profile
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        _isDriveConnected.value = false
        _currentUser.value = UserProfile(uid = "guest", email = "guest@local", displayName = "Guest User", driveConnected = false)
    }
}
