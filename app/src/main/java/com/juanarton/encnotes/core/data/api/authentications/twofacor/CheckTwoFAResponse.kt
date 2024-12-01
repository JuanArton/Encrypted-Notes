package com.juanarton.encnotes.core.data.api.authentications.twofacor

import com.google.gson.annotations.SerializedName

data class CheckTwoFAResponse(
    @SerializedName("status")
    val status: String?,

    @SerializedName("isEnabled")
    val isEnabled: Boolean?,

    @SerializedName("message")
    val message: String = "No message provided",
)
