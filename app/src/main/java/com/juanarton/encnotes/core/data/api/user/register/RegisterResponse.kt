package com.juanarton.encnotes.core.data.api.user.register

import com.google.gson.annotations.SerializedName

data class RegisterResponse (
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val registerData: RegisterData,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class RegisterData(
    @SerializedName("userId")
    val userId: String
)