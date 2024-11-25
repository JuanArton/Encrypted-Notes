package com.juanarton.encnotes.core.data.source.local

import com.juanarton.encnotes.core.data.source.local.room.dao.AttachmentsDAO
import com.juanarton.encnotes.core.data.source.local.room.entity.AttachmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentLocalDataSource @Inject constructor(
    private val attachmentsDAO: AttachmentsDAO,
) {
    fun getAttachments(): List<AttachmentEntity> {
        return attachmentsDAO.getAttachments()
    }

    fun getAttachmentByNoteId(id: String): List<AttachmentEntity> {
        return attachmentsDAO.getAttachmentsByNoteId(id)
    }

    fun insertAttachment(attachmentEntity: AttachmentEntity) {
        attachmentsDAO.insertAttachment(attachmentEntity)
    }

    fun deleteAttachment(attachmentEntity: AttachmentEntity) {
        attachmentsDAO.deleteAttachment(attachmentEntity)
    }

    fun permanentDeleteAtt(id: String) {
        attachmentsDAO.permanentDeleteAtt(id)
    }

    fun writeFileToDisk(file: File, byteArray: ByteArray): Flow<Pair<Boolean, String>> = flow {
        try {
            file.outputStream().use { it.write(byteArray) }
            emit(Pair(true, file.name))
        } catch (e: IOException) {
            emit(Pair(false, ""))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteFileFromDisk(file: File): Boolean  {
        try {
            val isDeleted = file.delete()
            return if (isDeleted) {
                true
            } else {
                false
            }
        } catch (e: Exception) {
            return false
        }
    }
}