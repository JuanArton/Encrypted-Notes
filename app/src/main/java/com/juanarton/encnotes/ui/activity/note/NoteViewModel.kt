package com.juanarton.encnotes.ui.activity.note

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val notesAppRepositoryUseCase: NotesAppRepositoryUseCase
): ViewModel() {
    private val _insertNote = MutableLiveData<Resource<Boolean>>()
    val insertNote = _insertNote

    fun insertNote(ownerId: String, title: String, content: String) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.insertNotes(ownerId, title, content).collect {
                _insertNote.value = it
            }
        }
    }


}