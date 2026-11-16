package com.example.data.ai

/**
 * Decides, per message and instantly (no extra AI call), whether a live web search is worth it.
 * Search mode ON = AUTO: greetings, chit-chat, app actions and things the model already knows are answered
 * directly; fresh / live / explicit "search this" questions trigger a search.
 */
object SearchRouter {
    private val greeting = Regex(
        "^\\s*(hi+|hii+|hello+|helo+|hey+|heyy+|yo|namaste|namaskar|good\\s*(morning|afternoon|evening|night)|gm|gn|" +
            "thanks?(\\s*you)?|thank\\s*you|thx|ok(ay)?|okk+|k|kk|haan+|han|ha|hmm+|nahi|nahin|no|yes|yep|yup|bye|cool|nice|great|wow|" +
            "accha|acha|theek\\s*hai|thik\\s*hai|shukriya|dhanyawad|kaise\\s*ho|kya\\s*haal\\s*hai|what'?s\\s*up|sup|test|testing)" +
            "[\\s!.?,]*$",
        RegexOption.IGNORE_CASE
    )

    private val url = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)

    // The user clearly asks for a search
    private val explicit = Regex(
        "\\b(search\\s+(karo|kar|kr|for|this|it|about|the|on|web)|web\\s*search|google\\s+(karo|kar|par|pe|it|this)|look\\s*up|" +
            "dhundh|dhoondh|khoj|internet\\s*(par|pe|se|me|mein)|net\\s*(par|pe|se)|online\\s*(dekh|check|search)|" +
            "search\\s*mode|check\\s*online|verify\\s*(online|karo))\\b",
        RegexOption.IGNORE_CASE
    )

    // Things that change over time or need live data
    private val fresh = Regex(
        "\\b(latest|newest|current(ly)?|todays?|tonight|tomorrow|yesterday|right\\s*now|recent(ly)?|breaking|news|headlines?|" +
            "aaj|abhi|kal\\s*ka|taaza|taza|naya|nayi|khabar|samachar|" +
            "price|prices|rate|rates|bhav|bhaav|kimat|keemat|cost\\s*of|kitne\\s*ka|kitna\\s*hai|" +
            "score|scores|live\\s*score|ipl|cricket|world\\s*cup|fifa|winner|" +
            "weather|mausam|temperature|forecast|stock|stocks|share\\s*price|nifty|sensex|bitcoin|crypto|exchange\\s*rate|dollar|petrol|diesel|gold\\s*rate|" +
            "released?|launched?|announced?|new\\s*model|version|benchmark|trending|viral|ranking|rankings|" +
            "election|who\\s+is\\s+(the\\s+)?(current\\s+)?(ceo|president|prime\\s*minister|pm|cm|chief\\s*minister|captain|coach)|" +
            "202[4-9]|this\\s*(week|month|year)|kya\\s*chal\\s*raha|kya\\s*hua|kab\\s*(hai|hoga|aayega|release)|" +
            "available\\s*(now|in)|in\\s*stock|opening\\s*hours|near\\s*me|nearby)\\b",
        RegexOption.IGNORE_CASE
    )

    /** True when the user explicitly asks to search / look something up online. */
    fun isExplicitSearch(message: String): Boolean = explicit.containsMatchIn(message) || url.containsMatchIn(message)

    fun needsSearch(message: String): Boolean {
        val t = message.trim()
        if (t.isBlank()) return false
        if (greeting.matches(t)) return false
        if (url.containsMatchIn(t)) return true
        if (explicit.containsMatchIn(t)) return true
        return fresh.containsMatchIn(t)
    }
}
