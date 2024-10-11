package com.juanarton.encnotes.core.utils

import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.source.local.room.entity.NotesEntity

object DataMapper {
    fun mapNotesEntityToDomain(notes: List<NotesEntity>): List<Notes> = run {
        notes.map {
            Notes(
                it.id,
                it.ownerId,
                it.notesTitle,
                it.notesContent,
                it.lastModified
            )
        }
    }

    fun mapNotesDomainToEntity(notes: Notes): NotesEntity = run {
        NotesEntity(
            notes.id,
            notes.ownerId,
            notes.notesTitle,
            notes.notesContent,
            notes.lastModified
        )
    }
}