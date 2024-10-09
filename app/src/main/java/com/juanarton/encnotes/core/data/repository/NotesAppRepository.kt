package com.juanarton.encnotes.core.data.repository

import com.juanarton.encnotes.core.data.domain.repository.INotesAppRepository
import com.juanarton.encnotes.core.data.source.local.SharedPrefDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NotesAppRepository @Inject constructor(
    private val sharedPrefDataSource: SharedPrefDataSource
): INotesAppRepository {
    override fun setIsLoggedIn(isLoggedIn: Boolean) = flow {
        emit(sharedPrefDataSource.setIsLoggedIn(isLoggedIn))
    }

    override fun getIsLoggedIn(): Boolean = sharedPrefDataSource.getIsLoggedIn()

    override fun setGUID(gUID: String): Flow<Boolean> = flow {
        emit(sharedPrefDataSource.setGUID(gUID))
    }

    override fun getGuid(): String? = sharedPrefDataSource.getGUID()
}