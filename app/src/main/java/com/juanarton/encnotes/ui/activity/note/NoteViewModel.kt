package com.juanarton.encnotes.ui.activity.note

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import com.juanarton.encnotes.core.utils.Cryptography
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private var _addNoteLocal: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var addNoteLocal: LiveData<Resource<Boolean>> = _addNoteLocal

    private var _addNoteRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    var addNoteRemote: LiveData<Resource<String>> = _addNoteRemote

    private var _updateNoteLocal: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var updateNoteLocal: LiveData<Resource<Boolean>> = _updateNoteLocal

    private var _addImgAtt: MutableLiveData<Resource<Attachment>> = MutableLiveData()
    var addImgAtt: LiveData<Resource<Attachment>> = _addImgAtt

    private var _getAttRemote: MutableLiveData<Resource<List<Attachment>>> = MutableLiveData()
    var getAttRemote: LiveData<Resource<List<Attachment>>> = _getAttRemote

    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> get() = _downloadProgress

    private val _imageBitmap = MutableLiveData<Bitmap>()
    val imageBitmap: LiveData<Bitmap> get() = _imageBitmap

    fun getCipherKey() = localNotesRepoUseCase.getCipherKey()

     fun insertNote(notes: Notes) {
        viewModelScope.launch {
            localNotesRepoUseCase.insertNotes(notes).collect {
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
            localNotesRepoUseCase.updateNotes(notes).collect {
                _updateNoteLocal.value = it
            }
        }
    }

    fun permanentDelete(id: String) = localNotesRepoUseCase.permanentDeleteNotes(id)

    fun addImgAddRemote(uri: Uri, notes: Notes, contentResolver: ContentResolver) {
        val key = getCipherKey()
        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)

            val originalBytes = uriToByteArray(uri, contentResolver)
            val encryptedBytes = Cryptography.encrypt(originalBytes, deserializedKey)

            viewModelScope.launch {
                remoteNotesRepoUseCase.uploadImageAtt(encryptedBytes, notes).collect {
                    _addImgAtt.value = it
                }
            }
        }
    }
    fun getAttRemote(id: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.getAttachmentRemote(id).collect {
                _getAttRemote.value = it
            }
        }
    }

    private fun uriToByteArray(uri: Uri, contentResolver: ContentResolver): ByteArray {
        contentResolver.openInputStream(uri).use { inputStream ->
            inputStream?.let {
                val buffer = ByteArray(8192) // 8KB buffer size
                val byteArrayOutputStream = ByteArrayOutputStream()
                var bytesRead: Int
                val bufferedInputStream = BufferedInputStream(it)

                while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }

                return byteArrayOutputStream.toByteArray()
            } ?: throw IllegalArgumentException("Unable to open URI: $uri")
        }
    }
}