package com.juanarton.privynote.receiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.juanarton.privynote.R
import com.juanarton.privynote.core.data.repository.LocalNotesRepository
import com.juanarton.privynote.core.data.source.local.SharedPrefDataSource
import com.juanarton.privynote.core.utils.Cryptography
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.APP_SETTINGS
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.AUTO_BACKUP
import com.juanarton.privynote.ui.activity.settings.SettingsViewModel.Companion.BACKUP_INTERVAL
import com.juanarton.privynote.utils.Utils.createNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class BackupBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var localNotesRepository: LocalNotesRepository

    @Inject
    lateinit var sharedPrefDataSource: SharedPrefDataSource

    companion object {
        const val LAST_BACKUP_TIME = "last_backup_time"
        const val ALARM_ID = 102
        private const val TIME = "09:00"
        const val BACKUP_NOTIF_CHANNEL_ID = "Backup Notification"

        @SuppressLint("CommitPrefEdits")
        fun setRepeatingAlarm(context: Context, intervalInDay: Int) {
            val sPref = context.getSharedPreferences(APP_SETTINGS, MODE_PRIVATE)
            val isAutoBackup = sPref.getBoolean(AUTO_BACKUP, true)

            if (isAutoBackup) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val timeArray = TIME.split(":").toTypedArray()
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, timeArray[0].toInt())
                calendar.set(Calendar.MINUTE, timeArray[1].toInt())
                calendar.set(Calendar.SECOND, 0)

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, intervalInDay)
                }

                val intent = Intent(context, BackupBroadcastReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_ID,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                if (pendingIntent != null) {
                    return
                }

                val newPendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_ID,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= 31) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            newPendingIntent
                        )
                    } else {
                        val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intentSettings)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        newPendingIntent
                    )
                }
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BackupBroadcastReceiver::class.java)
            val requestCode = ALARM_ID
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE)
            pendingIntent.cancel()
            alarmManager.cancel(pendingIntent)
        }

        fun isSameDay(time1: Long, time2: Long): Boolean {
            val calendar1 = Calendar.getInstance().apply { timeInMillis = time1 }
            val calendar2 = Calendar.getInstance().apply { timeInMillis = time2 }
            return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                    calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        createNotification(
            context, "PrivyNote Backup", "PrivyNote backup notification channel",
            BACKUP_NOTIF_CHANNEL_ID, context.getString(R.string.backup_complete),
            context.getString(R.string.backup_msg)
        )

        val sPref =  context.getSharedPreferences(APP_SETTINGS, MODE_PRIVATE)
        val backupInterval = sPref.getInt(BACKUP_INTERVAL, 1)
        val lastBackupTime = sPref.getLong(LAST_BACKUP_TIME, 0)
        val currentTime = System.currentTimeMillis()

        if (!isSameDay(lastBackupTime, currentTime)) {
            val key = sharedPrefDataSource.getCipherKey()

            if (!key.isNullOrEmpty()) {
                val deserializedKey = Cryptography.deserializeKeySet(key)

                runBlocking {
                    localNotesRepository.backUpNotes(context, deserializedKey).first()
                    sPref.edit().putLong(LAST_BACKUP_TIME, currentTime).apply()

                    cancelAlarm(context)
                    delay(500)
                    setRepeatingAlarm(context, backupInterval)
                }
            } else {
                Toast.makeText(
                    context, context.getString(R.string.unable_retrieve_key), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}