package com.juanarton.encnotes.ui.activity.note

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val notesAppRepositoryUseCase: NotesAppRepositoryUseCase
): ViewModel() {
    suspend fun insertNote(notes: Notes): Resource<Boolean> {
        return notesAppRepositoryUseCase.insertNotes(notes).first()
    }
}