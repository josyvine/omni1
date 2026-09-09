package com.vineyard.omnicam.app.domain.usecases

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.vineyard.omnicam.app.core.security.CryptoManager
import com.vineyard.omnicam.app.data.models.ShareToken
import org.json.JSONArray
import org.json.JSONObject

class GenerateShareQrUseCase(
    private val cryptoManager: CryptoManager
) {

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

    fun encodeToEncryptedPayload(token: ShareToken): String {
        val json = JSONObject().apply {
            put("token", token.token)
            put("admin", token.adminEmail)
            put("cameras", JSONArray(token.cameraIds))
            put("perm", token.permission)
            put("exp", token.expiresAt)
            put("v", 1)
        }.toString()

        return cryptoManager.encrypt(json)
    }

    fun decodeEncryptedPayload(encryptedText: String): ShareToken? {
        return try {
            val decrypted = cryptoManager.decrypt(encryptedText)
            val json = JSONObject(decrypted)
            val cameraList = mutableListOf<String>()
            val array = json.getJSONArray("cameras")
            for (i in 0 until array.length()) {
                cameraList.add(array.getString(i))
            }

            ShareToken(
                token = json.getString("token"),
                adminEmail = json.optString("admin", "Admin"),
                cameraIds = cameraList,
                permission = json.optString("perm", "VIEW_ONLY"),
                expiresAt = json.optLong("exp", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun renderQrBitmap(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
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
