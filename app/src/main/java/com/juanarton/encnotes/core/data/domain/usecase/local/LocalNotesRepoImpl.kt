package com.juanarton.encnotes.core.data.domain.usecase.local

import com.juanarton.encnotes.core.data.domain.model.Notes
import com.juanarton.encnotes.core.data.domain.repository.ILocalNotesRepository
import com.juanarton.encnotes.core.data.source.remote.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalNotesRepoImpl @Inject constructor(
    private val iLocalNotesRepository: ILocalNotesRepository
): LocalNotesRepoUseCase {
    override fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean> =
        iLocalNotesRepository.setIsLoggedIn(isLoggedIn)

    override fun getIsLoggedIn(): Boolean = iLocalNotesRepository.getIsLoggedIn()

    /*override fun getNotes(): Flow<PagingData<Notes>> =
        iNotesAppRepository.getNotes()*/

    override fun getNotes(): Flow<List<Notes>> =
        iLocalNotesRepository.getNotes()

    override fun insertNotes(notes: Notes): Flow<Resource<Boolean>> =
        iLocalNotesRepository.insertNotes(notes)

    override fun setAccessKey(accessKey: String): Flow<Boolean> =
        iLocalNotesRepository.setAccessKey(accessKey)

    override fun getAccessKey(): String? = iLocalNotesRepository.getAccessKey()

    override fun setRefreshKey(refreshKey: String): Flow<Boolean> =
        iLocalNotesRepository.setRefreshKey(refreshKey)

    override fun getRefreshKey(): String? = iLocalNotesRepository.getRefreshKey()

    override fun setCipherKey(cipherKey: String): Flow<Boolean> =
        iLocalNotesRepository.setCipherKey(cipherKey)

    override fun getCipherKey(): String? =
        iLocalNotesRepository.getCipherKey()

    override fun deleteNotes(notes: Notes): Flow<Resource<Boolean>> =
        iLocalNotesRepository.deleteNotes(notes)

    override fun getNotesById(id: String): Flow<Notes> =
        iLocalNotesRepository.getNotesById(id)

    override fun updateNotes(notes: Notes): Flow<Resource<Boolean>> =
        iLocalNotesRepository.updateNotes(notes)

    override fun permanentDeleteNotes(id: String) =
        iLocalNotesRepository.permanentDeleteNotes(id)
}