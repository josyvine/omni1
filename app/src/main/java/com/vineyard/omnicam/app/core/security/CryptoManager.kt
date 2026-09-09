package com.vineyard.omnicam.app.core.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BIT = 128
        private const val IV_LENGTH_BYTE = 12
        // App-wide master salt for QR share payload security
        private const val MASTER_SEED = "OmniCamVision_P2P_Encrypted_Family_Share_Key_2026"
    }

    private fun deriveKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest((passphrase + MASTER_SEED).toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String, keyPhrase: String = "OmniCamAdmin"): String {
        return try {
            val key = deriveKey(keyPhrase)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            // Prepend IV to ciphertext
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP or Base64.URL_SAFE)
        } catch (e: Exception) {
            plainText // Fallback to raw if encryption not available
        }
    }

    fun decrypt(encryptedBase64: String, keyPhrase: String = "OmniCamAdmin"): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP or Base64.URL_SAFE)
            if (combined.size < IV_LENGTH_BYTE) return encryptedBase64

            val iv = ByteArray(IV_LENGTH_BYTE)
            val cipherText = ByteArray(combined.size - IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE)
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherText.size)

            val key = deriveKey(keyPhrase)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encryptedBase64
        }
    }
}
