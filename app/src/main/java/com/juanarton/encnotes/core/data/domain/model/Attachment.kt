package com.juanarton.encnotes.core.data.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Attachment(
    val id: String,
    val noteId: String?,
    val url: String,
    val isDelete: Boolean?,
    val lastModified: Long?
): Parcelable
