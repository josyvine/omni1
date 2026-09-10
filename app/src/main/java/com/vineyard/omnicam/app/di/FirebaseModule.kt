package com.vineyard.omnicam.app.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vineyard.omnicam.app.core.constants.CentralConfig
import org.json.JSONObject

/**
 * Dual-Firebase Dependency Module.
 * 
 * Manages two concurrent FirebaseApp instances:
 * 1. [DEFAULT] instance: Uses CentralConfig (Developer Project) with Keystore SHA-1
 *    for Google Sign-In and OAuth 2.0 PKCE.
 * 2. "admin_cam_app" instance: Uses the User Admin's custom google-services.json
 *    for storing private cameras, access tokens, and Drive event logs via Email/Password.
 */
class FirebaseModule(private val context: Context) {

    companion object {
        const val ADMIN_APP_NAME = "admin_cam_app"
    }

    var customApp: FirebaseApp? = null
        private set

    init {
        ensureCentralInitialized()
    }

    /**
     * Ensures the permanent [DEFAULT] FirebaseApp is initialized with CentralConfig.
     */
    fun ensureCentralInitialized(): FirebaseApp? {
        return try {
            val apps = FirebaseApp.getApps(context)
            val defaultApp = apps.firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            defaultApp ?: run {
                val centralOptions = CentralConfig.toFirebaseOptions()
                FirebaseApp.initializeApp(context, centralOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Central Developer FirebaseAuth instance (for Google Sign-In).
     */
    val centralAuth: FirebaseAuth?
        get() = try {
            val defaultApp = ensureCentralInitialized()
            defaultApp?.let { FirebaseAuth.getInstance(it) }
        } catch (_: Exception) {
            null
        }

    /**
     * User Admin's private FirebaseAuth instance (operates with Email & Password).
     */
    val adminAuth: FirebaseAuth?
        get() = try {
            customApp?.let { FirebaseAuth.getInstance(it) } ?: run {
                val existingApp = FirebaseApp.getApps(context).firstOrNull { it.name == ADMIN_APP_NAME }
                existingApp?.let {
                    customApp = it
                    FirebaseAuth.getInstance(it)
                }
            }
        } catch (_: Exception) {
            null
        }

    /**
     * User Admin's private Cloud Firestore instance (for cameras, shares, and event logs).
     */
    val adminFirestore: FirebaseFirestore?
        get() = try {
            customApp?.let { FirebaseFirestore.getInstance(it) } ?: run {
                val existingApp = FirebaseApp.getApps(context).firstOrNull { it.name == ADMIN_APP_NAME }
                existingApp?.let {
                    customApp = it
                    FirebaseFirestore.getInstance(it)
                }
            }
        } catch (_: Exception) {
            null
        }

    /**
     * Primary auth accessor (defaults to centralAuth for Google Sign-In).
     */
    val auth: FirebaseAuth?
        get() = centralAuth

    /**
     * Primary firestore accessor (prefers adminFirestore if configured).
     */
    val firestore: FirebaseFirestore?
        get() = adminFirestore ?: try {
            val defaultApp = ensureCentralInitialized()
            defaultApp?.let { FirebaseFirestore.getInstance(it) }
        } catch (_: Exception) {
            null
        }

    /**
     * Dynamically initializes the User Admin's private FirebaseApp from a JSON configuration string.
     */
    fun initializeCustomFirebase(jsonContent: String): Boolean {
        return try {
            val root = JSONObject(jsonContent)
            val projectInfo = root.getJSONObject("project_info")
            val projectId = projectInfo.getString("project_id")
            val storageBucket = projectInfo.optString("storage_bucket")
            val clientArray = root.getJSONArray("client")
            val firstClient = clientArray.getJSONObject(0)
            val apiKey = firstClient.getJSONArray("api_key").getJSONObject(0).getString("current_key")
            val appId = firstClient.getJSONObject("client_info").getString("mobilesdk_app_id")

            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(appId)
                .setApiKey(apiKey)
                .apply {
                    if (storageBucket.isNotBlank()) setStorageBucket(storageBucket)
                }
                .build()

            // Remove existing custom app instance if one already exists
            try {
                FirebaseApp.getInstance(ADMIN_APP_NAME)?.delete()
            } catch (_: Exception) {}

            customApp = FirebaseApp.initializeApp(context, options, ADMIN_APP_NAME)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Backward-compatible alias for initializeCustomFirebase.
     */
    fun initializeByoFirebase(jsonContent: String): Boolean {
        return initializeCustomFirebase(jsonContent)
    }

    /**
     * Checks if the secondary Admin Firebase instance is currently active.
     */
    fun isCustomConfigured(): Boolean {
        return customApp != null || FirebaseApp.getApps(context).any { it.name == ADMIN_APP_NAME }
    }

    /**
     * Clears and deletes the secondary Admin FirebaseApp instance.
     */
    fun clearCustomConfig() {
        try {
            FirebaseApp.getInstance(ADMIN_APP_NAME)?.delete()
        } catch (_: Exception) {}
        customApp = null
    }
}