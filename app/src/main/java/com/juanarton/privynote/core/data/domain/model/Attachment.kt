package com.juanarton.privynote.core.data.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Attachment(
    val id: String,
    val noteId: String?,
    val url: String,
    val isDelete: Boolean?,
    val type: String?,
    val lastModified: Long?
): Parcelable
