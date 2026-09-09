package com.vineyard.omnicam.app.ui.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vineyard.omnicam.app.data.repository.AuthRepository
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import com.vineyard.omnicam.app.di.FirebaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LandingViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val firebaseModule: FirebaseModule
) : ViewModel() {

    private val _showByoDialog = MutableStateFlow(false)
    val showByoDialog: StateFlow<Boolean> = _showByoDialog.asStateFlow()

    val isDriveConnected: StateFlow<Boolean> = authRepository.isDriveConnected

    fun openByoDialog() {
        _showByoDialog.value = true
    }

    fun dismissByoDialog() {
        _showByoDialog.value = false
    }

    fun connectGoogleDrive() {
        authRepository.initiateGoogleDriveOAuth()
    }

    fun saveByoFirebase(json: String) {
        viewModelScope.launch {
            settingsRepository.saveCustomFirebaseJson(json)
            firebaseModule.initializeByoFirebase(json)
        }
    }
}
