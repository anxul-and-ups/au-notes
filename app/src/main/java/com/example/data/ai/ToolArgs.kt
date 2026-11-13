package com.example.data.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini sends tool arguments as declared (arrays/objects often arrive as JSON *strings*, and some
 * parameter names differ from what the handlers read). This normalises them once, before a tool runs:
 *  - JSON passed as a string (items, slides_json, patch, ...) becomes a real JSONArray / JSONObject
 *  - slides_json -> slides, chart_type -> type, update_chart.patch -> top-level fields
 */
object ToolArgs {
    private val JSON_KEYS = setOf("items", "slides_json", "slides", "data", "patch", "rows", "header", "bars")

    fun normalize(name: String, raw: JSONObject): JSONObject {
        val a = JSONObject(raw.toString())
        val keys = ArrayList<String>()
        val it = a.keys()
        while (it.hasNext()) keys.add(it.next())
        for (k in keys) {
            if (k !in JSON_KEYS) continue
            val v = a.opt(k)
            if (v is String) parse(v)?.let { parsed -> a.put(k, parsed) }
        }
        when (name) {
            "create_slide_deck", "create_html_slides" -> {
                val sj = a.opt("slides_json")
                if (!a.has("slides")) {
                    when (sj) {
                        is JSONArray -> a.put("slides", sj)
                        is JSONObject -> sj.optJSONArray("slides")?.let { a.put("slides", it) }
                    }
                }
                if (sj is JSONObject && a.optString("title").isBlank()) {
                    sj.optString("title").takeIf { it.isNotBlank() }?.let { a.put("title", it) }
                }
            }
            "add_chart", "update_chart" -> {
                if (name == "update_chart") {
                    (a.opt("patch") as? JSONObject)?.let { p ->
                        val pk = p.keys()
                        while (pk.hasNext()) {
                            val k = pk.next()
                            if (!a.has(k)) a.put(k, p.get(k))
                        }
                    }
                }
                if (!a.has("type")) a.optString("chart_type").takeIf { it.isNotBlank() }?.let { a.put("type", it) }
                a.optString("type").lowercase().let { t ->
                    val fixed = when (t) { "bars", "barchart", "bar chart", "column" -> "bar"; "donut" -> "pie"; else -> t }
                    if (fixed != t) a.put("type", fixed)
                }
                if (!a.has("items")) (a.opt("data") as? JSONArray)?.let { a.put("items", it) }
            }
        }
        return a
    }

    private fun parse(s: String): Any? {
        var t = s.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```json").removePrefix("```JSON").removePrefix("```").removeSuffix("```").trim()
        }
        val firstBrack = t.indexOf('[')
        val firstBrace = t.indexOf('{')
        if (firstBrack >= 0 && (firstBrace < 0 || firstBrack < firstBrace)) {
            val lastBrack = t.lastIndexOf(']')
            if (lastBrack > firstBrack) {
                t = t.substring(firstBrack, lastBrack + 1)
            }
        } else if (firstBrace >= 0) {
            val lastBrace = t.lastIndexOf('}')
            if (lastBrace > firstBrace) {
                t = t.substring(firstBrace, lastBrace + 1)
            }
        }
        val cleaned = t.replace(Regex(",\\s*([}\\]])"), "$1")

        return runCatching {
            when {
                cleaned.startsWith("[") -> JSONArray(cleaned)
                cleaned.startsWith("{") -> JSONObject(cleaned)
                else -> null
            }
        }.getOrNull() ?: runCatching {
            when {
                t.startsWith("[") -> JSONArray(t)
                t.startsWith("{") -> JSONObject(t)
                else -> null
            }
        }.getOrNull()
    }
}
