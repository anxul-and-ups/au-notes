package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.screens.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage>,
    val attachmentName: String? = null,
    val attachmentText: String? = null,
    val attachmentImages: List<String> = emptyList(),
    val attachmentUri: String? = null
)

class ChatSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("au_ai_chat_sessions", Context.MODE_PRIVATE)

    fun loadAllSessions(): List<ChatSession> {
        val raw = prefs.getString("sessions_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "Conversation")
                val timestamp = obj.optLong("timestamp", obj.optLong("updatedAt", System.currentTimeMillis()))
                val msgArray = obj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgArray.length()) {
                    val mObj = msgArray.getJSONObject(j)
                    val sender = mObj.optString("sender", mObj.optString("role", "user")).let {
                        if (it == "model" || it == "assistant") "bot" else it
                    }
                    val text = mObj.optString("text", mObj.optString("content", ""))
                    messages.add(
                        ChatMessage(
                            id = mObj.optString("id", UUID.randomUUID().toString()),
                            sender = sender,
                            text = text,
                            attachedFileName = mObj.optString("attachedFileName", "").ifBlank { null },
                            timestamp = mObj.optLong("timestamp", timestamp)
                        )
                    )
                }
                val imgArr = obj.optJSONArray("attachmentImages") ?: JSONArray()
                val imgs = mutableListOf<String>()
                for (k in 0 until imgArr.length()) {
                    val b64 = imgArr.optString(k)
                    if (b64.isNotBlank()) imgs.add(b64)
                }
                list.add(
                    ChatSession(
                        id = id,
                        title = title,
                        timestamp = timestamp,
                        messages = messages,
                        attachmentName = obj.optString("attachmentName", "").ifBlank { null },
                        attachmentText = obj.optString("attachmentText", "").ifBlank { null },
                        attachmentImages = imgs,
                        attachmentUri = obj.optString("attachmentUri", "").ifBlank { null }
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSession(session: ChatSession) {
        val existing = loadAllSessions().toMutableList()
        val index = existing.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            existing[index] = session
        } else {
            existing.add(0, session)
        }
        persistSessions(existing.take(30))
    }

    fun deleteSession(sessionId: String) {
        val existing = loadAllSessions().filter { it.id != sessionId }
        persistSessions(existing)
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        val existing = loadAllSessions().map {
            if (it.id == sessionId) it.copy(title = clean) else it
        }
        persistSessions(existing)
    }

    fun clearAllSessions() {
        prefs.edit().remove("sessions_json").apply()
    }

    private fun persistSessions(sessions: List<ChatSession>) {
        val array = JSONArray()
        for (session in sessions) {
            val obj = JSONObject()
            obj.put("id", session.id)
            obj.put("title", session.title)
            obj.put("timestamp", session.timestamp)
            session.attachmentName?.let { obj.put("attachmentName", it) }
            session.attachmentText?.let { obj.put("attachmentText", it.take(40_000)) }
            session.attachmentUri?.let { obj.put("attachmentUri", it) }
            if (session.attachmentImages.isNotEmpty()) {
                val imgArr = JSONArray()
                session.attachmentImages.take(4).forEach { imgArr.put(it) }
                obj.put("attachmentImages", imgArr)
            }

            val msgArray = JSONArray()
            for (m in session.messages) {
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("sender", m.sender)
                mObj.put("text", m.text)
                m.attachedFileName?.let { mObj.put("attachedFileName", it) }
                mObj.put("timestamp", m.timestamp)
                msgArray.put(mObj)
            }
            obj.put("messages", msgArray)
            array.put(obj)
        }
        prefs.edit().putString("sessions_json", array.toString()).apply()
    }
}
