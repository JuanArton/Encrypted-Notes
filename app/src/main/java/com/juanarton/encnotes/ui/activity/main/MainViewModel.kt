package com.juanarton.encnotes.ui.activity.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.ui.utils.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private var _getNotes: MutableLiveData<List<Notes>> = MutableLiveData()
    var getNotes: LiveData<List<Notes>> = _getNotes

    private var _getNotesRemote: MutableLiveData<Resource<List<Notes>>> = MutableLiveData()
    var getNotesRemote: LiveData<Resource<List<Notes>>> = _getNotesRemote

    private var _addNoteRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    var addNoteRemote: MutableLiveData<Resource<String>> = _addNoteRemote

    private var _updateNoteRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    var updateNoteRemote: MutableLiveData<Resource<String>> = _updateNoteRemote

    fun getIsLoggedIn(): Boolean = localNotesRepoUseCase.getIsLoggedIn()

    fun getNotes() {
        viewModelScope.launch {
            localNotesRepoUseCase.getNotes().collect {
                _getNotes.value = it
            }
        }
    }

    fun getNotesRemote() {
        viewModelScope.launch {
            remoteNotesRepoUseCase.getAllNoteRemote().collect {
                _getNotesRemote.value = it
            }
        }
    }

    fun insertNoteRemote(notes: Notes) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.insertNoteRemote(notes).collect {
                _addNoteRemote.value = it
            }
        }
    }

    fun updateNoteRemote(notes: Notes) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.updateNoteRemote(notes).collect {
                _updateNoteRemote.value = it
            }
        }
    }

    fun syncToLocal(syncResult: SyncResult) {
        viewModelScope.launch {
            syncResult.toDeleteInLocal.forEach { notes ->
                localNotesRepoUseCase.deleteNotes(notes).collect{}
            }

            syncResult.toAddToLocal.forEach { notes ->
                if (!notes.isDelete) {
                    localNotesRepoUseCase.insertNotes(notes).collect{}
                }
            }

            syncResult.toUpdateToLocal.forEach { notes ->
                localNotesRepoUseCase.updateNotes(notes).collect{}
            }
            getNotes()
        }
    }

    fun syncToRemote(syncResult: SyncResult) {
        viewModelScope.launch {
            syncResult.toDeleteInServer.forEach { notes ->
                remoteNotesRepoUseCase.deleteNoteRemote(notes.id).collect{}
            }

            syncResult.toAddToServer.forEach { notes ->
                remoteNotesRepoUseCase.insertNoteRemote(notes).collect{}
            }

            syncResult.toUpdateToServer.forEach { notes ->
                remoteNotesRepoUseCase.updateNoteRemote(notes).collect{}
            }
            getNotes()
        }
    }
}