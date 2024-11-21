package com.juanarton.encnotes.ui.activity.note

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
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
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    val localNotesRepoUseCase: LocalNotesRepoUseCase,
    val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private val _addNoteLocal: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var addNoteLocal: LiveData<Resource<Boolean>> = _addNoteLocal

    private var _updateNoteLocal: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    var updateNoteLocal: LiveData<Resource<Boolean>> = _updateNoteLocal

    private val _addAtt: MutableLiveData<Pair<Boolean, String>> = MutableLiveData()
    val addAtt: LiveData<Pair<Boolean, String>> = _addAtt

    private val _insertAtt: MutableLiveData<Resource<Attachment>> = MutableLiveData()
    val insertAtt: LiveData<Resource<Attachment>> = _insertAtt

    private val _deleteAttRemote: MutableLiveData<Resource<String>> = MutableLiveData()
    val deleteAttRemote: LiveData<Resource<String>> = _deleteAttRemote

    fun getCipherKey() = localNotesRepoUseCase.getCipherKey()

     fun insertNote(notes: Notes) {
        viewModelScope.launch {
            localNotesRepoUseCase.insertNotes(notes).collect {
                _addNoteLocal.value = it
            }
        }
    }

    fun updateNoteLocal(notes: Notes) {
        viewModelScope.launch {
            localNotesRepoUseCase.updateNotes(notes).collect {
                _updateNoteLocal.value = it
            }
        }
    }

    fun permanentDelete(id: String) = localNotesRepoUseCase.permanentDeleteNotes(id)

    fun addAtt(
        uri: Uri, contentResolver: ContentResolver, context: Context
    ) {
        val key = getCipherKey()
        if (!key.isNullOrEmpty()) {
            val deserializedKey = Cryptography.deserializeKeySet(key)

            val originalBytes = uriToByteArray(uri, contentResolver)
            val encryptedBytes = Cryptography.encrypt(originalBytes, deserializedKey)

            val fileName = getFileNameFromUri(uri, contentResolver) ?: "encrypted_file"
            val file = File(context.filesDir, fileName)

            viewModelScope.launch {
                localNotesRepoUseCase.writeFileToDisk(file, encryptedBytes).collect {
                    _addAtt.value = it
                }
            }
        }
    }

    fun insertAtt(attachment: Attachment) {
        viewModelScope.launch {
            localNotesRepoUseCase.insertAttachment(attachment).collect {
                _insertAtt.value = it
            }
        }
    }

    fun deleteAttRemote(id: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.deleteNoteRemote(id).collect {
                _deleteAttRemote.value = it
            }
        }
    }

    private fun uriToByteArray(uri: Uri, contentResolver: ContentResolver): ByteArray {
        contentResolver.openInputStream(uri).use { inputStream ->
            inputStream?.let {
                val buffer = ByteArray(8192)
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

    private fun getFileNameFromUri(uri: Uri, contentResolver: ContentResolver): String? {
        var finalName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val fileExtension = fileName?.substringAfterLast('.', missingDelimiterValue = "")
                finalName = NanoId.generate(32) + "." + fileExtension
            }
        }
        return finalName
    }
}