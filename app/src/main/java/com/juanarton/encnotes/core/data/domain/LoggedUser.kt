package com.juanarton.encnotes.core.data.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoggedUser(
    val uid: String,
    val displayName: String?,
    val photoUrl: String?
): Parcelable
