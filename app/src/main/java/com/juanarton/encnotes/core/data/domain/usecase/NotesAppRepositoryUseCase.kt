package com.juanarton.encnotes.core.data.domain.usecase

import kotlinx.coroutines.flow.Flow

interface NotesAppRepositoryUseCase {
    fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean>
    fun getIsLoggedIn(): Boolean
    fun setGUID(gUID: String): Flow<Boolean>
    fun getGuid(): String?
}