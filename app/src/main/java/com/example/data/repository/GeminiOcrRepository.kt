package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.data.db.entity.RevisionNoteEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ExtractionMode {
    VERBATIM, EXPLAIN
}

data class GeminiHighlightItem(val text: String, val color: String)
data class GeminiNoteItem(val front: String, val back: String, val highlights: List<GeminiHighlightItem>)
data class GeminiNoteResult(val title: String, val notes: List<GeminiNoteItem>)

// Defensive single card converter
fun GeminiNoteResult.toSingleNote(deckId: String, easeFactorDefault: Float = 2.5f): RevisionNoteEntity {
    val merged = if (notes.size > 1) {
        GeminiNoteItem(
            front = notes.first().front.ifBlank { title },
            back = notes.joinToString("\n\n") { it.back },
            highlights = notes.flatMap { it.highlights }
        )
    } else notes.firstOrNull() ?: GeminiNoteItem(
        front = title.ifBlank { "Scanned Card" },
        back = "No text recognized",
        highlights = emptyList()
    )

    var formattedBack = merged.back
    merged.highlights.forEach { h ->
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

    val nowMs = System.currentTimeMillis()
    val finalTitle = if (merged.front.isNotBlank()) merged.front else title.ifBlank { "Scanned Card" }

    return RevisionNoteEntity(
        id = UUID.randomUUID().toString(),
        deckId = deckId,
        title = finalTitle.take(80),
        contentMarkdown = formattedBack,
        createdAt = nowMs,
        updatedAt = nowMs,
        easeFactor = easeFactorDefault,
        intervalDays = 0,
        repetitions = 0,
        dueDate = nowMs
    )
}

interface OcrEngine {
    suspend fun extractStructuredContent(bitmap: Bitmap, mode: ExtractionMode = ExtractionMode.VERBATIM): GeminiNoteResult
}

class GeminiOcrEngine(
    private val apiKeyProvider: () -> String
) : OcrEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val VERBATIM_PROMPT = """
You are an OCR converter. Transcribe the text from this image VERBATIM.
Return ONLY valid JSON matching this schema, nothing else:

{
  "title": "Short title (first line or heading of the text)",
  "notes": [
    {
      "front": "First line or heading",
      "back": "Full transcribed text verbatim in Markdown format",
      "highlights": []
    }
  ]
}

Rules:
- Extract the text exactly as written in the image.
- Do NOT rewrite, explain, summarize, or rephrase.
- Keep the entire extracted text in a SINGLE note in the "notes" array.
""".trimIndent()

    private val EXPLAIN_PROMPT = """
You are a flashcard generator. Analyze this image and create a Q&A study card.
Return ONLY valid JSON matching this schema, nothing else:

{
  "title": "Short title of the subject",
  "notes": [
    {
      "front": "Main concept question or title",
      "back": "Structured answer and explanation in Markdown",
      "highlights": [
        {"text": "key term", "color": "amber|green|blue|red"}
      ]
    }
  ]
}

Rules:
- Rephrase the core concept as a clean question and answer.
- Preserve key definitions and details in Markdown.
- Keep the result in a SINGLE note in the "notes" array.
""".trimIndent()

    override suspend fun extractStructuredContent(bitmap: Bitmap, mode: ExtractionMode): GeminiNoteResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("Gemini API key is missing or invalid.")
        }

        val base64Jpeg = bitmapToBase64(bitmap)
        val prompt = if (mode == ExtractionMode.EXPLAIN) EXPLAIN_PROMPT else VERBATIM_PROMPT

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

        val models = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        var lastException: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 404 && model != models.last()) {
                        continue
                    }
                    val friendlyError = try {
                        val errObj = JSONObject(responseBodyStr).optJSONObject("error")
                        errObj?.optString("message") ?: responseBodyStr
                    } catch (e: Exception) { responseBodyStr }

                    val msg = when (code) {
                        401, 403 -> "Invalid Gemini API Key. Please verify your key in Settings."
                        429 -> "Gemini API rate limit reached. Please try again shortly."
                        else -> "Gemini API Error ($code): $friendlyError"
                    }
                    throw IllegalStateException(msg)
                }

                if (responseBodyStr.isBlank()) {
                    throw IllegalStateException("Empty response received from Gemini API.")
                }

                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                    ?: throw IllegalStateException("No candidates found in Gemini API response.")

                if (candidates.length() == 0) {
                    throw IllegalStateException("Gemini generated an empty response.")
                }

                val content = candidates.getJSONObject(0).optJSONObject("content")
                    ?: throw IllegalStateException("No content object in candidate.")

                val parts = content.optJSONArray("parts")
                    ?: throw IllegalStateException("No parts found in Gemini response.")

                val textResult = parts.getJSONObject(0).optString("text", "")
                if (textResult.isEmpty()) {
                    throw IllegalStateException("Gemini returned empty text content.")
                }

                return@withContext parseGeminiResponse(textResult)
            } catch (e: Exception) {
                lastException = e
                if (e is IllegalStateException && e.message?.contains("Invalid Gemini API Key") == true) {
                    throw e
                }
            }
        }

        throw lastException ?: IllegalStateException("Failed to process image with Gemini API.")
    }

    private fun parseGeminiResponse(jsonString: String): GeminiNoteResult {
        val cleanJson = jsonString.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val obj = JSONObject(cleanJson)
        val title = obj.optString("title", "Scanned Card")
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

class MlKitOcrEngine(private val context: Context) : OcrEngine {
    override suspend fun extractStructuredContent(bitmap: Bitmap, mode: ExtractionMode): GeminiNoteResult =
        suspendCancellableCoroutine { cont ->
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val fullText = visionText.text.trim()
                        if (fullText.isBlank()) {
                            cont.resumeWithException(IllegalStateException("No readable text detected in the image. Try selecting a clearer area."))
                            return@addOnSuccessListener
                        }
                        val firstLine = visionText.textBlocks.firstOrNull()?.lines?.firstOrNull()?.text?.trim()
                            ?: fullText.lineSequence().firstOrNull().orEmpty()

                        val title = if (firstLine.isNotBlank()) firstLine.take(60) else "Scanned Card"
                        val front = if (firstLine.isNotBlank()) firstLine else title
                        val back = fullText

                        cont.resume(
                            GeminiNoteResult(
                                title = title,
                                notes = listOf(
                                    GeminiNoteItem(
                                        front = front,
                                        back = back,
                                        highlights = emptyList()
                                    )
                                )
                            )
                        ) {}
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("MlKitOcrEngine", "ML Kit OCR failed", e)
                        val msg = when {
                            e.message?.contains("download", ignoreCase = true) == true ->
                                "On-device OCR model is downloading. Connect to Wi-Fi/Internet or enter a Gemini API Key in Settings."
                            else ->
                                e.localizedMessage ?: e.message ?: "On-device OCR failed"
                        }
                        cont.resumeWithException(IllegalStateException(msg, e))
                    }
            } catch (e: Exception) {
                android.util.Log.e("MlKitOcrEngine", "Failed to start ML Kit OCR", e)
                cont.resumeWithException(IllegalStateException("Failed to initialize OCR: ${e.localizedMessage ?: e.message}", e))
            }
        }
}

class OcrEngineProvider(
    private val apiKeyProvider: () -> String,
    private val context: Context
) {
    fun get(): OcrEngine {
        val key = apiKeyProvider().trim()
        val isValidKey = key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "null" && key != "DEFAULT_KEY"
        return if (isValidKey) GeminiOcrEngine { key } else MlKitOcrEngine(context)
    }
}

// Retain GeminiOcrRepository class for backward compatibility if needed elsewhere
class GeminiOcrRepository(
    private val getApiKey: () -> String
) {
    private val engine = GeminiOcrEngine(getApiKey)

    suspend fun extractNotesFromImage(
        bitmap: Bitmap,
        deckId: String,
        explainMode: Boolean = false
    ): List<RevisionNoteEntity> {
        val mode = if (explainMode) ExtractionMode.EXPLAIN else ExtractionMode.VERBATIM
        val result = engine.extractStructuredContent(bitmap, mode)
        return listOf(result.toSingleNote(deckId))
    }
}
