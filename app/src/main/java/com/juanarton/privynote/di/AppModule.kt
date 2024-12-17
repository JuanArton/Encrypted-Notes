package com.juanarton.privynote.di

import com.juanarton.privynote.core.data.domain.usecase.local.LocalNotesRepoImpl
import com.juanarton.privynote.core.data.domain.usecase.local.LocalNotesRepoUseCase
import com.juanarton.privynote.core.data.domain.usecase.remote.RemoteNotesRepoImpl
import com.juanarton.privynote.core.data.domain.usecase.remote.RemoteNotesRepoUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Suppress("unused")
@Module
@InstallIn(ViewModelComponent::class)
abstract class AppModule
{
    @Binds
    @ViewModelScoped
    abstract fun provideLocalNoteRepositoryUseCase(
        localNotesRepoImpl: LocalNotesRepoImpl
    ): LocalNotesRepoUseCase

    @Binds
    @ViewModelScoped
    abstract fun provideRemoteNoteRepositoryUseCase(
        remoteNotesRepoImpl: RemoteNotesRepoImpl
    ): RemoteNotesRepoUseCase
}