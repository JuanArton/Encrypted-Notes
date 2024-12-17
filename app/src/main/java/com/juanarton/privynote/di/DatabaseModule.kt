package com.juanarton.privynote.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.juanarton.privynote.core.data.source.local.room.NotesAppDatabase
import com.juanarton.privynote.core.data.source.local.room.dao.AttachmentsDAO
import com.juanarton.privynote.core.data.source.local.room.dao.NotesDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    companion object {
        const val FILE_NAME = "DB_KEY"
        const val KEY_NAME = "DB_KEY"
        const val DB_NAME = "notesapp.db"
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotesAppDatabase {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val dbKey = sharedPreferences.getString(KEY_NAME, null)

        val passphrase = if (dbKey.isNullOrEmpty()) {
            val newKey = generateRandomString()
            sharedPreferences.edit().putString(KEY_NAME, newKey).apply()
            newKey.toByteArray()
        } else {
            dbKey.toByteArray()
        }

        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            NotesAppDatabase::class.java, DB_NAME
        ).fallbackToDestructiveMigration().openHelperFactory(factory).build()
    }

    private fun generateRandomString(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16)
            .map { chars.random() }
            .joinToString("")
    }

    @Provides
    @Singleton
    fun provideNotesDao(database: NotesAppDatabase): NotesDAO = database.notesDAO()

    @Provides
    @Singleton
    fun provideAttachmentsDao(database: NotesAppDatabase): AttachmentsDAO = database.attachmentDAO()
}