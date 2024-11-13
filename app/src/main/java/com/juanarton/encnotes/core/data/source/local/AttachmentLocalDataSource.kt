package com.juanarton.encnotes.core.data.source.local

import com.juanarton.encnotes.core.data.source.local.room.dao.AttachmentsDAO
import com.juanarton.encnotes.core.data.source.local.room.entity.AttachmentEntity
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
}