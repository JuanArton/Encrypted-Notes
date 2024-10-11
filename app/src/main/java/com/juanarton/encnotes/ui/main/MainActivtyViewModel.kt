package com.juanarton.encnotes.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivtyViewModel @Inject constructor(
    private val notesAppRepositoryUseCase: NotesAppRepositoryUseCase
): ViewModel() {
    private var _insertNote: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var insertNote: LiveData<Resource<Boolean>> = _insertNote

    fun getNotes(): LiveData<PagingData<Notes>> {
        return notesAppRepositoryUseCase.getNotes().asLiveData().cachedIn(viewModelScope)
    }

    fun insertNote(ownerId: String, title: String, content: String) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.insertNotes(ownerId, title, content).collect {
                _insertNote.value = it
            }
        }
    }
}