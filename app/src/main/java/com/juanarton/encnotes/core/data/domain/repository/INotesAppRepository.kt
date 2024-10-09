package com.juanarton.encnotes.core.data.domain.repository

import kotlinx.coroutines.flow.Flow

interface INotesAppRepository {
    fun setIsLoggedIn(isLoggedIn: Boolean): Flow<Boolean>
    fun getIsLoggedIn(): Boolean
    fun setGUID(gUID: String): Flow<Boolean>
    fun getGuid(): String?
}