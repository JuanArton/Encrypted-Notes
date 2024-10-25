package com.juanarton.encnotes.ui.activity.login

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.encnotes.core.data.domain.LoggedUser
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val notesAppRepositoryUseCase: NotesAppRepositoryUseCase
): ViewModel() {
    private val _signInByGoogle = MutableLiveData<Resource<LoggedUser>>()
    val signInByGoogle = _signInByGoogle

    private val _signInByEmail = MutableLiveData<Resource<LoggedUser>>()
    val signInByEmail = _signInByEmail

    private val _loginByEmail = MutableLiveData<Resource<LoggedUser>>()
    val loginByEmail = _loginByEmail

    fun singWithGoogleAcc(option: GetSignInWithGoogleOption, activity: Activity) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.signInWithGoogle(option, activity).collect {
                _signInByGoogle.value = it
            }
        }
    }

    fun signInByEmail(email: String, password: String) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.signInByEmail(email, password).collect {
                _signInByEmail.value = it
            }
        }
    }

    fun loginByEmail(email: String, password: String) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.logInByEmail(email, password).collect {
                _loginByEmail.value = it
            }
        }
    }
}