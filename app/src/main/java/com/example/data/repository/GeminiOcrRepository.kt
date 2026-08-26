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

enum class ExtractionMode {
    VERBATIM, EXPLAIN
}

// --- Typed content blocks: replaces the old single "back" markdown blob -------------------
// "text"  -> a chunk of verbatim markdown text
// "table" -> a table that appeared in the source, kept as real rows/columns instead of being
//            flattened into prose
data class GeminiBlock(
    val type: String,                       // "text" | "table"
    val content: String? = null,            // used when type == "text"
    val headers: List<String>? = null,      // used when type == "table"
    val rows: List<List<String>>? = null    // used when type == "table"
)

data class GeminiHighlightItem(val text: String, val color: String)

data class GeminiNoteResult(
    val title: String,
    val blocks: List<GeminiBlock>,
    val highlights: List<GeminiHighlightItem> = emptyList()
)

// --- Converts Gemini's typed-block result into the single RevisionNoteEntity -----------------
// No markdown round-trip: blocks stay structured end-to-end. A "table" block's headers/rows are
// stored exactly as Gemini returned them and rendered directly — there is no flatten-to-"|
// a | b |"-text-then-reparse step anymore, which is what caused every row/column bug this file
// went through. Highlights are attached to the specific TextBlock they belong to instead of
// being embedded as "==color:x==term==" markup inside a string.
fun GeminiNoteResult.toSingleNote(deckId: String, easeFactorDefault: Float = 2.5f): RevisionNoteEntity {
    val safeBlocks = blocks.ifEmpty {
        listOf(GeminiBlock(type = "text", content = "No text recognized"))
    }

    val noteBlocks: List<NoteBlock> = safeBlocks.map { block ->
        when (block.type) {
            "table" -> NoteBlock.TableBlock(
                headers = block.headers.orEmpty(),
                rows = block.rows.orEmpty()
            )
            else -> {
                val content = block.content.orEmpty()
                val cleanContent = content.replace(Regex("""[*_`#~]"""), "")
                val relevantHighlights = highlights
                    .filter {
                        val cleanTerm = it.text.replace(Regex("""[*_`#~]"""), "").trim()
                        cleanTerm.isNotBlank() && cleanContent.contains(cleanTerm, ignoreCase = true)
                    }
                    .map { NoteHighlight(it.text.replace(Regex("""[*_`#~]"""), "").trim(), it.color.ifBlank { "amber" }) }
                NoteBlock.TextBlock(content = content, highlights = relevantHighlights)
            }
        }
    }

    val finalTitle = title.ifBlank { "Scanned Card" }
    val nowMs = System.currentTimeMillis()
    val blocksJson = NoteBlocksSerializer.toJson(noteBlocks)
    val plainTextPreview = NoteBlocksSerializer.toPlainTextPreview(noteBlocks)

    return RevisionNoteEntity(
        id = UUID.randomUUID().toString(),
        deckId = deckId,
        title = finalTitle.take(80),
        contentBlocksJson = blocksJson,
        plainTextPreview = plainTextPreview.ifBlank { "No text recognized" },
        createdAt = nowMs,
        updatedAt = nowMs,
        easeFactor = easeFactorDefault,
        intervalDays = 0,
        repetitions = 0,
        dueDate = nowMs
    )
}

interface OcrEngine {
    suspend fun extractStructuredContent(
        bitmap: Bitmap,
        mode: ExtractionMode = ExtractionMode.VERBATIM,
        promptOverride: String? = null,
        temporaryPromptAddendum: String? = null
    ): GeminiNoteResult
}

