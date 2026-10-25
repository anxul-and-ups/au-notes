package com.example.data.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps the microphone allowed while a live voice conversation is running, so you can leave AU Notes
 * (e.g. to WhatsApp) and keep talking to AU Agent. Shows a persistent notification while it runs.
 */
class LiveVoiceService : Service() {
    companion object {
        fun start(context: Context) {
            val i = Intent(context, LiveVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
        }
        fun stop(context: Context) { context.stopService(Intent(context, LiveVoiceService::class.java)) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel("au_live_voice", "AU Agent live voice", NotificationManager.IMPORTANCE_LOW))
        }
        val n: Notification = NotificationCompat.Builder(this, "au_live_voice")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("AU Agent is listening")
            .setContentText("Live voice is on. Open AU Notes to end it.")
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(4712, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else startForeground(4712, n)
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }
}
