package com.vineyard.omnicam.app.di

import android.content.Context
import com.vineyard.omnicam.app.core.security.BiometricHelper
import com.vineyard.omnicam.app.core.security.CryptoManager
import com.vineyard.omnicam.app.data.repository.AuthRepository
import com.vineyard.omnicam.app.data.repository.CameraRepository
import com.vineyard.omnicam.app.data.repository.DriveRepository
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import com.vineyard.omnicam.app.domain.usecases.AuthenticateBrandUseCase
import com.vineyard.omnicam.app.domain.usecases.DiscoverCamerasUseCase
import com.vineyard.omnicam.app.domain.usecases.GenerateShareQrUseCase
import com.vineyard.omnicam.app.domain.usecases.IdentifyDeviceUseCase
import com.vineyard.omnicam.app.domain.usecases.ProcessScannedQrUseCase
import com.vineyard.omnicam.app.domain.usecases.SyncDriveEventsUseCase

class AppModule(val context: Context) {

    val firebaseModule: FirebaseModule by lazy {
        FirebaseModule(context)
    }

    val networkModule: NetworkModule by lazy {
        NetworkModule(context, firebaseModule.firestore)
    }

    val cryptoManager: CryptoManager by lazy {
        CryptoManager()
    }

    val biometricHelper: BiometricHelper by lazy {
        BiometricHelper(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(context, firebaseModule, settingsRepository)
    }

    val cameraRepository: CameraRepository by lazy {
        CameraRepository(firebaseModule.firestore)
    }

    val driveRepository: DriveRepository by lazy {
        DriveRepository()
    }

    val discoverCamerasUseCase: DiscoverCamerasUseCase by lazy {
        DiscoverCamerasUseCase(networkModule.onvifDiscoverySource)
    }

    val identifyDeviceUseCase: IdentifyDeviceUseCase by lazy {
        IdentifyDeviceUseCase(networkModule.dynamicBrandRemoteSource)
    }

    val generateShareQrUseCase: GenerateShareQrUseCase by lazy {
        GenerateShareQrUseCase(cryptoManager, settingsRepository)
    }

    val processScannedQrUseCase: ProcessScannedQrUseCase by lazy {
        ProcessScannedQrUseCase(cryptoManager, settingsRepository, firebaseModule)
    }

    val syncDriveEventsUseCase: SyncDriveEventsUseCase by lazy {
        SyncDriveEventsUseCase(driveRepository)
    }

    val authenticateBrandUseCase: AuthenticateBrandUseCase by lazy {
        AuthenticateBrandUseCase(networkModule.tuyaP2PSource, generateShareQrUseCase)
    }
}