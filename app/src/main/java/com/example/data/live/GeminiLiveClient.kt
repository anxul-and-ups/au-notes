package com.example.data.live

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal Gemini Live (BidiGenerateContent) client: streams 16 kHz PCM up, receives 24 kHz PCM back,
 * transcripts, interruptions and native function calls. Adapted from the JARVIS project's websocket.
 */
class GeminiLiveClient(
    private val apiKey: String,
    private val model: String,
    private val voiceName: String,
    private val systemPrompt: String,
    private val tools: JSONArray?,
    private val listener: Listener
) {
    interface Listener {
        fun onReady()
        fun onAudio(pcm24k: ByteArray)
        fun onInputText(chunk: String)
        fun onOutputText(chunk: String)
        fun onInterrupted()
        fun onTurnComplete()
        fun onToolCall(id: String, name: String, args: JSONObject)
        fun onError(message: String, fatal: Boolean)
        fun onClosed()
    }

    companion object {
        const val MODEL_FLASH_LIVE = "models/gemini-3.1-flash-live-preview"
        const val MODEL_LIVE_38 = "models/gemini-3.8-live"
        val VOICES = listOf("Aoede" to "Aoede (female)", "Kore" to "Kore (female)", "Leda" to "Leda (female)", "Zephyr" to "Zephyr (female)",
            "Puck" to "Puck (male)", "Charon" to "Charon (male)", "Fenrir" to "Fenrir (male)", "Orus" to "Orus (male)")
        private const val URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(8, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    private var ws: WebSocket? = null
    private val ready = AtomicBoolean(false)
    private val closedByUs = AtomicBoolean(false)

    fun isReady() = ready.get()

    fun connect() {
        if (apiKey.isBlank()) { listener.onError("Gemini API key is missing. Add it in API Room.", true); return }
        closedByUs.set(false)
        ws = client.newWebSocket(Request.Builder().url("$URL?key=$apiKey").build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { sendSetup(webSocket) }
            override fun onMessage(webSocket: WebSocket, text: String) = handle(text)
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) = handle(bytes.utf8())
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(1000, null) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                ready.set(false)
                if (!closedByUs.get()) {
                    val keyProblem = reason.contains("API key", true) || reason.contains("API_KEY", true)
                    listener.onError(
                        if (keyProblem) "Google rejected the API key." else "Live voice closed ($code): ${reason.ifBlank { "no reason" }}",
                        fatal = true
                    )
                }
                listener.onClosed()
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ready.set(false)
                if (closedByUs.get()) return
                val code = response?.code ?: 0
                val msg = when (code) {
                    400, 401, 403 -> "Google rejected the API key or model (HTTP $code)."
                    429 -> "Gemini quota / rate limit reached (HTTP 429)."
                    else -> "Live voice connection failed: ${t.message ?: "network error"}"
                }
                listener.onError(msg, true)
                listener.onClosed()
            }
        })
    }

    private fun sendSetup(socket: WebSocket) {
        val setup = JSONObject().apply {
            put("model", model)
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("generation_config", JSONObject().apply {
                put("response_modalities", JSONArray().put("AUDIO"))
                put("speech_config", JSONObject().put("voice_config",
                    JSONObject().put("prebuilt_voice_config", JSONObject().put("voice_name", voiceName))))
                put("temperature", 0.7)
            })
            put("input_audio_transcription", JSONObject())
            put("output_audio_transcription", JSONObject())
            if (tools != null && tools.length() > 0) {
                put("tools", JSONArray().put(JSONObject().put("functionDeclarations", tools)))
            }
        }
        socket.send(JSONObject().put("setup", setup).toString())
    }

    private fun handle(text: String) {
        val json = try { JSONObject(text) } catch (e: Exception) { return }
        if (json.has("setupComplete")) { ready.set(true); listener.onReady(); return }
        if (!ready.get() && (json.has("serverContent") || json.has("toolCall"))) { ready.set(true); listener.onReady() }

        json.optJSONObject("toolCall")?.optJSONArray("functionCalls")?.let { calls ->
            for (i in 0 until calls.length()) {
                val c = calls.getJSONObject(i)
                listener.onToolCall(c.optString("id"), c.optString("name"), c.optJSONObject("args") ?: JSONObject())
            }
        }
        val sc = json.optJSONObject("serverContent") ?: return
        sc.optJSONObject("inputTranscription")?.optString("text")?.takeIf { it.isNotEmpty() }?.let { listener.onInputText(it) }
        sc.optJSONObject("outputTranscription")?.optString("text")?.takeIf { it.isNotEmpty() }?.let { listener.onOutputText(it) }
        if (sc.optBoolean("interrupted", false)) listener.onInterrupted()
        sc.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
            for (i in 0 until parts.length()) {
                val data = parts.getJSONObject(i).optJSONObject("inlineData")?.optString("data").orEmpty()
                if (data.length > 8) {
                    val pcm = Base64.decode(data, Base64.DEFAULT)
                    val even = pcm.size and 1.inv()
                    if (even >= 2) listener.onAudio(if (even == pcm.size) pcm else pcm.copyOf(even))
                }
            }
        }
        if (sc.optBoolean("turnComplete", false) || sc.optBoolean("generationComplete", false)) listener.onTurnComplete()
    }

    fun sendAudio(pcm16k: ByteArray) {
        if (!ready.get() || pcm16k.isEmpty()) return
        val b64 = Base64.encodeToString(pcm16k, Base64.NO_WRAP)
        ws?.send("{\"realtimeInput\":{\"audio\":{\"mimeType\":\"audio/pcm;rate=16000\",\"data\":\"$b64\"}}}")
    }

    fun sendImage(jpeg: ByteArray) {
        if (!ready.get() || jpeg.isEmpty()) return
        val b64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
        ws?.send("{\"realtimeInput\":{\"video\":{\"mimeType\":\"image/jpeg\",\"data\":\"$b64\"}}}")
    }

    fun sendText(text: String) {
        if (!ready.get() || text.isBlank()) return
        ws?.send(JSONObject().put("realtimeInput", JSONObject().put("text", text)).toString())
    }

    fun sendToolResponse(id: String, name: String, result: JSONObject) {
        val msg = JSONObject().put("tool_response", JSONObject().put("function_responses",
            JSONArray().put(JSONObject().put("id", id).put("name", name).put("response", result))))
        ws?.send(msg.toString())
    }

    fun close() {
        closedByUs.set(true)
        ready.set(false)
        try { ws?.close(1000, "bye") } catch (_: Exception) {}
        ws = null
    }
}
