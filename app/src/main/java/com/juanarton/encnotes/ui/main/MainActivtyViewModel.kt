package com.juanarton.encnotes.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivtyViewModel @Inject constructor(
    private val notesAppRepositoryUseCase: NotesAppRepositoryUseCase
): ViewModel() {
    private val _isLoggedInRes = MutableLiveData<Boolean>()
    val isLoggedInRes = _isLoggedInRes

    private val _gUIDRes = MutableLiveData<Boolean>()
    val gUIDRes = _gUIDRes

    fun getIsLoggedIn(): Boolean = notesAppRepositoryUseCase.getIsLoggedIn()

    fun setIsLoggedIn(isLoggedIn: Boolean) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.setIsLoggedIn(isLoggedIn).collect {
                _isLoggedInRes.value = it
            }
        }
    }

    fun getGUID(): String? = notesAppRepositoryUseCase.getGuid()

    fun setGUID(gUID: String) {
        viewModelScope.launch {
            notesAppRepositoryUseCase.setGUID(gUID).collect {
                _gUIDRes.value = it
            }
        }
    }
}