package com.vineyard.omnicam.app.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.vineyard.omnicam.app.core.utils.NetworkScanner
import com.vineyard.omnicam.app.data.sources.DynamicBrandRemoteSource
import com.vineyard.omnicam.app.data.sources.OnvifDiscoverySource
import com.vineyard.omnicam.app.data.sources.RtspStreamSource
import com.vineyard.omnicam.app.data.sources.TuyaP2PSource
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class NetworkModule(private val context: Context, private val firestore: FirebaseFirestore?) {

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val networkScanner: NetworkScanner by lazy {
        NetworkScanner(context)
    }

    val onvifDiscoverySource: OnvifDiscoverySource by lazy {
        OnvifDiscoverySource(networkScanner, okHttpClient)
    }

    val rtspStreamSource: RtspStreamSource by lazy {
        RtspStreamSource()
    }

    val tuyaP2PSource: TuyaP2PSource by lazy {
        TuyaP2PSource()
    }

    val dynamicBrandRemoteSource: DynamicBrandRemoteSource by lazy {
        DynamicBrandRemoteSource(firestore)
    }
}
