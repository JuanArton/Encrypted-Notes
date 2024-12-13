package com.juanarton.encnotes.core.data.api.authentications.login

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val loginData: LoginData?,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class LoginData(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String
)