package com.example.data.ai

import android.content.Context
import com.example.ui.screens.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage>
)

/**
 * Manages persistent storage of AI chat sessions.
 * Saved chats survive app restarts and can be switched or deleted from the AU Agent sidebar.
 */
class ChatSessionManager(private val context: Context) {
    private val sessionsFile: File
        get() = File(context.filesDir, "ai_chat_sessions.json")

    fun loadAllSessions(): List<ChatSession> {
        return try {
            val file = sessionsFile
            if (!file.exists()) return emptyList()
            val content = file.readText()
            if (content.isBlank()) return emptyList()
            val array = JSONArray(content)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "Chat Session")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val msgsArray = obj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgsArray.length()) {
                    val m = msgsArray.getJSONObject(j)
                    messages.add(
                        ChatMessage(
                            id = m.optString("id", UUID.randomUUID().toString()),
                            sender = m.optString("sender", "bot"),
                            text = m.optString("text", ""),
                            attachedFileName = m.optString("attachedFileName", "").ifBlank { null },
                            timestamp = m.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                list.add(ChatSession(id, title, timestamp, messages))
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSession(session: ChatSession) {
        val existing = loadAllSessions().toMutableList()
        val index = existing.indexOfFirst { it.id == session.id }
        if (index != -1) {
            existing[index] = session
        } else {
            existing.add(0, session)
        }
        persist(existing)
    }

    fun deleteSession(sessionId: String) {
        val existing = loadAllSessions().filter { it.id != sessionId }
        persist(existing)
    }

    private fun persist(sessions: List<ChatSession>) {
        try {
            val array = JSONArray()
            for (s in sessions) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("title", s.title)
                obj.put("timestamp", s.timestamp)
                val msgsArray = JSONArray()
                for (m in s.messages) {
                    val mObj = JSONObject()
                    mObj.put("id", m.id)
                    mObj.put("sender", m.sender)
                    mObj.put("text", m.text)
                    mObj.put("attachedFileName", m.attachedFileName ?: "")
                    mObj.put("timestamp", m.timestamp)
                    msgsArray.put(mObj)
                }
                obj.put("messages", msgsArray)
                array.put(obj)
            }
            sessionsFile.writeText(array.toString())
        } catch (_: Exception) {}
    }
}
