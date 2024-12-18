package com.juanarton.privynote.ui.activity.pin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.privynote.core.data.domain.model.Login
import com.juanarton.privynote.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.privynote.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.privynote.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private val _registerUser = MutableLiveData<Resource<String>>()
    val registerUser = _registerUser

    private val _loginUser = MutableLiveData<Resource<Login>>()
    val loginUser = _loginUser

    fun registerUser(id: String, pin: String, username: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.registerUser(id, pin, username).collect {
                _registerUser.value = it
            }
        }
    }

    fun loginUser(id: String, pin: String, otp: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.loginUser(id, pin, otp).collect {
                _loginUser.value = it
            }
        }
    }

    suspend fun setIsLoggedIn(isLoggedIn: Boolean): Boolean {
        return localNotesRepoUseCase.setIsLoggedIn(isLoggedIn).first()
    }

    suspend fun setAccessKey(accessKey: String): Boolean {
        return localNotesRepoUseCase.setAccessKey(accessKey).first()
    }

    suspend fun setRefreshKey(refreshKey: String): Boolean {
        return localNotesRepoUseCase.setRefreshKey(refreshKey).first()
    }

    suspend fun setCipherKey(cipherKey: String): Boolean {
        return localNotesRepoUseCase.setCipherKey(cipherKey).first()
    }
}