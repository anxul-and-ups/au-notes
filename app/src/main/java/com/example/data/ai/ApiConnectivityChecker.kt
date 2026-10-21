package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Item 26: Model Detector — makes a REAL request to the provider's API using
 * the user's key and reports back what actually happened (active/inactive,
 * connection status, available models), rather than a hardcoded "ACTIVE".
 */
data class ConnectivityResult(
    val isActive: Boolean,
    val statusMessage: String,
    val availableModels: List<String> = emptyList(),
    val rawInfo: String = ""
)

object ApiConnectivityChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Pulls the provider's own error text out of a JSON error body when there is one. */
    private fun errorText(body: String): String = try {
        val o = JSONObject(body)
        val e = o.opt("error")
        when (e) {
            is JSONObject -> e.optString("message")
            is String -> e
            else -> o.optString("message")
        }
    } catch (ex: Exception) { "" }

    suspend fun check(providerNameRaw: String, apiKeyRaw: String, baseUrlRaw: String = "", model: String = ""): ConnectivityResult =
        withContext(Dispatchers.IO) {
            val providerName = providerNameRaw
            val apiKey = apiKeyRaw.trim()
            var baseUrl = baseUrlRaw.trim()
            if (baseUrl.isNotBlank() && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "https://$baseUrl"
            }
            if (apiKey.isBlank() && providerName != "Custom") {
                return@withContext ConnectivityResult(false, "No API key entered.")
            }
            try {
                when (providerName) {
                    "Gemini" -> checkGemini(apiKey)
                    "OpenAI" -> checkOpenAiCompatible("https://api.openai.com/v1/models", apiKey)
                    "DeepSeek" -> {
                        val res = checkOpenAiCompatible("https://api.deepseek.com/models", apiKey)
                        if (!res.isActive) checkOpenAiCompatible("https://api.deepseek.com/v1/models", apiKey) else res
                    }
                    "Kimi" -> checkOpenAiCompatible("https://api.moonshot.cn/v1/models", apiKey)
                    "Anthropic" -> checkAnthropic(apiKey)
                    "Hugging Face" -> checkHuggingFace(apiKey)
                    "OpenCode", "Custom" -> {
                        if (baseUrl.isBlank()) {
                            ConnectivityResult(false, "Enter a base URL / endpoint (e.g. https://api.openai.com/v1) to test this provider.")
                        } else {
                            val cleanBase = baseUrl.trimEnd('/')
                            val url = if (cleanBase.endsWith("/models")) cleanBase else "$cleanBase/models"
                            checkOpenAiCompatible(url, apiKey)
                        }
                    }
                    else -> ConnectivityResult(false, "Unknown provider.")
                }
            } catch (e: Exception) {
                ConnectivityResult(false, "Connection failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }

    private fun checkGemini(apiKey: String): ConnectivityResult {
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000&key=$apiKey")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return ConnectivityResult(false, "Gemini rejected the key (HTTP ${resp.code}). ${errorText(body)}".trim(), rawInfo = body.take(300))
            }
            val models = try {
                val arr = JSONObject(body).optJSONArray("models") ?: JSONArray()
                (0 until arr.length()).mapNotNull { i ->
                    val m = arr.optJSONObject(i) ?: return@mapNotNull null
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    val canGenerate = methods == null || (0 until methods.length()).any { methods.optString(it) == "generateContent" }
                    if (canGenerate) m.optString("name").removePrefix("models/") else null
                }.filter { it.isNotBlank() }
            } catch (e: Exception) { emptyList() }
            return ConnectivityResult(true, "Gemini API is active.", models)
        }
    }

    /** Works for any OpenAI-compatible /v1/models endpoint: OpenAI itself,
     * DeepSeek, Moonshot/Kimi, and any custom OpenAI-compatible base URL. */
    private fun checkOpenAiCompatible(modelsUrl: String, apiKey: String): ConnectivityResult {
        val request = Request.Builder()
            .url(modelsUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AU-Notes/1.0")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val err = errorText(body)
                val msg = when (resp.code) {
                    401 -> "Invalid API Key or unauthorized (HTTP 401). $err"
                    429 -> "Rate limit / Quota exceeded (HTTP 429). Ensure your account balance is > $0. $err"
                    403 -> "Forbidden (HTTP 403). Check model permissions. $err"
                    404 -> "Models endpoint not found (HTTP 404). Check Base URL. $err"
                    else -> "Request rejected (HTTP ${resp.code}). ${err.ifBlank { "Check API key & balance." }}"
                }
                return ConnectivityResult(false, msg.trim(), rawInfo = body.take(300))
            }
            val models = try {
                val arr = JSONObject(body).optJSONArray("data") ?: JSONArray()
                (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.optString("id") }
            } catch (e: Exception) { emptyList() }
            return ConnectivityResult(true, "Connected successfully.", models)
        }
    }

    private fun checkAnthropic(apiKey: String): ConnectivityResult {
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/models?limit=1000")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AU-Notes/1.0")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val err = errorText(body)
                val msg = when (resp.code) {
                    401 -> "Anthropic rejected the key (HTTP 401). $err"
                    429 -> "Anthropic quota exceeded (HTTP 429). Check credit balance. $err"
                    else -> "Anthropic error (HTTP ${resp.code}). $err"
                }
                return ConnectivityResult(false, msg.trim(), rawInfo = body.take(300))
            }
            val models = try {
                val arr = JSONObject(body).optJSONArray("data") ?: JSONArray()
                (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.optString("id") }
            } catch (e: Exception) { emptyList() }
            return ConnectivityResult(true, "Anthropic API is active.", models)
        }
    }

    private fun checkHuggingFace(apiKey: String): ConnectivityResult {
        // Hugging Face has no single "list my models" endpoint; whoami-v2
        // is the standard way to verify a token actually works.
        val request = Request.Builder()
            .url("https://huggingface.co/api/whoami-v2")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                return ConnectivityResult(false, "Hugging Face rejected the token (HTTP ${resp.code}).", rawInfo = body.take(300))
            }
            val name = try { JSONObject(body).optString("name", "verified account") } catch (e: Exception) { "verified account" }
            // Token works — also pull real, popular text-generation models from the Hub.
            val models = try {
                val listReq = Request.Builder()
                    .url("https://huggingface.co/api/models?pipeline_tag=text-generation&sort=downloads&direction=-1&limit=40")
                    .header("Authorization", "Bearer $apiKey")
                    .get().build()
                client.newCall(listReq).execute().use { r ->
                    val arr = JSONArray(r.body?.string() ?: "[]")
                    (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.optString("id") }
                }
            } catch (e: Exception) { emptyList() }
            return ConnectivityResult(true, "Token is valid — authenticated as $name.", models)
        }
    }
}
