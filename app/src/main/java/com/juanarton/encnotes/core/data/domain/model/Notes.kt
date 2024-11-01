package com.juanarton.encnotes.core.data.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notes(
    val id: String,
    val notesTitle: String?,
    val notesContent: String?,
    val isDelete: Boolean,
    val lastModified: Long,
): Parcelable
