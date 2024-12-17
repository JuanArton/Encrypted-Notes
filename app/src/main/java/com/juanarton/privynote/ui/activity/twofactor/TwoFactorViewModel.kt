package com.juanarton.privynote.ui.activity.twofactor

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
class TwoFactorViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private val _twoFactorAuth = MutableLiveData<Resource<Login>>()
    val twoFactorAuth = _twoFactorAuth

    fun twoFactorAuth(id: String, pin: String, otp: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.twoFactorAuth(id, pin, otp).collect {
                _twoFactorAuth.value = it
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