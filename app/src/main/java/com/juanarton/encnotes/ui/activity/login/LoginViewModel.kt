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
    private val _loggedUser = MutableLiveData<Resource<LoggedUser>>()
    val loggedUser = _loggedUser

    fun singWithGoogleAcc(option: GetSignInWithGoogleOption, activity: Activity) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.signInWithGoogle(option, activity).collect {
                _loggedUser.value = it
            }
        }
    }
}