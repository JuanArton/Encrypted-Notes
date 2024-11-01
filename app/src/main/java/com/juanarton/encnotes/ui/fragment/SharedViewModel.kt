package com.juanarton.encnotes.ui.fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase
): ViewModel(){
    private val _loginUser = MutableLiveData<Resource<Login>>()
    val loginUser = _loginUser

    fun loginUser(id: String, pin: String) {
        viewModelScope.launch {
            localNotesRepoUseCase.loginUser(id, pin).collect {
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