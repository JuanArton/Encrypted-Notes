package com.juanarton.encnotes.core.data.api

import com.juanarton.encnotes.core.data.api.user.PostLogin
import com.juanarton.encnotes.core.data.api.user.PostRegister
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST("users")
    suspend fun register(
        @Body postRegister: PostRegister
    ): Response<ResponseBody>

    @POST("authentications")
    suspend fun login(
        @Body postLogin: PostLogin
    ): Response<ResponseBody>
}