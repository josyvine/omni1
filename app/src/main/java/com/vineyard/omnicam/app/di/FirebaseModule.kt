package com.vineyard.omnicam.app.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class FirebaseModule(private val context: Context) {

    var customApp: FirebaseApp? = null
        private set

    val auth: FirebaseAuth?
        get() = try {
            customApp?.let { FirebaseAuth.getInstance(it) } ?: run {
                if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance() else null
            }
        } catch (_: Exception) {
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            customApp?.let { FirebaseFirestore.getInstance(it) } ?: run {
                if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null
            }
        } catch (_: Exception) {
            null
        }

    fun initializeByoFirebase(jsonContent: String): Boolean {
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

            // Remove existing custom app if exists
            try {
                FirebaseApp.getInstance("BYO_OMNICAM")?.delete()
            } catch (_: Exception) {}

            customApp = FirebaseApp.initializeApp(context, options, "BYO_OMNICAM")
            true
        } catch (_: Exception) {
            false
        }
    }
}
