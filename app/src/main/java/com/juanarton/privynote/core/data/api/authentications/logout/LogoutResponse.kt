package com.juanarton.privynote.core.data.api.authentications.logout

import com.google.gson.annotations.SerializedName

data class LogoutResponse (
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String = "No message provided",
)