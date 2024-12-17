package com.juanarton.privynote.core.data.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotesPair (
    var notes: Notes,
    var attachmentList: MutableList<Attachment>
): Parcelable