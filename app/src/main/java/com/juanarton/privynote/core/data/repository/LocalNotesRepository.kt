package com.juanarton.privynote.core.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.google.crypto.tink.KeysetHandle
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.domain.model.Attachment
import com.juanarton.privynote.core.data.domain.model.Notes
import com.juanarton.privynote.core.data.domain.repository.ILocalNotesRepository
import com.juanarton.privynote.core.data.source.local.AttachmentLocalDataSource
import com.juanarton.privynote.core.data.source.local.NoteLocalDataSource
import com.juanarton.privynote.core.data.source.local.SharedPrefDataSource
import com.juanarton.privynote.core.data.source.local.room.entity.AttachmentEntity
import com.juanarton.privynote.core.data.source.local.room.entity.NotesEntity
import com.juanarton.privynote.core.data.source.remote.Resource
import com.juanarton.privynote.core.utils.DataMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class LocalNotesRepository @Inject constructor(
    private val noteLocalDataSource: NoteLocalDataSource,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
    private val sharedPrefDataSource: SharedPrefDataSource,
    private val context: Context
): ILocalNotesRepository {
    override fun setIsLoggedIn(isLoggedIn: Boolean) = flow {
        emit(sharedPrefDataSource.setIsLoggedIn(isLoggedIn))
    }

    override fun getIsLoggedIn(): Boolean = sharedPrefDataSource.getIsLoggedIn()

    override fun getNotes(): Flow<List<Notes>> = flow {
        emit(DataMapper.mapNotesEntityToDomain(noteLocalDataSource.getNotes().asReversed()))
    }.flowOn(Dispatchers.IO)

    override fun insertNotes(notes: Notes): Flow<Resource<Boolean>> = flow {
        try {
            noteLocalDataSource.insertNotes(
                DataMapper.mapNotesDomainToEntity(notes)
            )
            emit(Resource.Success(true))
        } catch (_: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.insert_error)))
        } catch (_: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun setAccessKey(accessKey: String): Flow<Boolean> = flow {
        emit(sharedPrefDataSource.setAccessKey(accessKey))
    }

    override fun getAccessKey(): String? = sharedPrefDataSource.getAccessKey()

    override fun setRefreshKey(refreshKey: String): Flow<Boolean> = flow {
        emit(sharedPrefDataSource.setRefreshKey(refreshKey))
    }

    override fun getRefreshKey(): String? = sharedPrefDataSource.getRefreshKey()

    override fun setCipherKey(cipherKey: String): Flow<Boolean> = flow {
        emit(sharedPrefDataSource.setCipherKey(cipherKey))
    }

    override fun getCipherKey(): String? = sharedPrefDataSource.getCipherKey()

    override fun deleteNotes(notes: Notes): Flow<Resource<Boolean>> = flow {
        try {
            noteLocalDataSource.deleteNotes(
                NotesEntity(
                    notes.id,
                    "",
                    "",
                    true,
                    notes.lastModified
                )
            )
            emit(Resource.Success(true))
        } catch (_: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.delete_error)))
        } catch (_: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun getNotesById(id: String): Flow<Notes> = flow {
        emit(DataMapper.mapNoteEntityToDomain(noteLocalDataSource.getNotesById(id)))
    }.flowOn(Dispatchers.IO)

    override fun updateNotes(notes: Notes): Flow<Resource<Boolean>> = flow {
        try {
            noteLocalDataSource.updateNotes(
                DataMapper.mapNotesDomainToEntity(notes)
            )
            emit(Resource.Success(true))
        } catch (_: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.update_error)))
        } catch (_: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun permanentDeleteNotes(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            noteLocalDataSource.permanentDelete(id)
        }
    }

    override fun getAttachments(): Flow<List<Attachment>> = flow {
        emit(DataMapper.mapAttachmentsEntityToDomain(attachmentLocalDataSource.getAttachments()))
    }.flowOn(Dispatchers.IO)

    override fun getAttachmentByNoteId(id: String): Flow<List<Attachment>> = flow {
        emit(DataMapper.mapAttachmentsEntityToDomain(attachmentLocalDataSource.getAttachmentByNoteId(id)))
    }.flowOn(Dispatchers.IO)

    override fun insertAttachment(attachment: Attachment): Flow<Resource<Attachment>> = flow {
        try {
            attachmentLocalDataSource.insertAttachment(
                DataMapper.mapAttachmentDomainToEntity(attachment)
            )
            emit(Resource.Success(attachment))
        } catch (_: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.insert_error)))
        } catch (_: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteAttachment(attachment: Attachment) = flow {
        try {
            attachmentLocalDataSource.deleteAttachment(
                AttachmentEntity(
                    attachment.id,
                    attachment.noteId,
                    "",
                    true,
                    attachment.type ?: "",
                    attachment.lastModified ?: 0L
                )
            )
            emit(Resource.Success(attachment))
        } catch (_: SQLiteConstraintException) {
            emit(Resource.Error(context.getString(R.string.delete_att_error)))
        } catch (_: Exception) {
            emit(Resource.Error(context.getString(R.string.unknown_error)))
        }
    }.flowOn(Dispatchers.IO)

    override fun permanentDeleteAtt(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            attachmentLocalDataSource.permanentDeleteAtt(id)
        }
    }

    override fun writeFileToDisk(file: File, byteArray: ByteArray): Flow<Pair<Boolean, String>> =
       attachmentLocalDataSource.writeFileToDisk(file, byteArray)

    override fun deleteFileFromDisk(file: File): Boolean =
        attachmentLocalDataSource.deleteFileFromDisk(file)

    override fun backUpNotes(context: Context, keysetHandle: KeysetHandle): Flow<Boolean> =
        noteLocalDataSource.backUpNotes(context, keysetHandle)

    override fun restoreNotes(context: Context, keysetHandle: KeysetHandle, backupFile: ByteArray): Flow<Boolean> =
        noteLocalDataSource.restoreNotes(context, keysetHandle, backupFile)
}