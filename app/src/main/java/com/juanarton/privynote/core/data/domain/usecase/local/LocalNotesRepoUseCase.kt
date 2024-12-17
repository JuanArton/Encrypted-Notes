package com.juanarton.privynote.core.data.domain.usecase.local

import android.content.Context
import com.google.crypto.tink.KeysetHandle
import com.juanarton.privynote.core.data.domain.model.Attachment
import com.juanarton.privynote.core.data.domain.model.Notes
import com.juanarton.privynote.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

interface LocalNotesRepoUseCase {
    fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean>

    fun getIsLoggedIn(): Boolean

    fun getNotes(): Flow<List<Notes>>

    fun insertNotes(notes: Notes): Flow<Resource<Boolean>>

    fun setAccessKey(accessKey: String): Flow<Boolean>

    fun getAccessKey(): String?

    fun setRefreshKey(refreshKey: String): Flow<Boolean>

    fun getRefreshKey(): String?

    fun setCipherKey(cipherKey: String): Flow<Boolean>

    fun getCipherKey(): String?

    fun deleteNotes(notes: Notes): Flow<Resource<Boolean>>

    fun getNotesById(id: String): Flow<Notes>

    fun updateNotes(notes: Notes): Flow<Resource<Boolean>>

    fun permanentDeleteNotes(id: String)

    fun getAttachments(): Flow<List<Attachment>>

    fun getAttachmentByNoteId(id: String): Flow<List<Attachment>>

    fun insertAttachment(attachment: Attachment): Flow<Resource<Attachment>>

    fun deleteAttachment(attachment: Attachment): Flow<Resource<Attachment>>

    fun permanentDeleteAtt(id: String)

    fun writeFileToDisk(file: File, byteArray: ByteArray): Flow<Pair<Boolean, String>>

    fun deleteFileFromDisk(file: File): Boolean

    fun backUpNotes(context: Context, keysetHandle: KeysetHandle): Flow<Boolean>

    fun restoreNotes(context: Context, keysetHandle: KeysetHandle, backupFile: ByteArray): Flow<Boolean>
}