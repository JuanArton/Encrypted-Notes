package com.juanarton.encnotes.core.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanarton.encnotes.core.data.source.local.room.entity.AttachmentEntity

@Dao
interface AttachmentsDAO {
    @Query("SELECT * FROM attachments")
    fun getAttachments(): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE notes_id = :id")
    fun getAttachmentsByNoteId(id: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT, entity = AttachmentEntity::class)
    fun insertAttachment(attachmentEntity: AttachmentEntity)

    @Update
    fun deleteAttachment(attachmentEntity: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    fun permanentDeleteAtt(id: String)
}