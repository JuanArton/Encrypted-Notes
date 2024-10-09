package com.juanarton.encnotes.core.data.domain.usecase

import com.juanarton.encnotes.core.data.domain.repository.INotesAppRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotesAppRepositoryImpl @Inject constructor(
    private val iNotesAppRepository: INotesAppRepository
): NotesAppRepositoryUseCase{
    override fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean> =
        iNotesAppRepository.setIsLoggedIn(isLoggedIn)

    override fun getIsLoggedIn(): Boolean = iNotesAppRepository.getIsLoggedIn()

    override fun setGUID(gUID: String): Flow<Boolean> =
        iNotesAppRepository.setGUID(gUID)

    override fun getGuid(): String? =
        iNotesAppRepository.getGuid()
}