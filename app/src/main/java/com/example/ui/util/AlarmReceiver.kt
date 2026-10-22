package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        // v2: the old channel had its sound frozen at creation and could never play a custom tone
        const val CHANNEL_ID = "au_notes_alarm_channel_v2"
        const val EXTRA_TITLE = "extra_alarm_title"
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val ACTION_STOP = "com.example.ACTION_STOP_ALARM_SOUND"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP) {
            AlarmSoundPlayer.stop()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Note Reminder Alarm"
        val customRingtoneUriStr = intent.getStringExtra(EXTRA_RINGTONE_URI)
        val ringtoneUri: Uri? = if (!customRingtoneUriStr.isNullOrBlank()) Uri.parse(customRingtoneUriStr) else null

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AU Notes Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and reminders for AU Notes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(null, null) // the tone is played by AlarmSoundPlayer
            }
            notificationManager.createNotificationChannel(channel)
        }

        val immutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutable
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 7001,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or immutable
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_verified_badge)
            .setContentTitle(title)
            .setContentText("AU Notes Alarm: It's time for your scheduled reminder!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setDeleteIntent(stopIntent)
            .addAction(0, "Stop", stopIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
        AlarmSoundPlayer.play(context, ringtoneUri)
    }
}
