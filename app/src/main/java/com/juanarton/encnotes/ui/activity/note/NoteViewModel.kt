package com.juanarton.encnotes.ui.activity.note

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private var _addNoteRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    var addNoteRemote: MutableLiveData<Resource<String>> = _addNoteRemote

    fun getCipherKey() = localNotesRepoUseCase.getCipherKey()

    suspend fun insertNote(notes: Notes): Resource<Boolean> {
        return localNotesRepoUseCase.insertNotes(notes).first()
    }

    fun insertNoteRemote(notes: Notes) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.insertNoteRemote(notes).collect {
                _addNoteRemote.value = it
            }
        }
    }
}