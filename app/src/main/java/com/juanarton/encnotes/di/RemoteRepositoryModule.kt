package com.juanarton.encnotes.di

import com.juanarton.encnotes.core.data.domain.repository.IRemoteNoteRepository
import com.juanarton.encnotes.core.data.repository.RemoteNoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteRepositoryModule {
    @Binds
    abstract fun provideRemoteNotesRepository(
        remoteNoteRepository: RemoteNoteRepository
    ): IRemoteNoteRepository
}