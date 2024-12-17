package com.juanarton.encnotes.ui.activity.login

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.model.LoggedUser
import com.juanarton.encnotes.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private val _signInByGoogle = MutableLiveData<Resource<LoggedUser>>()
    val signInByGoogle = _signInByGoogle

    private val _loginByEmail = MutableLiveData<Resource<LoggedUser>>()
    val loginByEmail = _loginByEmail

    private val _checkRegistered: MutableLiveData<Resource<Boolean>> = MutableLiveData()
    val checkRegistered: LiveData<Resource<Boolean>> = _checkRegistered

    fun singWithGoogleAcc(option: GetSignInWithGoogleOption, activity: Activity) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.signInWithGoogle(option, activity).collect {
                _signInByGoogle.value = it
            }
        }
    }

    fun loginByEmail(email: String, password: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.logInByEmail(email, password).collect {
                _loginByEmail.value = it
            }
        }
    }

    fun checkRegistered(id: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.checkIsRegistered(id).collect {
                _checkRegistered.value = it
            }
        }
    }
}