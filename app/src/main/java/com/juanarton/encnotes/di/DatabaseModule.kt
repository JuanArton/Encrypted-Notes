package com.juanarton.encnotes.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.juanarton.encnotes.core.data.source.local.room.NotesAppDatabase
import com.juanarton.encnotes.core.data.source.local.room.dao.NotesDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    companion object {
        const val FILE_NAME = "DB_KEY"
        const val KEY_NAME = "DB_KEY"
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
            SQLiteDatabase.getBytes(generateRandomString().toCharArray())
        } else {
            SQLiteDatabase.getBytes(dbKey.toCharArray())
        }

        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            NotesAppDatabase::class.java, "notesapp.db"
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
    fun provideAppStateDao(database: NotesAppDatabase): NotesDAO = database.notesDAO()
}