package com.juanarton.encnotes.core.data.api

import com.juanarton.encnotes.BuildConfig
import com.juanarton.encnotes.ui.activity.main.MainActivity.Companion.baseUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object API {
    private val client = OkHttpClient().newBuilder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else
                HttpLoggingInterceptor.Level.NONE
        })
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val services: APIService = retrofit.create(APIService::class.java)
}
