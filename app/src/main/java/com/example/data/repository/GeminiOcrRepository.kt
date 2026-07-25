package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.db.entity.RevisionNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class GeminiHighlightItem(val text: String, val color: String)
data class GeminiNoteItem(val front: String, val back: String, val highlights: List<GeminiHighlightItem>)
data class GeminiNoteResult(val title: String, val notes: List<GeminiNoteItem>)

class GeminiOcrRepository(
    private val getApiKey: () -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun extractNotesFromImage(bitmap: Bitmap, deckId: String): List<RevisionNoteEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isEmpty()) {
            throw IllegalArgumentException("Clé API Gemini non configurée. Veuillez ajouter votre clé API dans la section Configuration de l'application.")
        }

        val base64Jpeg = bitmapToBase64(bitmap)

        val prompt = """
You are an OCR-to-flashcard converter. Read the text in this image (it may be a book page or a screenshot).
Return ONLY valid JSON matching this schema, nothing else:

{
  "title": "short descriptive title",
  "notes": [
    {
      "front": "question or key term",
      "back": "answer or explanation, in Markdown",
      "highlights": [
        {"text": "term to highlight", "color": "amber|green|blue|red"}
      ]
    }
  ]
}

Rules:
- Split distinct concepts/definitions into separate notes (like Anki cards), don't dump everything into one.
- Preserve emphasis (bold/italic/headers) as Markdown in the "back" field.
- Use "highlights" for key terms, definitions, or dates that deserve a colored highlight.
- If the image contains a single continuous passage with no clear Q/A structure, create one note with the passage summarized as "front" and full text as "back".
- Do not invent information not present in the image.
""".trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Jpeg)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            throw IllegalStateException("Gemini API Error (${response.code}): $errBody")
        }

        val responseBodyStr = response.body?.string() ?: throw IllegalStateException("Empty response from Gemini API")
        val responseJson = JSONObject(responseBodyStr)

        val candidates = responseJson.optJSONArray("candidates")
            ?: throw IllegalStateException("No candidates found in Gemini response")

        if (candidates.length() == 0) {
            throw IllegalStateException("Empty candidates list from Gemini")
        }

        val content = candidates.getJSONObject(0).optJSONObject("content")
            ?: throw IllegalStateException("No content object in candidate")

        val parts = content.optJSONArray("parts")
            ?: throw IllegalStateException("No parts found in content")

        val textResult = parts.getJSONObject(0).optString("text", "")
        if (textResult.isEmpty()) {
            throw IllegalStateException("Empty text content returned by Gemini")
        }

        val parsedResult = parseGeminiResponse(textResult)

        val nowMs = System.currentTimeMillis()
        parsedResult.notes.map { noteItem ->
            var formattedBack = noteItem.back
            noteItem.highlights.forEach { h ->
                if (h.text.isNotBlank()) {
                    val colorTag = h.color.ifBlank { "amber" }
                    val replacement = "==color:$colorTag==${h.text}=="
                    if (formattedBack.contains(h.text)) {
                        formattedBack = formattedBack.replace(h.text, replacement)
                    } else {
                        formattedBack += "\n\n$replacement"
                    }
                }
            }

            RevisionNoteEntity(
                id = UUID.randomUUID().toString(),
                deckId = deckId,
                title = if (noteItem.front.isNotBlank()) noteItem.front else parsedResult.title,
                contentMarkdown = formattedBack,
                createdAt = nowMs,
                updatedAt = nowMs,
                easeFactor = 2.5f,
                intervalDays = 0,
                repetitions = 0,
                dueDate = nowMs // immediately due for review
            )
        }
    }

    private fun parseGeminiResponse(jsonString: String): GeminiNoteResult {
        // Strip code fencing if present
        val cleanJson = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val obj = JSONObject(cleanJson)
        val title = obj.optString("title", "Fiche de révision")
        val notesArray = obj.optJSONArray("notes") ?: JSONArray()

        val notesList = mutableListOf<GeminiNoteItem>()
        for (i in 0 until notesArray.length()) {
            val item = notesArray.getJSONObject(i)
            val front = item.optString("front", "")
            val back = item.optString("back", "")
            val highlightsArray = item.optJSONArray("highlights") ?: JSONArray()

            val highlightsList = mutableListOf<GeminiHighlightItem>()
            for (j in 0 until highlightsArray.length()) {
                val h = highlightsArray.getJSONObject(j)
                highlightsList.add(
                    GeminiHighlightItem(
                        text = h.optString("text", ""),
                        color = h.optString("color", "amber")
                    )
                )
            }
            notesList.add(GeminiNoteItem(front, back, highlightsList))
        }

        return GeminiNoteResult(title, notesList)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to JPEG with high quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
