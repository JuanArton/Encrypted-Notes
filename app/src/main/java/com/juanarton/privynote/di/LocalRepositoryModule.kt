package com.juanarton.privynote.di

import com.juanarton.privynote.core.data.domain.repository.ILocalNotesRepository
import com.juanarton.privynote.core.data.repository.LocalNotesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class LocalRepositoryModule {
    @Binds
    abstract fun provideLocalNotesRepository(
        localNotesRepository: LocalNotesRepository
    ): ILocalNotesRepository
}