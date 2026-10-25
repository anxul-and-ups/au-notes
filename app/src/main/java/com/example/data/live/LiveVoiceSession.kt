package com.example.data.live

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import com.example.data.ai.ScreenShare
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A realistic spoken conversation: microphone -> Gemini Live -> speaker, with tools executed by the app.
 * [level] is the REAL loudness (RMS) of whatever audio is currently moving: your voice while you talk,
 * the assistant's voice while it answers. The orb is driven by it.
 */
class LiveVoiceSession(
    private val apiKey: String,
    private val model: String,
    private val voiceName: String,
    private val systemPrompt: String,
    private val tools: JSONArray?,
    private val executeTool: suspend (name: String, args: JSONObject) -> String,
    private val onTurn: (userText: String, assistantText: String) -> Unit,
    private val onFatal: (String) -> Unit
) : GeminiLiveClient.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var client: GeminiLiveClient? = null
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var micJob: Job? = null
    private var frameJob: Job? = null

    private val _state = MutableStateFlow("connecting") // connecting | listening | thinking | speaking
    val state: StateFlow<String> = _state.asStateFlow()
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()
    private val _userText = MutableStateFlow("")
    val userText: StateFlow<String> = _userText.asStateFlow()
    private val _assistantText = MutableStateFlow("")
    val assistantText: StateFlow<String> = _assistantText.asStateFlow()

    @Volatile private var playingUntil = 0L
    @Volatile private var muted = false
    private var turnUser = StringBuilder()
    private var turnAssistant = StringBuilder()
    private var inLevel = 0f
    private var outLevel = 0f

    fun start() {
        client = GeminiLiveClient(apiKey, model, voiceName, systemPrompt, tools, this).also { it.connect() }
    }

    fun setMuted(m: Boolean) { muted = m }
    fun sendText(text: String) { client?.sendText(text) }

    fun stop() {
        micJob?.cancel(); frameJob?.cancel()
        try { record?.stop() } catch (_: Exception) {}
        try { record?.release() } catch (_: Exception) {}
        try { track?.stop() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
        record = null; track = null
        client?.close(); client = null
        scope.cancel()
    }

    // ------------------------------------------------------------ listener
    override fun onReady() {
        startPlayback()
        startMic()
        startScreenFrames()
        scope.launch {
            while (isActive) { // the orb level = whichever side is currently louder, with a soft decay
                inLevel *= 0.8f; outLevel *= 0.85f
                _level.value = maxOf(inLevel, outLevel).coerceIn(0f, 1f)
                delay(40)
            }
        }
        _state.value = "listening"
    }

    override fun onAudio(pcm24k: ByteArray) {
        _state.value = "speaking"
        val t = track ?: return
        // real loudness of this chunk
        var sum = 0.0
        var i = 0
        while (i + 1 < pcm24k.size) {
            val s = ((pcm24k[i + 1].toInt() shl 8) or (pcm24k[i].toInt() and 0xFF)).toShort().toInt()
            sum += s.toDouble() * s
            i += 2
        }
        val rms = sqrt(sum / (pcm24k.size / 2).coerceAtLeast(1)).toFloat()
        outLevel = maxOf(outLevel, (rms / 9000f).coerceIn(0f, 1f))
        val durMs = pcm24k.size / 2 * 1000L / 24000L
        val now = System.currentTimeMillis()
        playingUntil = maxOf(playingUntil, now) + durMs
        try { t.write(pcm24k, 0, pcm24k.size) } catch (_: Exception) {}
    }

    override fun onInputText(chunk: String) {
        turnUser.append(chunk)
        _userText.value = turnUser.toString()
        if (_state.value != "speaking") _state.value = "thinking"
    }

    override fun onOutputText(chunk: String) {
        turnAssistant.append(chunk)
        _assistantText.value = turnAssistant.toString()
    }

    override fun onInterrupted() {
        // the user talked over the assistant: drop whatever audio is still queued
        try { track?.pause(); track?.flush(); track?.play() } catch (_: Exception) {}
        playingUntil = 0L
        _state.value = "listening"
    }

    override fun onTurnComplete() {
        val u = turnUser.toString().trim()
        val a = turnAssistant.toString().trim()
        if (u.isNotEmpty() || a.isNotEmpty()) onTurn(u, a)
        turnUser = StringBuilder(); turnAssistant = StringBuilder()
        scope.launch {
            // stay in "speaking" until the queued audio has really finished
            while (System.currentTimeMillis() < playingUntil) delay(80)
            _state.value = "listening"
        }
    }

    override fun onToolCall(id: String, name: String, args: JSONObject) {
        _state.value = "thinking"
        scope.launch {
            val result = try { executeTool(name, args) } catch (e: Exception) { "FAILED: ${e.message}" }
            client?.sendToolResponse(id, name, JSONObject().put("result", result))
        }
    }

    override fun onError(message: String, fatal: Boolean) { if (fatal) onFatal(message) }
    override fun onClosed() {}

    // ------------------------------------------------------------ audio plumbing
    @SuppressLint("MissingPermission")
    private fun startMic() {
        val rate = 16000
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 4096) * 2)
        } catch (e: Exception) { onFatal("Microphone could not start: ${e.message}"); return }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { onFatal("Microphone could not start. Check the microphone permission."); return }
        try {
            if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(rec.audioSessionId)?.enabled = true
            if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(rec.audioSessionId)?.enabled = true
        } catch (_: Exception) {}
        record = rec
        rec.startRecording()
        micJob = scope.launch {
            val buf = ByteArray(1600) // 50 ms
            while (isActive) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                var i = 0
                while (i + 1 < n) {
                    val s = ((buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)).toShort().toInt()
                    sum += s.toDouble() * s
                    i += 2
                }
                val rms = sqrt(sum / (n / 2).coerceAtLeast(1)).toFloat()
                if (muted) continue
                val assistantTalking = System.currentTimeMillis() < playingUntil
                // while the assistant speaks, only clearly loud speech (barge-in) goes up: avoids hearing itself
                if (assistantTalking && rms < 2200f) continue
                inLevel = maxOf(inLevel, (rms / 6000f).coerceIn(0f, 1f))
                client?.sendAudio(if (n == buf.size) buf else buf.copyOf(n))
            }
        }
    }

    private fun startPlayback() {
        val rate = 24000
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(min, 8192) * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        t.play()
    }

    /** While screen sharing is on, a screenshot goes to Gemini about once a second ("see my screen"). */
    private fun startScreenFrames() {
        frameJob = scope.launch {
            var last: Int? = null
            while (isActive) {
                if (ScreenShare.active) {
                    val jpeg = ScreenShare.latestJpeg
                    if (jpeg != null && jpeg.size.hashCode() != last) {
                        last = jpeg.size.hashCode()
                        client?.sendImage(jpeg)
                    }
                }
                delay(1000)
            }
        }
    }
}
