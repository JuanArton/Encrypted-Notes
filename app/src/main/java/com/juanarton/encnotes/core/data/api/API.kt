package com.juanarton.encnotes.core.data.api

import com.juanarton.encnotes.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object API {
    private const val BASE_URL = "https://itasoft.int.joget.cloud/jw/"

    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("api_key", "6a9ed2eaf0ff4274ab2370bed8ea31fc")
            .addHeader("api_id", "API-b8b98d97-008d-4b83-aa59-cb133665638b")
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient().newBuilder()
        .addInterceptor(headerInterceptor) // Add the header interceptor
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else
                HttpLoggingInterceptor.Level.NONE
        })
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    //val services: APIService = retrofit.create(APIService::class.java)
}
