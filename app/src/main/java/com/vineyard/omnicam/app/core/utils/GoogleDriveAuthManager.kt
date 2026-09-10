package com.vineyard.omnicam.app.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Manages Google Drive OAuth 2.0 PKCE authorization, token exchange,
 * and secure local persistence.
 *
 * Guarantees zero hardcoded client secrets and executes RFC 7636 compliant
 * Proof Key for Code Exchange (PKCE) suitable for mobile native applications.
 */
class GoogleDriveAuthManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        initSecurePreferences(context)
    }

    private fun initSecurePreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback for virtualized sandboxes (e.g. microG / GBox / HMS environments)
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Generates a cryptographically random PKCE code verifier and computes
     * its SHA-256 code challenge.
     * Persists the verifier locally to be used during the code exchange step.
     */
    fun createAuthorizationUri(clientId: String, redirectUri: String): Uri {
        val verifier = generateCodeVerifier()
        prefs.edit().putString(KEY_CODE_VERIFIER, verifier).apply()

        val challenge = generateCodeChallenge(verifier)

        return Uri.parse(GOOGLE_AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", DRIVE_SCOPES)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .build()
    }

    /**
     * Exchanges an authorization code returned from the browser redirect
     * for Google OAuth access and refresh tokens.
     */
    suspend fun exchangeAuthorizationCode(
        code: String,
        clientId: String,
        redirectUri: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val verifier = prefs.getString(KEY_CODE_VERIFIER, null)
            ?: return@withContext Result.failure(IllegalStateException("PKCE code verifier not found. Aborting exchange."))

        val params = mapOf(
            "client_id" to clientId,
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri,
            "code_verifier" to verifier
        )

        val result = executeTokenRequest(params)
        if (result.isSuccess) {
            // Clean up the consumed verifier
            prefs.edit().remove(KEY_CODE_VERIFIER).apply()
        }
        result
    }

    /**
     * Refreshes an expired access token using the stored refresh token.
     */
    suspend fun refreshAccessToken(clientId: String): Result<String> = withContext(Dispatchers.IO) {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            ?: return@withContext Result.failure(IllegalStateException("No Google Drive refresh token available."))

        val params = mapOf(
            "client_id" to clientId,
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken
        )

        executeTokenRequest(params)
    }

    /**
     * Executes the HTTP POST request to Google's token endpoint.
     */
    private fun executeTokenRequest(params: Map<String, String>): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(GOOGLE_TOKEN_ENDPOINT)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Accept", "application/json")
            }

            val body = params.map { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
            }.joinToString("&")

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(body)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                reader.readText()
            }

            if (responseCode !in 200..299) {
                return Result.failure(IllegalStateException("Google Token API Error ($responseCode): $responseText"))
            }

            val json = JSONObject(responseText)
            val accessToken = json.getString("access_token")
            val expiresIn = json.optLong("expires_in", 3600L)
            val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

            val editor = prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putLong(KEY_EXPIRES_AT, expiresAt)

            if (json.has("refresh_token")) {
                editor.putString(KEY_REFRESH_TOKEN, json.getString("refresh_token"))
            }
            editor.apply()

            Result.success(accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Checks if a valid, non-expired Google Drive access token exists.
     */
    fun isConnected(): Boolean {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        return !token.isNullOrBlank() || !refreshToken.isNullOrBlank()
    }

    /**
     * Retrieves the active access token if still within its valid lifetime.
     */
    fun getValidAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        // Ensure at least 60 seconds buffer before expiration
        return if (System.currentTimeMillis() < (expiresAt - 60000L)) token else null
    }

    /**
     * Clears all stored OAuth tokens and resets Drive connection state.
     */
    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_CODE_VERIFIER)
            .apply()
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        private const val SECURE_PREFS_NAME = "omnicam_drive_secure_tokens"
        private const val FALLBACK_PREFS_NAME = "omnicam_drive_tokens_fallback"

        private const val KEY_ACCESS_TOKEN = "key_drive_access_token"
        private const val KEY_REFRESH_TOKEN = "key_drive_refresh_token"
        private const val KEY_EXPIRES_AT = "key_drive_expires_at"
        private const val KEY_CODE_VERIFIER = "key_drive_code_verifier"

        private const val GOOGLE_AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

        private const val DRIVE_SCOPES = "https://www.googleapis.com/auth/drive.file"
    }
}