package com.juanarton.encnotes.core.data.api

import com.juanarton.encnotes.BuildConfig
import com.juanarton.encnotes.core.data.api.download.ProgressResponseBody
import com.juanarton.encnotes.ui.activity.main.MainActivity.Companion.baseUrl
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object API {
    external fun getSha256Pin(): String
    external fun getNakedHost(): String

    init {
        System.loadLibrary("native-lib")
    }

    private val certificate = CertificatePinner.Builder()
        .add(getNakedHost(), getSha256Pin())
        .build()

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .certificatePinner(certificate)
        .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body, progressListener))
                .build()
        }
        .cache(null)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val services: APIService = retrofit.create(APIService::class.java)

    var progressListener: ProgressListener = object : ProgressListener {
        override fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
        }
    }
}
