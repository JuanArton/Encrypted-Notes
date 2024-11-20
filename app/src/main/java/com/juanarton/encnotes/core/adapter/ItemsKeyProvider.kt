package com.juanarton.encnotes.core.adapter

import androidx.recyclerview.selection.ItemKeyProvider


class ItemsKeyProvider(private val notesAdapter: NotesAdapter): ItemKeyProvider<String>(SCOPE_CACHED) {
    override fun getKey(position: Int): String {
        return notesAdapter.noteList[position].notes.id
    }

    override fun getPosition(key: String): Int {
        return notesAdapter.noteList.indexOfFirst { it.notes.id == key }
    }
}