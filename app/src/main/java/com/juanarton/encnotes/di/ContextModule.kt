package com.juanarton.encnotes.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import javax.inject.Singleton
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ContextModule {
    @Singleton
    @Provides
    fun provideContext(application: Application): Context = application.applicationContext
}