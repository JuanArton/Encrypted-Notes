package com.juanarton.privynote.core.data.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TwoFactor(
    val qrImage: String,
    val secret: String,
): Parcelable
