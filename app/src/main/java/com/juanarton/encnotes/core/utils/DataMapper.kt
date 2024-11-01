package com.juanarton.encnotes.core.utils

import com.juanarton.encnotes.core.data.api.note.getallnote.NoteData
import com.juanarton.encnotes.core.data.domain.model.Notes
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
}