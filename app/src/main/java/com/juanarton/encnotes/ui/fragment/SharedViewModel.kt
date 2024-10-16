package com.juanarton.encnotes.ui.fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.model.Login
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val notesAppRepositoryUseCase: NotesAppRepositoryUseCase
): ViewModel(){
    private val _loginUser = MutableLiveData<Resource<Login>>()
    val loginUser = _loginUser

    fun loginUser(id: String, pin: String) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.loginUser(id, pin).collect {
                _loginUser.value = it
            }
        }
    }

    suspend fun setIsLoggedIn(isLoggedIn: Boolean): Boolean {
        return notesAppRepositoryUseCase.setIsLoggedIn(isLoggedIn).first()
    }

    suspend fun setAccessKey(accessKey: String): Boolean {
        return notesAppRepositoryUseCase.setAccessKey(accessKey).first()
    }

    suspend fun setRefreshKey(refreshKey: String): Boolean {
        return notesAppRepositoryUseCase.setRefreshKey(refreshKey).first()
    }

    suspend fun setCipherKey(cipherKey: String): Boolean {
        return notesAppRepositoryUseCase.setCipherKey(cipherKey).first()
    }
}