package com.juanarton.encnotes.di

import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryImpl
import com.juanarton.encnotes.core.data.domain.usecase.NotesAppRepositoryUseCase
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
    abstract fun provideNoteAppRepositoryUseCase(
        notesAppRepositoryImpl: NotesAppRepositoryImpl
    ): NotesAppRepositoryUseCase
}