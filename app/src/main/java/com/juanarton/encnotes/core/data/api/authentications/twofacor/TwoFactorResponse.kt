package com.juanarton.encnotes.core.data.api.authentications.twofacor

import com.google.gson.annotations.SerializedName

class TwoFactorResponse (
    @SerializedName("status")
    val status: String?,

    @SerializedName("data")
    val twoFactorData: TwoFactorData?,

    @SerializedName("message")
    val message: String = "No message provided",
)

data class TwoFactorData(
    @SerializedName("qrImage")
    val qrImage: String,

    @SerializedName("secret")
    val secret: String,
)