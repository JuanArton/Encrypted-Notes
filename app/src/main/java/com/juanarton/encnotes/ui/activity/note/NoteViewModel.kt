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
    private var _addNoteLocal: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var addNoteLocal: MutableLiveData<Resource<Boolean>> = _addNoteLocal

    private var _addNoteRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    var addNoteRemote: MutableLiveData<Resource<String>> = _addNoteRemote

    private var _updateNoteLocal: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var updateNoteLocal: MutableLiveData<Resource<Boolean>> = _updateNoteLocal

    fun getCipherKey() = localNotesRepoUseCase.getCipherKey()

     fun insertNote(notes: Notes) {
        viewModelScope.launch {
            localNotesRepoUseCase.insertNotes(notes).collect() {
                _addNoteLocal.value = it
            }
        }
    }

    suspend fun insertNoteRemote(notes: Notes) {
        remoteNotesRepoUseCase.insertNoteRemote(notes).first()
    }

    suspend fun updateNoteRemote(notes: Notes) {
        remoteNotesRepoUseCase.updateNoteRemote(notes).first()
    }

    fun updateNoteLocal(notes: Notes) {
        viewModelScope.launch {
            localNotesRepoUseCase.updateNotes(notes).collect() {
                _updateNoteLocal.value = it
            }
        }
    }

    fun permanentDelete(id: String) = localNotesRepoUseCase.permanentDeleteNotes(id)
}