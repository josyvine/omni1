package com.vineyard.omnicam.app.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import com.vineyard.omnicam.app.core.constants.ApiEndpoints
import java.security.MessageDigest
import java.security.SecureRandom

object DeepLinkHandler {

    private var currentCodeVerifier: String? = null

    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        val verifier = Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        currentCodeVerifier = verifier
        return verifier
    }

    fun getCodeVerifier(): String? = currentCodeVerifier

    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, 0, bytes.size)
        val digest = md.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun launchOAuthCustomTab(context: Context, clientId: String) {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)

        val authUri = Uri.parse(ApiEndpoints.GOOGLE_OAUTH_AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", ApiEndpoints.OAUTH_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", ApiEndpoints.GOOGLE_DRIVE_SCOPE)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        // Prevents AndroidRuntimeException when launched from ApplicationContext
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(context, authUri)
    }

    fun extractAuthCode(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == ApiEndpoints.OAUTH_REDIRECT_SCHEME && data.host == ApiEndpoints.OAUTH_REDIRECT_HOST) {
            return data.getQueryParameter("code")
        }
        return null
    }
}