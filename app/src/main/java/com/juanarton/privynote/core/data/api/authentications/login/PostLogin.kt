package com.juanarton.privynote.core.data.api.authentications.login

data class PostLogin(
    val id: String,
    val pin: String,
    val otp: String
)
