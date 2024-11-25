package com.juanarton.encnotes.ui.activity.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.model.NotesPair
import com.juanarton.encnotes.core.data.domain.model.NotesPairRaw
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import com.juanarton.encnotes.core.utils.SyncAttachment
import com.juanarton.encnotes.core.utils.SyncNotes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val localNotesRepoUseCase: LocalNotesRepoUseCase,
    val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    var _getNotesPair: MutableLiveData<NotesPairRaw> = MutableLiveData()
    var getNotesPair: LiveData<NotesPairRaw> = _getNotesPair

    private var _getNoteById: MutableLiveData<NotesPair> = MutableLiveData()
    var getNoteById: LiveData<NotesPair> = _getNoteById

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

    var _notDeleted: MutableLiveData<List<NotesPair>> = MutableLiveData()
    var notDeleted: LiveData<List<NotesPair>> = _notDeleted

    private var _getAttachmentsRemote: MutableLiveData<Resource<List<Attachment>>> = MutableLiveData()
    var getAttachmentRemote: LiveData<Resource<List<Attachment>>> = _getAttachmentsRemote

    private var _uploadAttachment: MutableLiveData<Resource<Attachment>> = MutableLiveData()
    var uploadAttachment: LiveData<Resource<Attachment>> = _uploadAttachment

    private val _deleteAttRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteAttRemote: LiveData<Resource<String>> = _deleteAttRemote

    private val _deleteAttFromDisk: MutableLiveData<Boolean> = MutableLiveData()
    val deleteAttFromDisk: LiveData<Boolean> = _deleteAttFromDisk

    private val _deleteAtt: MutableLiveData<Resource<Attachment>> = MutableLiveData()
    val deleteAtt: LiveData<Resource<Attachment>> = _deleteAtt

    fun getCipherKey() = localNotesRepoUseCase.getCipherKey()

    fun getNotes() {
        viewModelScope.launch {
            val attachment: MutableLiveData<List<Attachment>> = MutableLiveData()
            val notes: MutableLiveData<List<Notes>> = MutableLiveData()

            attachment.value = localNotesRepoUseCase.getAttachments().first()
            notes.value = localNotesRepoUseCase.getNotes().first()

            _getNotesPair.value = NotesPairRaw(
                notes.value ?: emptyList(), attachment.value ?: emptyList()
            )
        }
    }

    fun getNotesBydId(id: String) {
        viewModelScope.launch {
            val attachment: MutableLiveData<List<Attachment>> = MutableLiveData()
            val notes: MutableLiveData<Notes> = MutableLiveData()

            notes.value = localNotesRepoUseCase.getNotesById(id).first()
            attachment.value = localNotesRepoUseCase.getAttachmentByNoteId(id).first()

            _getNoteById.value = NotesPair(notes.value!!, attachment.value as ArrayList<Attachment>)
        }
    }

    fun getNotesRemote() {
        viewModelScope.launch {
            getAttachmentRemote()
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

    fun deleteNote(note: Notes) {
        viewModelScope.launch {
            localNotesRepoUseCase.deleteNotes(note).collect {
                _deleteNote.value = it
            }
        }
    }

    fun deleteNoteRemote(note: Notes) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.deleteNoteRemote(note.id).collect {
                _deleteNoteRemote.value = it
            }
        }
    }

    private fun getAttachmentRemote() {
        viewModelScope.launch {
            remoteNotesRepoUseCase.getAllAttRemote().collect {
                _getAttachmentsRemote.value = it
            }
        }
    }

    fun uploadAttachment(context: Context, attachments: List<Attachment>) {
        attachments.forEach { attachment ->
            val file = File(context.filesDir, attachment.url)
            val byteArray = file.inputStream().use { inputStream ->
                BufferedInputStream(inputStream).readBytes()
            }
            viewModelScope.launch {
                remoteNotesRepoUseCase.uploadImageAtt(byteArray, attachment).collect{
                    _uploadAttachment.value = it
                }
            }
        }
    }

    fun deleteAttRemote(attachments: List<Attachment>) {
        viewModelScope.launch {
            attachments.forEach {
                remoteNotesRepoUseCase.deleteAttById(it.id).collect {
                    _deleteAttRemote.value = it
                }
            }
        }
    }

    fun deleteAttFromDisk(attachment: Attachment, context: Context) {
        val file = File(context.filesDir, attachment.url)
        viewModelScope.launch {
            _deleteAttFromDisk.value = localNotesRepoUseCase.deleteFileFromDisk(file)
        }
    }

    fun deleteAtt(attachments: List<Attachment>) {
        viewModelScope.launch {
            attachments.forEach {
                localNotesRepoUseCase.deleteAttachment(it).collect {
                    _deleteAtt.value = it
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
                localNotesRepoUseCase.insertNotes(notes).collect{}
            }

            syncNotes.toUpdateToLocal.forEach { notes ->
                localNotesRepoUseCase.updateNotes(notes).collect{}
            }

            if(
                syncNotes.toAddToLocal.isNotEmpty() || syncNotes.toUpdateToLocal.isNotEmpty() ||
                syncNotes.toDeleteInLocal.isNotEmpty()
            ) {
                getNotes()
            }
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
        }
    }

    fun syncAttToLocal(syncAttachment: SyncAttachment, context: Context) {
        viewModelScope.launch {
            syncAttachment.toDeleteInLocal.forEach { attachment ->
                val file = File(context.filesDir, attachment.url)
                localNotesRepoUseCase.deleteAttachment(attachment).collect{}
                localNotesRepoUseCase.deleteFileFromDisk(file)
            }

            syncAttachment.toAddToLocal.forEach { attachment ->
                localNotesRepoUseCase.insertAttachment(attachment).collect{}
            }
            if (syncAttachment.toDeleteInLocal.isNotEmpty() || syncAttachment.toAddToLocal.isNotEmpty()) {
                getNotes()
            }
        }
    }

    fun syncAttToRemote(syncAttachment: SyncAttachment, context: Context) {
        viewModelScope.launch {
            syncAttachment.toDeleteInServer.forEach { attachment ->
                remoteNotesRepoUseCase.deleteAttById(attachment.id).collect{}
            }

            syncAttachment.toAddToServer.forEach { attachment ->
                val file = File(context.filesDir, attachment.url)
                val byteArray = file.inputStream().use { inputStream ->
                    BufferedInputStream(inputStream).readBytes()
                }
                remoteNotesRepoUseCase.uploadImageAtt(byteArray, attachment).collect{
                    _uploadAttachment.value = it
                }
            }
        }
    }

    fun decrypt() {
        val key = getCipherKey()
        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)

            _notDeleted.value = _notDeleted.value?.map {
                val note = Notes(
                    it.notes.id,
                    Cryptography.decrypt(it.notes.notesTitle ?: "", deserializedKey),
                    Cryptography.decrypt(it.notes.notesContent ?: "", deserializedKey),
                    it.notes.isDelete,
                    it.notes.lastModified
                )

                NotesPair(note, it.attachmentList)
            }
        } else {
            _notDeleted.value = emptyList()
        }
    }

    fun decrypt(notes: Notes): Notes {
        val key = getCipherKey()
        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)

            return Notes(
                notes.id,
                Cryptography.decrypt(notes.notesTitle ?: "", deserializedKey),
                Cryptography.decrypt(notes.notesContent ?: "", deserializedKey),
                notes.isDelete,
                notes.lastModified
            )
        } else {
            return notes
        }
    }
}