package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ProviderCheckResult(
    val ok: Boolean,
    val statusCode: Int,
    val message: String,
    val models: List<String> = emptyList()
)

/**
 * Real HTTP connectivity & model detection for every provider in the API Room.
 * Strictly queries the live provider endpoints with zero hardcoded model lists.
 */
object ApiConnectivityChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    fun isOnline(context: android.content.Context): Boolean {
        return try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                ?: return true
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun check(
        providerName: String,
        apiKey: String,
        baseUrl: String = ""
    ): ProviderCheckResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim().replace(Regex("\\s+"), "")
        if (cleanKey.isBlank()) {
            return@withContext ProviderCheckResult(false, 0, "API key is empty. Paste a valid API key first.")
        }

        try {
            when (providerName.trim().lowercase()) {
                "gemini", "google", "google gemini" -> checkGemini(cleanKey)
                "openai", "chatgpt" -> checkBearerModels("https://api.openai.com/v1/models", cleanKey)
                "deepseek" -> checkBearerModels("https://api.deepseek.com/models", cleanKey)
                "grok", "xai", "grok (xai)" -> checkBearerModels("https://api.x.ai/v1/models", cleanKey)
                "groq" -> checkBearerModels("https://api.groq.com/openai/v1/models", cleanKey)
                "openrouter" -> checkBearerModels("https://openrouter.ai/api/v1/models", cleanKey)
                "claude", "anthropic", "claude (anthropic)" -> checkAnthropic(cleanKey)
                else -> {
                    val rawBase = baseUrl.trim().trimEnd('/')
                    if (rawBase.isBlank()) {
                        ProviderCheckResult(false, 0, "Base URL is required for custom provider '$providerName'.")
                    } else {
                        val normalizedBase = rawBase
                            .removeSuffix("/chat/completions")
                            .removeSuffix("/v1/chat/completions")
                            .removeSuffix("/models")
                            .trimEnd('/')
                        val primaryEndpoint = "$normalizedBase/models"
                        val firstTry = checkBearerModels(primaryEndpoint, cleanKey)
                        if (!firstTry.ok && !normalizedBase.endsWith("/v1")) {
                            val secondTry = checkBearerModels("$normalizedBase/v1/models", cleanKey)
                            if (secondTry.ok) secondTry else firstTry
                        } else {
                            firstTry
                        }
                    }
                }
            }
        } catch (e: java.net.UnknownHostException) {
            ProviderCheckResult(false, 0, "No internet connection or host unreachable (${e.message}).")
        } catch (e: java.net.SocketTimeoutException) {
            ProviderCheckResult(false, 0, "Connection timed out. Check your internet connection.")
        } catch (e: Exception) {
            ProviderCheckResult(false, 0, "Network error: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun checkGemini(apiKey: String): ProviderCheckResult {
        val allModels = mutableListOf<String>()
        var nextPageToken = ""
        var lastCode = 200

        while (true) {
            val url = buildString {
                append("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000&key=")
                append(apiKey)
                if (nextPageToken.isNotBlank()) {
                    append("&pageToken=").append(nextPageToken)
                }
            }
            val req = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                lastCode = resp.code
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = try {
                        JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
                    } catch (_: Exception) { "" }
                    return ProviderCheckResult(
                        ok = false,
                        statusCode = resp.code,
                        message = if (errMsg.isNotBlank()) "Google rejected the key (HTTP ${resp.code}): $errMsg"
                        else "Google rejected the key (HTTP ${resp.code}). Verify the key in Google AI Studio."
                    )
                }

                val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val arr = json.optJSONArray("models")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val rawName = obj.optString("name", "").removePrefix("models/").trim()
                        if (rawName.isBlank()) continue
                        val methods = obj.optJSONArray("supportedGenerationMethods")
                        var supportsGen = methods == null || methods.length() == 0
                        if (methods != null) {
                            for (m in 0 until methods.length()) {
                                val method = methods.optString(m, "")
                                if (
                                    method.equals("generateContent", ignoreCase = true) ||
                                    method.equals("streamGenerateContent", ignoreCase = true) ||
                                    method.equals("bidiGenerateContent", ignoreCase = true)
                                ) {
                                    supportsGen = true
                                    break
                                }
                            }
                        }
                        if (supportsGen) {
                            allModels.add(rawName)
                        }
                    }
                }
                nextPageToken = json.optString("nextPageToken", "").trim()
                if (nextPageToken.isBlank()) break
            }
        }

        val distinctModels = allModels
            .distinct()
            .sortedWith(
                compareByDescending<String> { it.startsWith("gemini-", ignoreCase = true) }
                    .thenByDescending { it.contains("pro", ignoreCase = true) || it.contains("flash", ignoreCase = true) }
                    .thenByDescending { it }
            )

        return if (distinctModels.isEmpty()) {
            ProviderCheckResult(false, lastCode, "Key responded (HTTP $lastCode) but returned 0 models.")
        } else {
            ProviderCheckResult(
                ok = true,
                statusCode = lastCode,
                message = "Connected (HTTP $lastCode) - ${distinctModels.size} models detected.",
                models = distinctModels
            )
        }
    }

    private fun checkBearerModels(url: String, apiKey: String): ProviderCheckResult {
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val errMsg = try {
                    JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
                } catch (_: Exception) { "" }
                return ProviderCheckResult(
                    ok = false,
                    statusCode = resp.code,
                    message = if (errMsg.isNotBlank()) "Rejected (HTTP ${resp.code}): $errMsg"
                    else "Rejected by server (HTTP ${resp.code}). Check that the API key and Base URL are valid."
                )
            }

            val models = mutableListOf<String>()
            try {
                val trimmed = body.trim()
                if (trimmed.startsWith("[")) {
                    val arr = org.json.JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        val id = (obj?.optString("id")?.ifBlank { obj.optString("name") } ?: arr.optString(i)).orEmpty().trim()
                        if (id.isNotBlank()) models.add(id.removePrefix("models/"))
                    }
                } else {
                    val root = JSONObject(trimmed)
                    val arr = root.optJSONArray("data") ?: root.optJSONArray("models") ?: root.optJSONArray("items")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i)
                            val id = (obj?.optString("id")?.ifBlank { obj.optString("name") } ?: arr.optString(i)).orEmpty().trim()
                            if (id.isNotBlank()) models.add(id.removePrefix("models/"))
                        }
                    }
                }
            } catch (_: Exception) { }

            val distinct = models.distinct().sorted()
            return if (distinct.isEmpty()) {
                ProviderCheckResult(false, resp.code, "Server returned HTTP ${resp.code} but no model list.")
            } else {
                ProviderCheckResult(
                    ok = true,
                    statusCode = resp.code,
                    message = "Connected (HTTP ${resp.code}) - ${distinct.size} models detected.",
                    models = distinct
                )
            }
        }
    }

    private fun checkAnthropic(apiKey: String): ProviderCheckResult {
        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/models")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val errMsg = try {
                    JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
                } catch (_: Exception) { "" }
                return ProviderCheckResult(
                    ok = false,
                    statusCode = resp.code,
                    message = if (errMsg.isNotBlank()) "Anthropic rejected key (HTTP ${resp.code}): $errMsg"
                    else "Anthropic rejected key (HTTP ${resp.code})."
                )
            }

            val models = mutableListOf<String>()
            try {
                val arr = JSONObject(body).optJSONArray("data")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val id = arr.optJSONObject(i)?.optString("id").orEmpty().trim()
                        if (id.isNotBlank()) models.add(id)
                    }
                }
            } catch (_: Exception) { }

            val distinct = models.distinct()
            return if (distinct.isEmpty()) {
                ProviderCheckResult(false, resp.code, "Anthropic returned HTTP ${resp.code} but 0 models.")
            } else {
                ProviderCheckResult(
                    ok = true,
                    statusCode = resp.code,
                    message = "Connected (HTTP ${resp.code}) - ${distinct.size} Claude models detected.",
                    models = distinct
                )
            }
        }
    }
}
