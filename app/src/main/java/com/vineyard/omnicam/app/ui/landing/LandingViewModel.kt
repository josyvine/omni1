package com.vineyard.omnicam.app.ui.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vineyard.omnicam.app.data.models.UserProfile
import com.vineyard.omnicam.app.data.repository.AuthRepository
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import com.vineyard.omnicam.app.di.FirebaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class LandingViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val firebaseModule: FirebaseModule
) : ViewModel() {

    private val _showByoDialog = MutableStateFlow(false)
    val showByoDialog: StateFlow<Boolean> = _showByoDialog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isDriveConnected: StateFlow<Boolean> = authRepository.isDriveConnected
    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser

    /**
     * Observable Project ID extracted dynamically from custom google-services.json.
     * Emits null when no custom JSON is configured.
     */
    val configuredProjectId: StateFlow<String?> = settingsRepository.customFirebaseJson
        .map { json ->
            if (!json.isNullOrBlank()) {
                try {
                    val root = JSONObject(json)
                    if (root.has("project_info")) {
                        root.getJSONObject("project_info").optString("project_id", "Configured")
                    } else if (root.has("p")) {
                        root.optString("p", "Configured")
                    } else {
                        "Active"
                    }
                } catch (_: Exception) {
                    "Active"
                }
            } else {
                null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun openByoDialog() {
        _showByoDialog.value = true
    }

    fun dismissByoDialog() {
        _showByoDialog.value = false
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setGuestConnected(adminEmail: String) {
        _statusMessage.value = "Connected to House Admin: $adminEmail"
    }

    fun connectGoogleDrive() {
        authRepository.initiateGoogleDriveOAuth()
    }

    /**
     * Signs in with Google ID token on Central Developer Firebase
     * and silently bridges into the User Admin's private Firebase with Email/Password.
     */
    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signInWithGoogle(idToken)
            _isLoading.value = false

            if (result.isSuccess) {
                _statusMessage.value = "Signed in as ${result.getOrNull()?.email}"
            } else {
                _statusMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Google Sign-In failed."
            }
            onResult(result.isSuccess)
        }
    }

    /**
     * Saves the custom Firebase configuration and dynamically mounts the "admin_cam_app" instance.
     */
    fun saveByoFirebase(json: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            settingsRepository.saveCustomFirebaseJson(json)
            val success = firebaseModule.initializeByoFirebase(json)
            _isLoading.value = false

            if (success) {
                _statusMessage.value = "House Admin Firebase successfully configured!"
            } else {
                _statusMessage.value = "Failed to initialize Firebase with provided JSON."
            }
            onComplete(success)
        }
    }

    /**
     * Clears the custom Firebase configuration and resets to default.
     */
    fun clearByoFirebase() {
        viewModelScope.launch {
            settingsRepository.clearCustomFirebaseJson()
            firebaseModule.clearCustomConfig()
            _statusMessage.value = "Firebase configuration cleared."
        }
    }
}