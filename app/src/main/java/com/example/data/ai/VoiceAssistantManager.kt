package com.example.data.ai

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class VoiceAssistantManager(
    private val context: Context,
    private val onUserSpeechResult: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    // Real speech activity while the assistant talks (word-by-word from the TTS engine) — drives the orb
    private val _speakLevel = MutableStateFlow(0f)
    val speakLevel: StateFlow<Float> = _speakLevel.asStateFlow()

    private var userRate = 0.96f
    private var userPitch = 1.02f
    private var userVolume = 1.0f
    private var userVoiceName = ""

    var isVoiceModeActive = false
        private set

    /** Applies the Voice Adjuster settings (speed / pitch / volume / chosen voice) to the real engine. */
    fun applyVoiceSettings(rate: Float, pitch: Float, volume: Float, voiceName: String) {
        userRate = rate; userPitch = pitch; userVolume = volume; userVoiceName = voiceName
        val engine = tts ?: return
        if (!isTtsReady) return
        try {
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)
            if (voiceName.isNotBlank()) engine.voices?.firstOrNull { it.name == voiceName }?.let { engine.voice = it }
        } catch (_: Exception) {}
    }

    /** Voices installed on this phone: (name, readable label). Female-looking voices are marked. */
    fun availableVoices(): List<Pair<String, String>> {
        val engine = tts ?: return emptyList()
        return try {
            (engine.voices ?: emptySet()).filter { !it.isNetworkConnectionRequired || true }
                .sortedWith(compareBy({ !(it.locale.language == "hi" || it.locale.language == "en") }, { it.locale.toString() }, { it.name }))
                .map { v ->
                    val female = v.name.contains("female", true) || v.features?.contains("gender=female") == true
                    val label = "${v.locale.displayName}${if (female) " • female" else ""} (${v.name.take(18)})"
                    v.name to label
                }
        } catch (_: Exception) { emptyList() }
    }

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                configureFemaleVoice()
                applyVoiceSettings(userRate, userPitch, userVolume, userVoiceName)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                    }

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        // a real word is being spoken right now: longer words push the orb harder
                        _speakLevel.value = (0.45f + (end - start) / 9f).coerceIn(0.45f, 1f)
                    }

                    override fun onDone(utteranceId: String?) {
                        _speakLevel.value = 0f
                        _voiceState.value = VoiceState.IDLE
                        if (isVoiceModeActive) {
                            // Automatically resume listening if in active voice conversation mode
                            startListening()
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        _voiceState.value = VoiceState.IDLE
                        if (isVoiceModeActive) {
                            startListening()
                        }
                    }
                })
            }
        }
    }

    private fun configureFemaleVoice() {
        val engine = tts ?: return
        // Configure calm, professional, composed female assistant tone
        engine.setPitch(1.02f) // Natural calm pitch
        engine.setSpeechRate(0.96f) // Normal attentive working assistant pace

        // Setup audio attributes so speech plays cleanly in background
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            engine.setAudioAttributes(audioAttributes)
        } catch (_: Exception) {}

        try {
            val hiLocale = Locale("hi", "IN")
            val enInLocale = Locale("en", "IN")
            if (engine.isLanguageAvailable(hiLocale) >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = hiLocale
            } else if (engine.isLanguageAvailable(enInLocale) >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = enInLocale
            } else {
                engine.language = Locale.ENGLISH
            }

            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val femaleVoice = voices.find { v: Voice ->
                    !v.isNetworkConnectionRequired && (
                        v.name.contains("hi-in-x-hie", ignoreCase = true) ||
                        v.name.contains("hi-in", ignoreCase = true) ||
                        v.name.contains("female", ignoreCase = true) ||
                        v.name.contains("en-in-x-ahp", ignoreCase = true) ||
                        v.features?.contains("gender=female") == true
                    )
                } ?: voices.find { v: Voice ->
                    v.locale.language.startsWith("hi") || v.locale.language.startsWith("en")
                }
                if (femaleVoice != null) {
                    engine.voice = femaleVoice
                }
            }
        } catch (_: Exception) {}
    }

    fun startVoiceMode() {
        isVoiceModeActive = true
        _userTranscript.value = ""
        // Greet user in calm, respectful assistant tone
        speak("Main sun rahi hoon. Boliye, kya kaam karna hai?")
    }

    fun stopVoiceMode() {
        isVoiceModeActive = false
        stopListening()
        stopSpeaking()
        _voiceState.value = VoiceState.IDLE
    }

    fun speak(text: String) {
        if (!isTtsReady || tts == null) return
        stopListening()
        // Strip markdown stars, hashes, code blocks for clean verbal speech
        val cleanSpeech = text
            .replace(Regex("```[\\s\\S]*?```"), "Code block generated.")
            .replace(Regex("[#*`_~>\\[\\]|]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanSpeech.isBlank()) return

        _lastSpokenText.value = cleanSpeech
        _voiceState.value = VoiceState.SPEAKING

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "au_agent_speech_${System.currentTimeMillis()}")
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, userVolume.coerceIn(0f, 1f))
        tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, "au_agent_speech")
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun startListening() {
        if (!isVoiceModeActive) return
        stopSpeaking()

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
        }

        val recognizer = speechRecognizer ?: return
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                _micLevel.value = ((rmsdB + 2f) / 10f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.THINKING
            }

            override fun onError(error: Int) {
                _micLevel.value = 0f
                if (isVoiceModeActive) {
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        // Restart listening silently if user was just quiet
                        _voiceState.value = VoiceState.LISTENING
                        startListening()
                    } else {
                        _voiceState.value = VoiceState.IDLE
                    }
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim() ?: ""
                _micLevel.value = 0f
                if (recognized.isNotBlank()) {
                    _userTranscript.value = recognized
                    _voiceState.value = VoiceState.THINKING
                    onUserSpeechResult(recognized)
                } else if (isVoiceModeActive) {
                    startListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim() ?: ""
                if (partial.isNotBlank()) {
                    _userTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            recognizer.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (_: Exception) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _micLevel.value = 0f
    }

    fun release() {
        stopVoiceMode()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