class GeminiOcrEngine(
    private val apiKeyProvider: () -> String,
    private val selectedModelProvider: () -> String? = { null }
) : OcrEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        val VERBATIM_PROMPT = """
You are an intelligent OCR transcription engine. Unless user instructions specify otherwise, your default job is to reproduce the relevant text visible in this image accurately and preserve structural elements like tables and lists.

General guidelines (unless overridden by specific user instructions):
- Respect user directives: If user instructions specify to focus on, extract, or ignore specific elements (such as focusing only on a table, ignoring surrounding paragraphs, or selecting specific sections), STRICTLY follow those user instructions above all default rules.
- Copy words accurately in the exact order as spelled.
- Preserve paragraph and line breaks.
- Preserve bold/italic/headers using Markdown syntax.
- If a table is present and included in the requested extraction, reproduce it as a "table" block (see schema).

Table-specific discipline (when tables are extracted):
- The header row is always the very FIRST row of the table as it appears in the image.
- Preserve all columns and rows accurately in the "headers" and "rows" arrays.

Return ONLY valid JSON matching this schema, nothing else — no markdown code fences, no commentary:

{
  "title": "a short label (3-8 words) describing what this content IS ABOUT",
  "blocks": [
    { "type": "text", "content": "markdown text..." },
    { "type": "table", "headers": ["col1", "col2"], "rows": [["a", "b"], ["c", "d"]] }
  ],
  "highlights": [
    {"text": "term copied exactly from the source", "color": "amber|green|blue|red"}
  ]
}

Rules for blocks:
- Emit "table" blocks for distinct tables extracted, and "text" blocks for other text requested.
- If user instructions ask to ignore certain parts (e.g. "forget paragraph, focus on table"), ONLY emit blocks for the requested content.
""".trimIndent()

        val EXPLAIN_PROMPT = """
You are a flashcard generator. Analyze this image and create a Q&A study card according to the image content and any specific user instructions.
Return ONLY valid JSON matching this schema, nothing else:

{
  "title": "Short title of the subject",
  "blocks": [
    { "type": "text", "content": "structured answer/explanation in Markdown" },
    { "type": "table", "headers": ["col1", "col2"], "rows": [["a", "b"]] }
  ],
  "highlights": [
    {"text": "key term", "color": "amber|green|blue|red"}
  ]
}

Rules:
- Respect any user instructions provided regarding what content to focus on, ignore, or rephrase.
- Rephrase the core concept as a clean question (in "title") and answer (in "blocks").
- Use a "table" block for any tabular data instead of describing it in prose.
""".trimIndent()
    }

    override suspend fun extractStructuredContent(
        bitmap: Bitmap,
        mode: ExtractionMode,
        promptOverride: String?,
        temporaryPromptAddendum: String?
    ): GeminiNoteResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("A Gemini API key is required to scan text. Add one in Settings.")
        }

        val base64Jpeg = bitmapToBase64(bitmap)
        val basePrompt = promptOverride ?: if (mode == ExtractionMode.EXPLAIN) EXPLAIN_PROMPT else VERBATIM_PROMPT
        val finalPrompt = if (!temporaryPromptAddendum.isNullOrBlank()) {
            """
            HIGH PRIORITY USER DIRECTIVE FOR THIS SCAN:
            "$temporaryPromptAddendum"
            
            CRITICAL REQUIREMENT: The user directive above is your HIGHEST PRIORITY instruction.
            Strictly follow what the user requested above (for example: if the user asks to ignore paragraphs and focus only on tables, or extract specific sections, only include the requested content in the JSON blocks).
            Override any default guidelines below that conflict with the user's directive above.
            
            ---
            SYSTEM EXTRACTION RULES & SCHEMA:
            $basePrompt
            """.trimIndent()
        } else basePrompt

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
                            put("text", finalPrompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                // Critical fix: without this, the API defaults to a much higher temperature and
                // will happily "improve"/rephrase wording even when the prompt asks for verbatim
                // transcription. 0 = as close to deterministic copy-out as the API allows.
                put("temperature", 0.0)
                // Critical fix: with no explicit limit, long tables/lists were getting cut off
                // mid-way (missing rows, missing bullets). This gives enough headroom for a
                // dense scanned page.
                put("max_output_tokens", 8192)
                // Critical fix: the prompt alone wasn't enough to stop the model from hand-
                // formatting its own markdown table inside a "text" block instead of using the
                // "table" block type — it would produce a single garbled pipe-table string with
                // merged/missing cells. A response_schema makes the "table" shape mandatory
                // rather than a suggestion: the API will only accept a "table" block that
                // actually has a headers array and a rows array of arrays.
                put("response_schema", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("title", JSONObject().apply { put("type", "STRING") })
                        put("blocks", JSONObject().apply {
                            put("type", "ARRAY")
                            put("items", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("type", JSONObject().apply {
                                        put("type", "STRING")
                                        put("enum", JSONArray().apply { put("text"); put("table") })
                                    })
                                    put("content", JSONObject().apply { put("type", "STRING") })
                                    put("headers", JSONObject().apply {
                                        put("type", "ARRAY")
                                        put("items", JSONObject().apply { put("type", "STRING") })
                                    })
                                    put("rows", JSONObject().apply {
                                        put("type", "ARRAY")
                                        put("items", JSONObject().apply {
                                            put("type", "ARRAY")
                                            put("items", JSONObject().apply { put("type", "STRING") })
                                        })
                                    })
                                })
                                put("required", JSONArray().apply { put("type") })
                            })
                        })
                        put("highlights", JSONObject().apply {
                            put("type", "ARRAY")
                            put("items", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("text", JSONObject().apply { put("type", "STRING") })
                                    put("color", JSONObject().apply { put("type", "STRING") })
                                })
                            })
                        })
                    })
                    put("required", JSONArray().apply { put("title"); put("blocks") })
                })
            })
        }

        // Current, real, GA model IDs only (checked against Google's live model list — the
        // previous version of this file guessed at some IDs that don't exist, which silently
        // burned through 404 fallbacks before landing on a weaker/older model).
        val baseModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.5-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-2.0-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite"
        )
        val selected = selectedModelProvider()?.trim().orEmpty()
        val models = if (selected.isNotBlank()) {
            listOf(selected) + baseModels.filter { it != selected }
        } else {
            baseModels
        }
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
        val blocksArray = obj.optJSONArray("blocks") ?: JSONArray()

        val blocksList = mutableListOf<GeminiBlock>()
        for (i in 0 until blocksArray.length()) {
            val item = blocksArray.getJSONObject(i)
            when (item.optString("type", "text")) {
                "table" -> {
                    val headersArr = item.optJSONArray("headers") ?: JSONArray()
                    val headers = (0 until headersArr.length()).map { headersArr.optString(it, "") }

                    val rowsArr = item.optJSONArray("rows") ?: JSONArray()
                    val rows = (0 until rowsArr.length()).map { r ->
                        val rowArr = rowsArr.optJSONArray(r) ?: JSONArray()
                        (0 until rowArr.length()).map { c -> rowArr.optString(c, "") }
                    }
                    blocksList.add(GeminiBlock(type = "table", headers = headers, rows = rows))
                }
                else -> {
                    blocksList.add(GeminiBlock(type = "text", content = item.optString("content", "")))
                }
            }
        }

        val highlightsArray = obj.optJSONArray("highlights") ?: JSONArray()
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

        return GeminiNoteResult(title = title, blocks = blocksList, highlights = highlightsList)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

