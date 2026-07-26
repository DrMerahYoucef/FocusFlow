package com.example.data.repository

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiQuestionResult(val title: String, val question: String)

enum class CaptureMediaKind(val mimeType: String) {
    IMAGE_JPEG("image/jpeg"),
    AUDIO_M4A("audio/mp4")
}

class GeminiQuestionEngine(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String = { "gemini-3.5-flash" }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val QUESTION_PROMPT = """
You are a study-flashcard assistant. Look at (or listen to) this material and write exactly ONE
short, specific study question that tests whether someone has understood or memorized it. Do not
answer the question — the learner will look at the original photo or listen to the original
recording again to check themselves.

Rules:
- Ask about the single most important/testable fact, concept, or relationship in the material.
- Keep the question under 25 words, in the same language as the material.
- Do not restate the whole content as a question ("What does this image show?") — be specific
  ("What is the mechanism of an inter-ligamentaire fracture?").
- "title" is a short 3-6 word label for this card, not the full question.

Return ONLY valid JSON, nothing else:
{
  "title": "short label",
  "question": "the study question"
}
""".trimIndent()

    suspend fun generateQuestion(mediaBytes: ByteArray, mediaKind: CaptureMediaKind): GeminiQuestionResult =
        withContext(Dispatchers.IO) {
            val apiKey = apiKeyProvider().trim()
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                throw IllegalStateException("A Gemini API key is required to generate a question. Add one in Settings.")
            }

            val base64Data = Base64.encodeToString(mediaBytes, Base64.NO_WRAP)

            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", mediaKind.mimeType)
                                    put("data", base64Data)
                                })
                            })
                            put(JSONObject().apply { put("text", QUESTION_PROMPT) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.4)
                    put("max_output_tokens", 512)
                    put("response_schema", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("title", JSONObject().apply { put("type", "STRING") })
                            put("question", JSONObject().apply { put("type", "STRING") })
                        })
                        put("required", JSONArray().apply { put("title"); put("question") })
                    })
                })
            }

            val selectedModel = modelProvider().trim().ifBlank { "gemini-3.5-flash" }
            val models = listOf(
                selectedModel,
                "gemini-3.5-flash",
                "gemini-2.5-flash",
                "gemini-1.5-flash",
                "gemini-2.0-flash"
            ).distinct()
            var lastException: Exception? = null

            for (model in models) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder().url(url).post(requestBody).build()
                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        if (response.code == 404 && model != models.last()) continue
                        val msg = when (response.code) {
                            401, 403 -> "Invalid Gemini API Key. Please verify your key in Settings."
                            429 -> "Gemini API rate limit reached. Please try again shortly."
                            else -> "Gemini API Error (${response.code}): $bodyStr"
                        }
                        throw IllegalStateException(msg)
                    }

                    val text = JSONObject(bodyStr)
                        .getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                        .optString("text", "")
                    if (text.isEmpty()) throw IllegalStateException("Gemini returned an empty response.")

                    val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val obj = JSONObject(clean)
                    return@withContext GeminiQuestionResult(
                        title = obj.optString("title", "Card"),
                        question = obj.optString("question", "")
                    )
                } catch (e: Exception) {
                    lastException = e
                    if (e is IllegalStateException && e.message?.contains("Invalid Gemini API Key") == true) throw e
                }
            }
            throw lastException ?: IllegalStateException("Failed to generate a question.")
        }

    suspend fun testActiveModel(modelOverride: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("No API key provided. Please save a key in Settings."))
        }

        val targetModel = (modelOverride ?: modelProvider()).trim().ifBlank { "gemini-3.5-flash" }

        val testPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Hi, reply with status ok.") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("max_output_tokens", 10)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"
        try {
            val requestBody = testPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Result.success("Model '$targetModel' is active and verified!")
            } else {
                val errorMsg = try {
                    val errorObj = JSONObject(bodyStr).optJSONObject("error")
                    val msg = errorObj?.optString("message", "") ?: bodyStr
                    val code = response.code
                    "Error ($code): $msg"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $bodyStr"
                }
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
