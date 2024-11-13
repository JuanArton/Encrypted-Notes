package com.juanarton.encnotes.ui.activity.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.ui.utils.SyncNotes
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
    var addNoteRemote: LiveData<Resource<String>> = _addNoteRemote

    private var _updateNoteRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    var updateNoteRemote: LiveData<Resource<String>> = _updateNoteRemote

    private var _deleteNote: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var deleteNote: LiveData<Resource<Boolean>> = _deleteNote

    private var _deleteNoteRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    var deleteNoteRemote: LiveData<Resource<String>> = _deleteNoteRemote

    fun getIsLoggedIn(): Boolean = localNotesRepoUseCase.getIsLoggedIn()

    var _notDeleted: MutableLiveData<List<Notes>> = MutableLiveData()
    var notDeleted: LiveData<List<Notes>> = _notDeleted

    private fun getCipherKey() = localNotesRepoUseCase.getCipherKey()

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

    fun deleteNote(noteList: List<Notes>) {
        viewModelScope.launch {
            noteList.forEach { note ->
                localNotesRepoUseCase.deleteNotes(note).collect {
                    _deleteNote.value = it
                }
            }
        }
    }

    fun deleteNoteRemote(noteList: List<Notes>) {
        viewModelScope.launch {
            noteList.forEach { note ->
                remoteNotesRepoUseCase.deleteNoteRemote(note.id).collect {
                    _deleteNoteRemote.value = it
                }
            }
        }
    }

    fun syncToLocal(syncNotes: SyncNotes) {
        viewModelScope.launch {
            syncNotes.toDeleteInLocal.forEach { notes ->
                localNotesRepoUseCase.deleteNotes(notes).collect{}
            }

            syncNotes.toAddToLocal.forEach { notes ->
                if (!notes.isDelete) {
                    localNotesRepoUseCase.insertNotes(notes).collect{}
                }
            }

            syncNotes.toUpdateToLocal.forEach { notes ->
                localNotesRepoUseCase.updateNotes(notes).collect{}
            }
            getNotes()
        }
    }

    fun syncToRemote(syncNotes: SyncNotes) {
        viewModelScope.launch {
            syncNotes.toDeleteInServer.forEach { notes ->
                remoteNotesRepoUseCase.deleteNoteRemote(notes.id).collect{}
            }

            syncNotes.toAddToServer.forEach { notes ->
                remoteNotesRepoUseCase.insertNoteRemote(notes).collect{}
            }

            syncNotes.toUpdateToServer.forEach { notes ->
                remoteNotesRepoUseCase.updateNoteRemote(notes).collect{}
            }

            getNotes()
        }
    }

    fun decrypt() {
        val key = getCipherKey()
        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)

            _notDeleted.value = _notDeleted.value?.map {
                Notes(
                    it.id,
                    Cryptography.decrypt(it.notesTitle ?: "", deserializedKey),
                    Cryptography.decrypt(it.notesContent ?: "", deserializedKey),
                    it.isDelete,
                    it.lastModified
                )
            }
        } else {
            _notDeleted.value = emptyList()
        }
    }
}