// --- OCR Engine Provider: Supports Google ML Kit on-device and Gemini AI Cloud ---
class OcrEngineProvider(
    private val apiKeyProvider: () -> String,
    private val selectedModelProvider: () -> String? = { null }
) {
    private val mlKitEngine by lazy { MlKitOcrEngine() }

    fun get(choice: OcrEngineChoice = OcrEngineChoice.ML_KIT): OcrEngine {
        return when (choice) {
            OcrEngineChoice.ML_KIT -> mlKitEngine
            OcrEngineChoice.GEMINI -> {
                val key = apiKeyProvider().trim()
                val isValidKey = key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "null" && key != "DEFAULT_KEY"
                if (!isValidKey) {
                    // Fall back cleanly to ML Kit on-device if Gemini key is missing
                    mlKitEngine
                } else {
                    GeminiOcrEngine(apiKeyProvider = { key }, selectedModelProvider = selectedModelProvider)
                }
            }
        }
    }

    fun hasValidGeminiKey(): Boolean {
        val key = apiKeyProvider().trim()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "null" && key != "DEFAULT_KEY"
    }
}

// Retained for any call sites still using the repository-style API.
class GeminiOcrRepository(
    private val getApiKey: () -> String
) {
    private val engine = GeminiOcrEngine(apiKeyProvider = getApiKey)

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

data class GeminiTitleResult(val title: String)

class GeminiTitleEngine(
    private val apiKeyProvider: () -> String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val TITLE_PROMPT = """
Look at (or listen to) this material and write a short title (3-8 words) describing what it is
about — its subject or topic. Do not transcribe or summarize the content itself, just name it.
Return ONLY valid JSON: {"title": "..."}
""".trimIndent()

    suspend fun generateTitle(mediaBytes: ByteArray, mimeType: String): GeminiTitleResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("A Gemini API key is required. Add one in Settings.")
        }
        val base64Data = Base64.encodeToString(mediaBytes, Base64.NO_WRAP)
        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", mimeType)
                                put("data", base64Data)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", TITLE_PROMPT)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.2)
                put("response_schema", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("title", JSONObject().apply { put("type", "STRING") })
                    })
                    put("required", JSONArray().apply { put("title") })
                })
            })
        }

        val models = listOf("gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-2.5-flash", "gemini-2.5-flash-lite")
        var lastException: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(requestBody).build()
                val response = client.newCall(request).execute()
                val responseBodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 404 && model != models.last()) continue
                    val friendlyError = try {
                        JSONObject(responseBodyStr).optJSONObject("error")?.optString("message") ?: responseBodyStr
                    } catch (e: Exception) { responseBodyStr }
                    val msg = when (code) {
                        401, 403 -> "Invalid Gemini API Key. Please verify your key in Settings."
                        429 -> "Gemini API rate limit reached. Please try again shortly."
                        else -> "Gemini API Error ($code): $friendlyError"
                    }
                    throw IllegalStateException(msg)
                }

                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates") ?: throw IllegalStateException("No candidates in response")
                if (candidates.length() == 0) throw IllegalStateException("Empty candidates array")
                val textResult = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "") ?: ""
                val cleanJson = textResult.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val title = JSONObject(cleanJson).optString("title", "Local Media Card")
                return@withContext GeminiTitleResult(title = title)
            } catch (e: Exception) {
                lastException = e
                if (e is IllegalStateException && e.message?.contains("Invalid Gemini API Key") == true) throw e
            }
        }
        throw lastException ?: IllegalStateException("Failed to generate title with Gemini API.")
    }
}

enum class GeminiModelTestStatus {
    UNTESTED, TESTING, APPROVED, FAILED
}

suspend fun verifyGeminiModel(apiKey: String, model: String): Boolean = withContext(Dispatchers.IO) {
    val cleanKey = apiKey.trim()
    if (cleanKey.isBlank() || cleanKey == "MY_GEMINI_API_KEY") return@withContext false
    val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    return@withContext try {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "ping") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("max_output_tokens", 1)
            })
        }
        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()
        val response = client.newCall(request).execute()
        val isSuccessful = response.isSuccessful
        response.close()
        isSuccessful
    } catch (e: Exception) {
        false
    }
}

