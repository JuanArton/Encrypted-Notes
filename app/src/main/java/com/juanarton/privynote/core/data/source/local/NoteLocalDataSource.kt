package com.juanarton.privynote.core.data.source.local

import android.content.Context
import android.os.Environment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.KeysetHandle
import com.juanarton.privynote.core.data.source.local.room.dao.NotesDAO
import com.juanarton.privynote.core.data.source.local.room.entity.NotesEntity
import com.juanarton.privynote.core.utils.Cryptography
import com.juanarton.privynote.di.DatabaseModule.Companion.DB_NAME
import com.juanarton.privynote.di.DatabaseModule.Companion.FILE_NAME
import com.juanarton.privynote.di.DatabaseModule.Companion.KEY_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteLocalDataSource @Inject constructor(
    private val notesDAO: NotesDAO,
) {
    fun getNotes(): List<NotesEntity> {
        return notesDAO.getNotes()
    }

    fun insertNotes(notes: NotesEntity) {
        notesDAO.insertNotes(notes)
    }

    fun deleteNotes(notesEntity: NotesEntity) {
        notesDAO.deleteNotes(notesEntity)
    }

    fun updateNotes(notesEntity: NotesEntity) {
        notesDAO.updateNotes(notesEntity)
    }

    fun getNotesById(id: String): NotesEntity {
        return notesDAO.getNotesById(id)
    }

    fun permanentDelete(id: String) {
        notesDAO.permanentDelete(id)
    }

    fun backUpNotes(context: Context, keysetHandle: KeysetHandle): Flow<Boolean> =
        flow {
            val dbPath = File(context.filesDir.parentFile, "databases").absolutePath
            val filesDirPath = context.filesDir.absolutePath + "/images"

            try {
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
                val dbKey = sharedPreferences.getString(KEY_NAME, null) ?: throw IllegalStateException("Database key not found")

                val backupFile = File(context.filesDir, "backup.zip")
                ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                    zipFile(File("$dbPath/notesapp.db"), "notesapp.db", zos)
                    zipFile(File("$dbPath/notesapp.db-wal"), "notesapp.db-wal", zos)
                    zipFile(File("$dbPath/notesapp.db-shm"), "notesapp.db-shm", zos)
                    zipFolder(File(filesDirPath), "filesDir", zos)
                    zipData(dbKey.toByteArray(), "db_key", zos)
                }

                val backupDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PrivyNotes"
                )
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestamp = dateFormat.format(Date())
                val encryptedBackup = Cryptography.encrypt(backupFile.readBytes(), keysetHandle)

                val outputFile = File(
                    backupDir, "PrivyNote_Encrypted_Backup_$timestamp.bin"
                )
                outputFile.writeBytes(encryptedBackup)

                val backupFiles = backupDir.listFiles { file ->
                    file.name.startsWith("PrivyNote_Encrypted_Backup") && file.name.endsWith(".bin")
                }?.sortedBy { it.lastModified() } ?: emptyList()

                if (backupFiles.size > 5) {
                    backupFiles.take(backupFiles.size - 5).forEach { it.delete() }
                }

                backupFile.delete()
                emit(true)
            } catch (e: Exception) {
                emit(false)
                e.printStackTrace()
            }
        }.flowOn(Dispatchers.IO)

    fun restoreNotes(context: Context, keysetHandle: KeysetHandle, backupFile: ByteArray): Flow<Boolean> =
        flow {
            try {
                val dbPath = File(context.filesDir.parentFile, "databases").absolutePath
                val filesDirPath = context.filesDir.absolutePath + "/images"
                val imageDir =  File(filesDirPath)

                if (imageDir.exists() && imageDir.isDirectory) {
                    val files = imageDir.listFiles()
                    files?.forEach { file ->
                        if (file.isFile) {
                            file.delete()
                        }
                    }
                }

                val decryptedBackup = Cryptography.decrypt(backupFile, keysetHandle)

                val backupFile = File(context.filesDir, "backup.zip")
                backupFile.writeBytes(decryptedBackup)

                context.deleteDatabase(DB_NAME)
                ZipInputStream(FileInputStream(backupFile)).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val outputFile = when (entry?.name) {
                            "notesapp.db" -> File(dbPath, "notesapp.db")
                            "notesapp.db-wal" -> File(dbPath, "notesapp.db-wal")
                            "notesapp.db-shm" -> File(dbPath, "notesapp.db-shm")
                            "db_key" -> File(context.filesDir, "db_key")
                            else -> File(filesDirPath, entry?.name?.removePrefix("filesDir/") ?: continue)
                        }
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }

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
                val dbKeyFile = File(context.filesDir, "db_key")

                val dbKey = dbKeyFile.readBytes().decodeToString()
                sharedPreferences.edit().putString(KEY_NAME, dbKey).apply()

                emit(true)
            } catch (e: Exception) {
                emit(false)
            }
        }.flowOn(Dispatchers.IO)


    private fun zipFile(file: File, entryName: String, zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun zipFolder(folder: File, baseFolder: String, zos: ZipOutputStream) {
        folder.listFiles()?.forEach {
            val entryName = "$baseFolder/${it.name}"
            if (it.isDirectory) zipFolder(it, entryName, zos) else zipFile(it, entryName, zos)
        }
    }

    private fun zipData(data: ByteArray, entryName: String, zos: ZipOutputStream) {
        zos.putNextEntry(ZipEntry(entryName))
        zos.write(data)
        zos.closeEntry()
    }
}