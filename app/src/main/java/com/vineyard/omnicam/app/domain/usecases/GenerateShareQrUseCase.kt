package com.vineyard.omnicam.app.domain.usecases

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.vineyard.omnicam.app.core.security.CryptoManager
import com.vineyard.omnicam.app.data.models.ShareToken
import com.vineyard.omnicam.app.data.repository.SettingsRepository
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * UseCase to generate, encrypt, and render QR share codes for Guest / Member access.
 * 
 * Bundles:
 * 1. Random Share Token UUID.
 * 2. Admin identifier.
 * 3. Permitted camera IDs list.
 * 4. Permission level ("VIEW_ONLY" or "FULL_CONTROL_PTZ").
 * 5. Expiration timestamp.
 * 6. Minified Admin Firebase credentials (projectId, apiKey, appId, storageBucket).
 */
class GenerateShareQrUseCase @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val settingsRepository: SettingsRepository? = null
) {

    /**
     * Generates a new ShareToken domain model.
     */
    fun generateShareToken(
        adminEmail: String,
        selectedCameraIds: List<String>,
        permission: String, // "VIEW_ONLY", "FULL_CONTROL_PTZ"
        durationHours: Int // 1, 8, 24, 0 (Never)
    ): ShareToken {
        val expiresAt = if (durationHours > 0) {
            System.currentTimeMillis() + (durationHours.toLong() * 3600_000L)
        } else {
            0L
        }

        return ShareToken(
            token = java.util.UUID.randomUUID().toString(),
            adminUserId = "admin_master",
            adminEmail = adminEmail,
            cameraIds = selectedCameraIds,
            permission = permission,
            expiresAt = expiresAt
        )
    }

    /**
     * Serializes and encrypts the ShareToken along with minified Firebase credentials into an AES-256 string.
     */
    fun encodeToEncryptedPayload(
        token: ShareToken,
        customFirebaseJson: String? = null
    ): String {
        val rawConfig = customFirebaseJson 
            ?: settingsRepository?.getCustomFirebaseJson() 
            ?: ""

        val essentialConfig = extractEssentialFirebaseConfig(rawConfig)

        val json = JSONObject().apply {
            put("token", token.token)
            put("admin", token.adminEmail)
            put("cameras", JSONArray(token.cameraIds))
            put("perm", token.permission)
            put("exp", token.expiresAt)
            if (essentialConfig != null) {
                put("fbConfig", essentialConfig)
            }
            put("createdAt", token.createdAt)
            put("v", 2)
        }.toString()

        return cryptoManager.encrypt(json)
    }

    /**
     * Extracts only the 4 essential Firebase parameters to prevent QR buffer overflows:
     * - p: projectId
     * - k: apiKey
     * - a: applicationId
     * - b: storageBucket
     */
    private fun extractEssentialFirebaseConfig(rawJson: String): JSONObject? {
        if (rawJson.isBlank()) return null
        return try {
            val root = JSONObject(rawJson)
            if (root.has("project_info") && root.has("client")) {
                val projectInfo = root.getJSONObject("project_info")
                val projectId = projectInfo.getString("project_id")
                val storageBucket = projectInfo.optString("storage_bucket", "")
                val clientArray = root.getJSONArray("client")
                val firstClient = clientArray.getJSONObject(0)
                val apiKey = firstClient.getJSONArray("api_key").getJSONObject(0).getString("current_key")
                val appId = firstClient.getJSONObject("client_info").getString("mobilesdk_app_id")

                JSONObject().apply {
                    put("p", projectId)
                    put("k", apiKey)
                    put("a", appId)
                    if (storageBucket.isNotBlank()) {
                        put("b", storageBucket)
                    }
                }
            } else if (root.has("p") && root.has("k") && root.has("a")) {
                root
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decrypts and parses an incoming encrypted payload.
     */
    fun decodeEncryptedPayload(encryptedText: String): ShareToken? {
        return try {
            val decrypted = cryptoManager.decrypt(encryptedText)
            val json = JSONObject(decrypted)
            val cameraList = mutableListOf<String>()
            val array = json.optJSONArray("cameras")
            if (array != null) {
                for (i in 0 until array.length()) {
                    cameraList.add(array.getString(i))
                }
            }

            ShareToken(
                token = json.getString("token"),
                adminUserId = json.optString("adminUserId", "admin_master"),
                adminEmail = json.optString("admin", "Admin"),
                cameraIds = cameraList,
                permission = json.optString("perm", "VIEW_ONLY"),
                expiresAt = json.optLong("exp", 0L),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Renders an encrypted string into a high-contrast QR Code Bitmap.
     * Uses ErrorCorrectionLevel.M to guarantee compact byte density and fast scanning.
     */
    fun renderQrBitmap(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}