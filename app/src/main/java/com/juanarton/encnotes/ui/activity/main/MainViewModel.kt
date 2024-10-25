package com.juanarton.encnotes.ui.activity.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
import com.juanarton.encnotes.core.data.source.remote.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val notesAppRepositoryUseCase: NotesAppRepositoryUseCase
): ViewModel() {
    private var _getNotes: MutableLiveData<List<Notes>> = MutableLiveData()
    var getNotes: LiveData<List<Notes>> = _getNotes

    fun getIsLoggedIn(): Boolean = notesAppRepositoryUseCase.getIsLoggedIn()

    fun getNotes() {
        viewModelScope.launch {
            notesAppRepositoryUseCase.getNotes().collect {
                _getNotes.value = it
            }
        }
    }
}