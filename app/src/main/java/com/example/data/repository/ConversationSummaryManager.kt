package com.example.data.repository

import android.content.Context
import com.example.util.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ConversationSummaryManager {

    suspend fun summarizePendingThreads(context: Context) {
        val apiKey = SecureStorage.getGeminiApiKey(context) ?: return
        if (apiKey.isBlank()) return

        val pendingThreads = ConversationRepository.threads.value.filter { 
            it.aiSummary == null && it.messages.isNotEmpty() 
        }
        if (pendingThreads.isEmpty()) return

        withContext(Dispatchers.IO) {
            for (thread in pendingThreads) {
                try {
                    val summary = requestGeminiSummary(apiKey, thread)
                    if (!summary.isNullOrBlank()) {
                        ConversationRepository.updateSummary(thread.packageName, thread.sender, summary)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun requestGeminiSummary(apiKey: String, thread: ConversationThread): String {
        val prompt = """
            Here are the recent messages from a conversation with ${thread.sender} on ${thread.appName}:

            ${thread.messages.joinToString("\n") { "- $it" }}

            In 1-2 short sentences, summarize what this person is talking about and the overall context/topic of the conversation. Be concise and neutral, just the gist.
        """.trimIndent()

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
        }

        conn.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val responseString = conn.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseString)
            return responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP $responseCode: $err")
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
