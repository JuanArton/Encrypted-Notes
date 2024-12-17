package com.juanarton.privynote.ui.fragment

import androidx.lifecycle.ViewModel
import com.juanarton.privynote.core.data.domain.usecase.local.LocalNotesRepoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val localNotesRepoUseCase: LocalNotesRepoUseCase,
): ViewModel() {
    suspend fun setCipherKey(cipherKey: String): Boolean {
        return localNotesRepoUseCase.setCipherKey(cipherKey).first()
    }
}