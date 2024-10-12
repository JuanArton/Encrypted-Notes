package com.juanarton.encnotes.core.data.api

import com.juanarton.encnotes.core.data.api.user.LoginResponse
import com.juanarton.encnotes.core.data.api.user.PostLogin
import com.juanarton.encnotes.core.data.api.user.PostRegister
import com.juanarton.encnotes.core.data.api.user.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface APIService {
    @POST("users")
    suspend fun register(
        @Body postRegister: PostRegister
    ): RegisterResponse

    @POST("authentications")
    suspend fun login(
        @Body postLogin: PostLogin
    ): LoginResponse
}