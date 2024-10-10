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

}