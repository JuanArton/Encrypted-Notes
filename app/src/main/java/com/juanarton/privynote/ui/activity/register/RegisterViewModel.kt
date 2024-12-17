package com.juanarton.privynote.ui.activity.register

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.juanarton.privynote.core.data.domain.model.LoggedUser
import com.juanarton.privynote.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import com.juanarton.privynote.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val remoteNotesRepoUseCase: RemoteNotesRepoUseCase
): ViewModel() {
    private val _signInByGoogle = MutableLiveData<Resource<LoggedUser>>()
    val signInByGoogle = _signInByGoogle

    private val _signInByEmail = MutableLiveData<Resource<LoggedUser>>()
    val signInByEmail = _signInByEmail

    fun singWithGoogleAcc(option: GetSignInWithGoogleOption, activity: Activity) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.signInWithGoogle(option, activity).collect {
                _signInByGoogle.value = it
            }
        }
    }

    fun signInByEmail(email: String, password: String) {
        viewModelScope.launch {
            remoteNotesRepoUseCase.signInByEmail(email, password).collect {
                _signInByEmail.value = it
            }
        }
    }
}