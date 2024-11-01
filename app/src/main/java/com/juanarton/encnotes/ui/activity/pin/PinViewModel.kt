package com.juanarton.encnotes.ui.activity.pin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinViewModel @Inject constructor(
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private val _registerUser = MutableLiveData<Resource<String>>()
    val registerUser = _registerUser

    fun registerUser(id: String, pin: String, username: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.registerUser(id, pin, username).collect {
                _registerUser.value = it
            }
        }
    }
}