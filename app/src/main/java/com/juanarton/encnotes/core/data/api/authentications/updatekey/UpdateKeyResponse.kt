package com.juanarton.encnotes.core.data.api.authentications.updatekey

import com.google.gson.annotations.SerializedName

data class UpdateKeyResponse(
    @SerializedName("status")
    val status: String,

    @SerializedName("data")
    val updateKeyData: UpdateKeyData,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class UpdateKeyData(
    @SerializedName("accessToken")
    val accessToken: String,
)