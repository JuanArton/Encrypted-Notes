package com.juanarton.privynote.core.data.domain.usecase.local

import android.content.Context
import com.google.crypto.tink.KeysetHandle
import com.juanarton.privynote.core.data.domain.model.Attachment
import com.juanarton.privynote.core.data.domain.model.Notes
import com.juanarton.privynote.core.data.domain.repository.ILocalNotesRepository
import com.juanarton.privynote.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class LocalNotesRepoImpl @Inject constructor(
    private val iLocalNotesRepository: ILocalNotesRepository
): LocalNotesRepoUseCase {
    override fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean> =
        iLocalNotesRepository.setIsLoggedIn(isLoggedIn)

    override fun getIsLoggedIn(): Boolean = iLocalNotesRepository.getIsLoggedIn()

    override fun getNotes(): Flow<List<Notes>> =
        iLocalNotesRepository.getNotes()

    override fun insertNotes(notes: Notes): Flow<Resource<Boolean>> =
        iLocalNotesRepository.insertNotes(notes)

    override fun setAccessKey(accessKey: String): Flow<Boolean> =
        iLocalNotesRepository.setAccessKey(accessKey)

    override fun getAccessKey(): String? = iLocalNotesRepository.getAccessKey()

    override fun setRefreshKey(refreshKey: String): Flow<Boolean> =
        iLocalNotesRepository.setRefreshKey(refreshKey)

    override fun getRefreshKey(): String? = iLocalNotesRepository.getRefreshKey()

    override fun setCipherKey(cipherKey: String): Flow<Boolean> =
        iLocalNotesRepository.setCipherKey(cipherKey)

    override fun getCipherKey(): String? =
        iLocalNotesRepository.getCipherKey()

    override fun deleteNotes(notes: Notes): Flow<Resource<Boolean>> =
        iLocalNotesRepository.deleteNotes(notes)

    override fun getNotesById(id: String): Flow<Notes> =
        iLocalNotesRepository.getNotesById(id)

    override fun updateNotes(notes: Notes): Flow<Resource<Boolean>> =
        iLocalNotesRepository.updateNotes(notes)

    override fun permanentDeleteNotes(id: String) =
        iLocalNotesRepository.permanentDeleteNotes(id)

    override fun getAttachments(): Flow<List<Attachment>> =
        iLocalNotesRepository.getAttachments()

    override fun getAttachmentByNoteId(id: String): Flow<List<Attachment>> =
        iLocalNotesRepository.getAttachmentByNoteId(id)

    override fun insertAttachment(attachment: Attachment): Flow<Resource<Attachment>> =
        iLocalNotesRepository.insertAttachment(attachment)

    override fun deleteAttachment(attachment: Attachment): Flow<Resource<Attachment>> =
        iLocalNotesRepository.deleteAttachment(attachment)

    override fun permanentDeleteAtt(id: String) =
        iLocalNotesRepository.permanentDeleteAtt(id)

    override fun writeFileToDisk(file: File, byteArray: ByteArray): Flow<Pair<Boolean, String>> =
        iLocalNotesRepository.writeFileToDisk(file, byteArray)

    override fun deleteFileFromDisk(file: File): Boolean =
        iLocalNotesRepository.deleteFileFromDisk(file)

    override fun backUpNotes(context: Context, keysetHandle: KeysetHandle): Flow<Boolean> =
        iLocalNotesRepository.backUpNotes(context, keysetHandle)

    override fun restoreNotes(context: Context, keysetHandle: KeysetHandle, backupFile: ByteArray): Flow<Boolean> =
        iLocalNotesRepository.restoreNotes(context, keysetHandle, backupFile)
}