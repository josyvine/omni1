package com.vineyard.omnicam.app.di

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vineyard.omnicam.app.core.constants.CentralConfig
import org.json.JSONObject

/**
 * Origin source of the dynamic secondary Firebase configuration.
 */
enum class ConfigSource {
    NONE,
    ADMIN_JSON,
    GUEST_QR
}

/**
 * Dual-Firebase Dependency Module.
 * 
 * Manages two concurrent FirebaseApp instances:
 * 1. [DEFAULT] instance: Uses CentralConfig (Developer Project) with Keystore SHA-1
 *    for Google Sign-In and OAuth 2.0 PKCE.
 * 2. "admin_cam_app" instance: Uses the User Admin's custom google-services.json
 *    or Guest-scanned QR credentials for storing private cameras, access tokens,
 *    and Drive event logs via Email/Password.
 */
class FirebaseModule(private val context: Context) {

    companion object {
        const val ADMIN_APP_NAME = "admin_cam_app"
        private const val PREFS_NAME = "omnicam_firebase_module_prefs"
        private const val KEY_CONFIG_SOURCE = "key_firebase_config_source"
    }

    private val tag = "FirebaseModule"
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var customApp: FirebaseApp? = null
        private set

    /**
     * Identifies whether the active custom configuration was established via
     * direct Admin google-services.json upload or a Guest-scanned QR code.
     */
    var configSource: ConfigSource
        get() {
            val sourceName = prefs.getString(KEY_CONFIG_SOURCE, ConfigSource.NONE.name)
            return try {
                ConfigSource.valueOf(sourceName ?: ConfigSource.NONE.name)
            } catch (_: Exception) {
                ConfigSource.NONE
            }
        }
        private set(value) {
            prefs.edit().putString(KEY_CONFIG_SOURCE, value.name).apply()
        }

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
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Central Developer FirebaseApp", e)
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
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve Central Developer FirebaseAuth", e)
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
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve secondary adminAuth instance", e)
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
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve secondary adminFirestore instance", e)
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
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve fallback Central Firestore instance", e)
            null
        }

    /**
     * Dynamically initializes the User Admin's private FirebaseApp from a JSON configuration string.
     */
    fun initializeCustomFirebase(
        jsonContent: String,
        source: ConfigSource = ConfigSource.ADMIN_JSON
    ): Boolean {
        return try {
            val root = JSONObject(jsonContent)
            val projectInfo = root.getJSONObject("project_info")
            val projectId = projectInfo.getString("project_id")
            val storageBucket = projectInfo.optString("storage_bucket")
            val clientArray = root.getJSONArray("client")
            val firstClient = clientArray.getJSONObject(0)
            val apiKey = firstClient.getJSONArray("api_key").getJSONObject(0).getString("current_key")
            val appId = firstClient.getJSONObject("client_info").getString("mobilesdk_app_id")

            initializeFromCredentials(
                projectId = projectId,
                apiKey = apiKey,
                applicationId = appId,
                storageBucket = storageBucket,
                source = source
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse and initialize custom Firebase JSON", e)
            false
        }
    }

    /**
     * Direct programmatic initialization using unpacked credentials
     * (e.g. from an encrypted House Member QR code).
     */
    fun initializeFromCredentials(
        projectId: String,
        apiKey: String,
        applicationId: String,
        storageBucket: String? = null,
        source: ConfigSource = ConfigSource.GUEST_QR
    ): Boolean {
        return try {
            val optionsBuilder = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(applicationId)
                .setApiKey(apiKey)

            if (!storageBucket.isNullOrBlank()) {
                optionsBuilder.setStorageBucket(storageBucket)
            }

            // Clean up existing custom app instance before re-initializing
            try {
                FirebaseApp.getInstance(ADMIN_APP_NAME)?.delete()
            } catch (_: Exception) {}

            customApp = FirebaseApp.initializeApp(context, optionsBuilder.build(), ADMIN_APP_NAME)
            configSource = source
            Log.d(tag, "Successfully initialized secondary Firebase ($ADMIN_APP_NAME) for project: $projectId with source: $source")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize secondary Firebase with credentials", e)
            false
        }
    }

    /**
     * Backward-compatible alias for initializeCustomFirebase.
     */
    fun initializeByoFirebase(jsonContent: String): Boolean {
        return initializeCustomFirebase(jsonContent, ConfigSource.ADMIN_JSON)
    }

    /**
     * Checks if the secondary Admin Firebase instance is currently active.
     */
    fun isCustomConfigured(): Boolean {
        return customApp != null || FirebaseApp.getApps(context).any { it.name == ADMIN_APP_NAME }
    }

    /**
     * Retrieves the project ID of the active secondary Firebase instance, if configured.
     */
    fun getActiveProjectId(): String? {
        val app = customApp ?: FirebaseApp.getApps(context).firstOrNull { it.name == ADMIN_APP_NAME }
        return app?.options?.projectId
    }

    /**
     * Clears and deletes the secondary Admin FirebaseApp instance and resets origin tracking.
     */
    fun clearCustomConfig() {
        try {
            FirebaseApp.getInstance(ADMIN_APP_NAME)?.delete()
        } catch (e: Exception) {
            Log.e(tag, "Error deleting secondary FirebaseApp instance", e)
        }
        customApp = null
        configSource = ConfigSource.NONE
    }
}