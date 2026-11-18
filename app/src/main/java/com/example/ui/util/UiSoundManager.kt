package com.example.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Zero-latency synthesized tactile UI sound engine for AU Notes.
 *
 * Replaces harsh default folder vibrations with studio-crafted synthesized acoustic cues:
 *  1. [playFolderTap]  — Smooth typographic mechanical key tap when switching folders/tabs.
 *  2. [playSciFiLock]  — Futuristic sci-fi harmonic lock/unlock pulse when locking or unlocking.
 *  3. [playDeleteSound] — Crisp modern delete/recycle whoosh-chime when moving to trash/deleting.
 */
object UiSoundManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val SAMPLE_RATE = 44100

    @Volatile
    private var soundPool: SoundPool? = null

    @Volatile
    private var tapSoundId: Int = 0

    @Volatile
    private var lockSoundId: Int = 0

    @Volatile
    private var unlockSoundId: Int = 0

    @Volatile
    private var deleteSoundId: Int = 0

    @Volatile
    private var initialized = false

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (initialized && soundPool != null) return
        initialized = true
        val appCtx = context.applicationContext
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        soundPool = pool

        scope.launch {
            try {
                val cacheDir = File(appCtx.cacheDir, "ui_sounds").apply { mkdirs() }
                val tapFile = File(cacheDir, "typo_tap_v1.wav")
                val lockFile = File(cacheDir, "scifi_lock_v1.wav")
                val unlockFile = File(cacheDir, "scifi_unlock_v1.wav")
                val deleteFile = File(cacheDir, "crisp_delete_v1.wav")

                if (!tapFile.exists() || tapFile.length() < 100L) {
                    writeWavFile(tapFile, synthesizeTypographicTap())
                }
                if (!lockFile.exists() || lockFile.length() < 100L) {
                    writeWavFile(lockFile, synthesizeSciFiLock(unlock = false))
                }
                if (!unlockFile.exists() || unlockFile.length() < 100L) {
                    writeWavFile(unlockFile, synthesizeSciFiLock(unlock = true))
                }
                if (!deleteFile.exists() || deleteFile.length() < 100L) {
                    writeWavFile(deleteFile, synthesizeCrispDelete())
                }

                tapSoundId = pool.load(tapFile.absolutePath, 1)
                lockSoundId = pool.load(lockFile.absolutePath, 1)
                unlockSoundId = pool.load(unlockFile.absolutePath, 1)
                deleteSoundId = pool.load(deleteFile.absolutePath, 1)
            } catch (_: Throwable) {
                // Ignore audio init errors on headless JVM / muted devices
            }
        }
    }

    private fun canPlaySound(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am == null || am.ringerMode == AudioManager.RINGER_MODE_NORMAL
        } catch (_: Throwable) {
            true
        }
    }

    /**
     * Smooth typographic mechanical key-click sound for folder & tab switching.
     * No vibration is triggered.
     */
    fun playFolderTap(context: Context) {
        try {
            ensureInitialized(context)
            if (!canPlaySound(context)) return
            val pool = soundPool ?: return
            val id = tapSoundId
            if (id != 0) {
                pool.play(id, 0.52f, 0.52f, 1, 0, 1.0f)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Futuristic sci-fi biometric/vault lock & unlock sound.
     */
    fun playSciFiLock(context: Context, isUnlock: Boolean = false) {
        try {
            ensureInitialized(context)
            if (!canPlaySound(context)) return
            val pool = soundPool ?: return
            val id = if (isUnlock) unlockSoundId else lockSoundId
            if (id != 0) {
                pool.play(id, 0.68f, 0.68f, 2, 0, 1.0f)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Crisp modern delete / move-to-recycle-bin chime.
     */
    fun playDeleteSound(context: Context) {
        try {
            ensureInitialized(context)
            if (!canPlaySound(context)) return
            val pool = soundPool ?: return
            val id = deleteSoundId
            if (id != 0) {
                pool.play(id, 0.62f, 0.62f, 2, 0, 1.0f)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Synthesizes a warm, tactile mechanical typewriter/switch tap (~36ms).
     */
    internal fun synthesizeTypographicTap(): ShortArray {
        val durationSec = 0.036
        val count = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            // Fast percussive attack + warm low-mid body resonance (420Hz -> 180Hz) + crisp keycap transient (1950Hz)
            val envBody = exp(-t * 155.0)
            val envClick = exp(-t * 480.0)
            val freqBody = 390.0 - 180.0 * (t / durationSec)
            val bodyWave = sin(2.0 * PI * freqBody * t) * envBody * 0.68
            val clickWave = sin(2.0 * PI * 1880.0 * t) * envClick * 0.32
            val combined = (bodyWave + clickWave).coerceIn(-1.0, 1.0)
            samples[i] = (combined * 24000).toInt().toShort()
        }
        return samples
    }

    /**
     * Synthesizes a futuristic sci-fi dual-tone harmonic servo lock/unlock sound (~195ms).
     */
    internal fun synthesizeSciFiLock(unlock: Boolean): ShortArray {
        val durationSec = 0.195
        val count = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            // Two-stage futuristic pitch sweep:
            // Lock: high-tech confirmation step down + harmonic latch (1180Hz -> 620Hz -> 880Hz)
            // Unlock: ascending cyber chime (540Hz -> 860Hz -> 1320Hz)
            val freq = if (unlock) {
                if (progress < 0.45) 560.0 + progress * 650.0 else 880.0 + (progress - 0.45) * 820.0
            } else {
                if (progress < 0.50) 1140.0 - progress * 720.0 else 680.0 - (progress - 0.50) * 220.0
            }
            val subFreq = freq * 0.5
            val shimmerFreq = freq * 1.5
            val attack = (t / 0.006).coerceAtMost(1.0)
            val decay = exp(-t * 16.5)
            val gate = if (progress in 0.44..0.50) 0.35 else 1.0
            val env = attack * decay * gate

            val wave = (
                sin(2.0 * PI * freq * t) * 0.58 +
                    sin(2.0 * PI * shimmerFreq * t) * 0.27 +
                    sin(2.0 * PI * subFreq * t) * 0.15
                ) * env
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
        }
        return samples
    }

    /**
     * Synthesizes a crisp modern delete/recycle whoosh + crystalline drop chime (~165ms).
     */
    internal fun synthesizeCrispDelete(): ShortArray {
        val durationSec = 0.165
        val count = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            // Descending crystalline sweep from 920Hz -> 260Hz with crisp transient
            val freq = 920.0 * exp(-progress * 1.35)
            val overtone = freq * 2.0
            val attack = (t / 0.004).coerceAtMost(1.0)
            val decay = exp(-t * 21.0)
            val env = attack * decay
            val wave = (sin(2.0 * PI * freq * t) * 0.72 + sin(2.0 * PI * overtone * t) * 0.28) * env
            samples[i] = (wave.coerceIn(-1.0, 1.0) * 25500).toInt().toShort()
        }
        return samples
    }

    private fun writeWavFile(file: File, pcmData: ShortArray) {
        val byteDataSize = pcmData.size * 2
        val totalFileSize = 36 + byteDataSize
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalFileSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // AudioFormat = 1 (PCM)
            putShort(1) // Mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2) // ByteRate
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(byteDataSize)
        }.array()

        val body = ByteBuffer.allocate(byteDataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            body.putShort(sample)
        }

        val out = ByteArrayOutputStream(44 + byteDataSize)
        out.write(header)
        out.write(body.array())
        FileOutputStream(file).use { it.write(out.toByteArray()) }
    }
}
