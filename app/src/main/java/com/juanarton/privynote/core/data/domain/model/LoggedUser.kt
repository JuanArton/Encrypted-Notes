package com.juanarton.privynote.core.data.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoggedUser(
    val uid: String,
    val displayName: String?,
    val photoUrl: String?
): Parcelable
