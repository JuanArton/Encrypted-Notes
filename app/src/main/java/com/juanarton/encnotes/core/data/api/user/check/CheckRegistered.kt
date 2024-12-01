package com.juanarton.encnotes.core.data.api.user.check

import com.google.gson.annotations.SerializedName

data class CheckRegistered(
    @SerializedName("status")
    val status: String,

    @SerializedName("isRegistered")
    val isRegistered: Boolean
)
