package com.juanarton.encnotes.ui.activity.imagedetail

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Attachment
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    val localNotesRepoUseCase: LocalNotesRepoUseCase,
    val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private val _deleteAttFromDisk: MutableLiveData<Boolean> = MutableLiveData()
    val deleteAttFromDisk: LiveData<Boolean> = _deleteAttFromDisk

    private val _deleteAtt: MutableLiveData<Resource<Attachment>> = MutableLiveData()
    val deleteAtt: LiveData<Resource<Attachment>> = _deleteAtt

    fun deleteAttFromDisk(attachment: Attachment, context: Context) {
        val file = File(context.filesDir, attachment.url)
        viewModelScope.launch {
            _deleteAttFromDisk.value = localNotesRepoUseCase.deleteFileFromDisk(file)
        }
    }

    fun deleteAtt(attachment: Attachment) {
        viewModelScope.launch {
            localNotesRepoUseCase.deleteAttachment(attachment).collect {
                _deleteAtt.value = it
            }
        }
    }
}