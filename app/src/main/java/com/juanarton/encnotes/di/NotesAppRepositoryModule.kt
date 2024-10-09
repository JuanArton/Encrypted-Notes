package com.juanarton.encnotes.di

import com.juanarton.encnotes.core.data.domain.repository.INotesAppRepository
import com.juanarton.encnotes.core.data.repository.NotesAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class NotesAppRepositoryModule {
    @Binds
    abstract fun provideNotesAppRepository(
        notesAppRepository: NotesAppRepository
    ): INotesAppRepository
}