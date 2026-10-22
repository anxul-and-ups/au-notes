package com.example.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Plays the alarm / notification tone itself instead of relying on a notification channel.
 * Channels freeze their sound after the first creation and cannot read file:// or content:// tones
 * picked by the user, which is why custom tones used to stay silent.
 */
object AlarmSoundPlayer {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stop() }

    /** Tries [preferred] first, then the device's default alarm / notification / ringtone. */
    fun play(context: Context, preferred: Uri?, maxMillis: Long = 30_000L) {
        stop()
        val app = context.applicationContext
        val candidates = listOfNotNull(
            preferred,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        for (uri in candidates) {
            try {
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(app, uri)
                mp.isLooping = true
                mp.setOnErrorListener { _, _, _ -> stop(); true }
                mp.prepare()
                mp.start()
                player = mp
                handler.postDelayed(autoStop, maxMillis)
                return
            } catch (e: Exception) {
                // unreadable tone -> try the next fallback
            }
        }
    }

    fun stop() {
        handler.removeCallbacks(autoStop)
        try {
            player?.let { if (it.isPlaying) it.stop(); it.release() }
        } catch (_: Exception) { }
        player = null
    }
}
