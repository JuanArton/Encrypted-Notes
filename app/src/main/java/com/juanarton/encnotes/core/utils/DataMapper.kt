package com.juanarton.encnotes.core.utils

import com.juanarton.encnotes.core.data.api.attachments.getattachment.AttachmentData
import com.juanarton.encnotes.core.data.api.note.getallnote.NoteData
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.local.room.entity.AttachmentEntity
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity

object DataMapper {
    fun mapNotesEntityToDomain(notes: List<NotesEntity>): List<Notes> = run {
        notes.map {
            Notes(
                it.id,
                it.notesTitle,
                it.notesContent,
                it.isDelete,
                it.lastModified
            )
        }
    }

    fun mapNotesDomainToEntity(notes: Notes): NotesEntity = run {
        NotesEntity(
            notes.id,
            notes.notesTitle,
            notes.notesContent,
            notes.isDelete,
            notes.lastModified
        )
    }

    fun mapNotesRemoteToDomain(notes: List<NoteData>): List<Notes> = run {
        notes.map {
            Notes(
                it.id,
                it.title,
                it.content,
                it.isDelete,
                it.lastModified
            )
        }
    }

    fun mapNoteEntityToDomain(notesEntity: NotesEntity): Notes = run {
        Notes(
            notesEntity.id,
            notesEntity.notesTitle,
            notesEntity.notesContent,
            notesEntity.isDelete,
            notesEntity.lastModified
        )
    }

    fun mapAttachmentsRemoteToDomain(attachment: List<AttachmentData>): List<Attachment> = run {
        attachment.map {
            Attachment(
                it.id,
                it.notesId,
                it.url,
                it.isDelete,
                it.type,
                it.lastModified
            )
        }
    }

    fun mapAttachmentsEntityToDomain(attachment: List<AttachmentEntity>): List<Attachment> = run {
        attachment.map {
            Attachment(
                it.id,
                it.noteId,
                it.url ?: "",
                it.isDelete,
                it.type,
                it.lastModified
            )
        }
    }

    fun mapAttachmentDomainToEntity(attachment: Attachment): AttachmentEntity = run {
        AttachmentEntity(
            attachment.id,
            attachment.noteId,
            attachment.url,
            attachment.isDelete ?: false,
            attachment.type ?: "",
            attachment.lastModified ?: 0L
        )
    }
}