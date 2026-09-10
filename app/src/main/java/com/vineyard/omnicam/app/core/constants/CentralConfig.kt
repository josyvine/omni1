package com.vineyard.omnicam.app.core.constants

import com.google.firebase.FirebaseOptions

/**
 * Central Developer Firebase Configuration.
 * 
 * This configuration is permanently bound to the [DEFAULT] FirebaseApp instance.
 * It is owned by the app developer and registered with the developer Keystore SHA-1/SHA-256
 * in the Google Cloud Console to allow 1-tap Google Sign-In and OAuth 2.0 PKCE 
 * token negotiation for Google Drive API.
 * 
 * Strict Firestore Security Rules apply to this project: all reads and writes are denied.
 * It serves solely as the Master Identity Provider (IdP).
 */
object CentralConfig {

    // Web Client ID (Type 3 OAuth Client ID from google-services.json)
    // Used for Google Sign-In ID Token requests and PKCE auth code exchanges.
    const val WEB_CLIENT_ID: String = "248540631596-lnma6a7vsnto7efthdhdd216usvqlaqu.apps.googleusercontent.com"

    // Developer Central Firebase API Key
    const val API_KEY: String = "AIzaSyCq89ULCTPjuR5-VfvFAUKK0f_bZJ6rCCs"

    // Central Firebase Project ID
    const val PROJECT_ID: String = "omnicam-93996"

    // Android Mobile Application ID
    const val APPLICATION_ID: String = "1:248540631596:android:c969a5e57600bf590ef789"

    // Default Storage Bucket
    const val STORAGE_BUCKET: String = "omnicam-93996.firebasestorage.app"

    // OAuth 2.0 PKCE Redirect Configuration
    const val OAUTH_REDIRECT_SCHEME: String = "com.vineyard.omnicam.app"
    const val OAUTH_REDIRECT_HOST: String = "oauth2redirect"
    const val OAUTH_REDIRECT_URI: String = "$OAUTH_REDIRECT_SCHEME://$OAUTH_REDIRECT_HOST"

    // Required OAuth Scopes
    const val SCOPE_DRIVE_FILE: String = "https://www.googleapis.com/auth/drive.file"
    const val SCOPE_EMAIL: String = "email"
    const val SCOPE_PROFILE: String = "profile"

    /**
     * Builds and returns the programmatic FirebaseOptions instance
     * used to initialize the permanent [DEFAULT] FirebaseApp instance.
     */
    fun toFirebaseOptions(): FirebaseOptions {
        return FirebaseOptions.Builder()
            .setApiKey(API_KEY)
            .setApplicationId(APPLICATION_ID)
            .setProjectId(PROJECT_ID)
            .setStorageBucket(STORAGE_BUCKET)
            .build()
    }
}