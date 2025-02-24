package com.juanarton.privynote.worker

import android.content.Context
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.repository.LocalNotesRepository
import com.juanarton.privynote.core.data.source.local.SharedPrefDataSource
import com.juanarton.privynote.core.utils.Cryptography
import com.juanarton.privynote.receiver.BackupBroadcastReceiver.Companion.BACKUP_NOTIF_CHANNEL_ID
import com.juanarton.privynote.receiver.BackupBroadcastReceiver.Companion.LAST_BACKUP_TIME
import com.juanarton.privynote.receiver.BackupBroadcastReceiver.Companion.isSameDay
import com.juanarton.privynote.receiver.BackupBroadcastReceiver.Companion.setRepeatingAlarm
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.APP_SETTINGS
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.AUTO_BACKUP
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.BACKUP_INTERVAL
import com.juanarton.privynote.utils.Utils.createNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class BackupWorker @AssistedInject constructor(
    private val localNotesRepository: LocalNotesRepository,
    private val sharedPrefDataSource: SharedPrefDataSource,
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    companion object {
        fun scheduleBackupWork(context: Context, intervalInDays: Int) {
            val sPref = context.getSharedPreferences(APP_SETTINGS, MODE_PRIVATE)
            val isAutoBackup = sPref.getBoolean(AUTO_BACKUP, true)

            if (isAutoBackup) {
                val workManager = WorkManager.getInstance(context)

                workManager.cancelUniqueWork("PrivyNoteBackup")

                val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalInDays.toLong(), TimeUnit.DAYS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .setRequiresBatteryNotLow(false)
                            .build()
                    )
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "PrivyNoteBackup",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun cancelBackupWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("PrivyNoteBackup")
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val sPref =  context.getSharedPreferences(APP_SETTINGS, MODE_PRIVATE)
        val lastBackupTime = sPref.getLong(LAST_BACKUP_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val backupInterval = sPref.getInt(BACKUP_INTERVAL, 1)

        if (!isSameDay(lastBackupTime, currentTime)) {
            val key = sharedPrefDataSource.getCipherKey()

            if (!key.isNullOrEmpty()) {
                val deserializedKey = Cryptography.deserializeKeySet(key)
                localNotesRepository.backUpNotes(context, deserializedKey).first()
                sPref.edit().putLong(LAST_BACKUP_TIME, currentTime).apply()
                setRepeatingAlarm(context, backupInterval)

                createNotification(
                    context, "PrivyNote Backup", "PrivyNote backup notification channel",
                    BACKUP_NOTIF_CHANNEL_ID, context.getString(R.string.backup_complete),
                    context.getString(R.string.backup_msg)
                )

                return Result.success()
            } else {
                return Result.failure()
            }
        }

        return Result.success()
    }
}