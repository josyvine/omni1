package com.vineyard.omnicam.app.domain.usecases

import android.util.Log
import com.vineyard.omnicam.app.core.security.CryptoManager
import com.vineyard.omnicam.app.data.models.ShareToken
import com.vineyard.omnicam.app.data.repository.AuthRepository
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import com.vineyard.omnicam.app.di.ConfigSource
import com.vineyard.omnicam.app.di.FirebaseModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * UseCase to process, decrypt, and validate a QR code scanned by a Guest / Member.
 * 
 * Flow:
 * 1. Takes the raw encrypted AES-256 string from CameraX / ML Kit.
 * 2. Decrypts it using [CryptoManager].
 * 3. Parses the decrypted JSON to extract the House Admin's minified Firebase configuration (`fbConfig`),
 *    permitted camera IDs, permission level, and expiration timestamp.
 * 4. Checks if the token has expired.
 * 5. Mounts the House Admin's secondary FirebaseApp instance dynamically with ConfigSource.GUEST_QR
 *    so the guest can query the Admin's Firestore database without needing Keystore SHA-1.
 * 6. If the user is already authenticated with Google, silently syncs their profile document
 *    into the House Admin's Firestore under /users/{memberUid} with role "guest".
 */
class ProcessScannedQrUseCase @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val settingsRepository: SettingsRepository,
    private val firebaseModule: FirebaseModule,
    private val authRepository: AuthRepository? = null
) {

    private val tag = "ProcessScannedQrUseCase"

    suspend operator fun invoke(encryptedQrPayload: String): Result<ShareToken> = withContext(Dispatchers.IO) {
        try {
            // 1. Decrypt AES-256 payload
            val decryptedJsonString = cryptoManager.decrypt(encryptedQrPayload.trim())
            if (decryptedJsonString.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Failed to decrypt QR code payload. Invalid key or corrupted data."))
            }

            // 2. Parse the decrypted JSON
            val rootJson = JSONObject(decryptedJsonString)

            val token = rootJson.optString("token", "")
            val adminEmail = rootJson.optString("admin", "")
            val adminUserId = rootJson.optString("adminUserId", "admin_master")
            val permission = rootJson.optString("perm", "VIEW_ONLY")
            val expiresAt = rootJson.optLong("exp", 0L)

            if (token.isEmpty() || adminEmail.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Malformed QR Code: Missing required authentication tokens."))
            }

            // 3. Check for expiration
            val currentTime = System.currentTimeMillis()
            if (expiresAt in 1 until currentTime) {
                return@withContext Result.failure(IllegalStateException("This camera share QR code has expired."))
            }

            // 4. Extract permitted camera IDs
            val camerasJsonArray: JSONArray? = rootJson.optJSONArray("cameras")
            val permittedCameraIds = mutableListOf<String>()
            if (camerasJsonArray != null) {
                for (i in 0 until camerasJsonArray.length()) {
                    permittedCameraIds.add(camerasJsonArray.getString(i))
                }
            }

            // 5. Extract and mount the House Admin's Firebase configuration with GUEST_QR origin tag
            val finalFirebaseJson: String? = when {
                // Version 2: Minified 4-key config object
                rootJson.has("fbConfig") -> {
                    val fbObj = rootJson.getJSONObject("fbConfig")
                    val projectId = fbObj.optString("p", "")
                    val apiKey = fbObj.optString("k", "")
                    val appId = fbObj.optString("a", "")
                    val storageBucket = fbObj.optString("b", "")

                    if (projectId.isNotBlank() && apiKey.isNotBlank() && appId.isNotBlank()) {
                        // Directly initialize secondary FirebaseApp with GUEST_QR origin
                        firebaseModule.initializeFromCredentials(
                            projectId = projectId,
                            apiKey = apiKey,
                            applicationId = appId,
                            storageBucket = storageBucket.ifBlank { null },
                            source = ConfigSource.GUEST_QR
                        )

                        // Reconstruct standard schema for SettingsRepository persistence
                        JSONObject().apply {
                            put("project_info", JSONObject().apply {
                                put("project_id", projectId)
                                if (storageBucket.isNotBlank()) {
                                    put("storage_bucket", storageBucket)
                                }
                            })
                            put("client", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("client_info", JSONObject().apply {
                                        put("mobilesdk_app_id", appId)
                                    })
                                    put("api_key", JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("current_key", apiKey)
                                        })
                                    })
                                })
                            })
                        }.toString()
                    } else {
                        null
                    }
                }
                // Version 1 fallback: Raw JSON string
                rootJson.has("firebaseConfig") -> {
                    val rawConfig = rootJson.optString("firebaseConfig", "")
                    if (rawConfig.isNotBlank()) {
                        firebaseModule.initializeCustomFirebase(rawConfig, ConfigSource.GUEST_QR)
                    }
                    rawConfig
                }
                else -> null
            }

            if (!finalFirebaseJson.isNullOrBlank()) {
                settingsRepository.saveCustomFirebaseJson(finalFirebaseJson)
                Log.d(tag, "Successfully mounted House Admin Firebase from QR for member session")
            }

            // 6. If the user is already signed in with Google, silently sync their profile to Admin's Firestore
            authRepository?.let { repo ->
                val activeUser = repo.currentUser.value
                if (activeUser != null && activeUser.email.isNotBlank() && activeUser.email != "guest@shared.home") {
                    repo.bridgeToAdminFirebase(
                        email = activeUser.email,
                        googleUid = activeUser.uid,
                        role = "guest",
                        displayName = activeUser.displayName
                    )
                }
            }

            // 7. Construct the validated ShareToken model
            val shareToken = ShareToken(
                token = token,
                adminUserId = adminUserId,
                adminEmail = adminEmail,
                cameraIds = permittedCameraIds,
                permission = permission,
                expiresAt = expiresAt,
                createdAt = rootJson.optLong("createdAt", currentTime)
            )

            // 8. Save current guest session
            settingsRepository.saveActiveGuestShareToken(token)

            Result.success(shareToken)
        } catch (e: Exception) {
            Log.e(tag, "Failed to process scanned QR code", e)
            Result.failure(e)
        }
    }
}