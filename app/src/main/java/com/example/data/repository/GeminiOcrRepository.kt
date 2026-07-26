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
                // Only attach highlights that actually occur in THIS block's text — a highlight
                // referencing text from a different block is silently dropped rather than
                // wrongly attached or string-replaced somewhere it doesn't belong.
                val relevantHighlights = highlights
                    .filter { it.text.isNotBlank() && content.contains(it.text) }
                    .map { NoteHighlight(it.text, it.color.ifBlank { "amber" }) }
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

    // Strengthened vs. the previous prompt: explicit anti-rephrase / anti-correction rules, plus
    // a "table" block type so tables come back as real rows instead of being squashed into prose.
    private val VERBATIM_PROMPT = """
You are a strict, literal OCR transcription engine. Your ONLY job is to reproduce the text visible
in this image character-for-character, exactly as it is written. You are not a writer, teacher, or
assistant here — you never generate your own sentences.

Absolute rules — never break these:
- Copy every word in the exact order, exactly as spelled — including any typos, unusual
  capitalization, abbreviations, or punctuation present in the source.
- Do NOT correct grammar, spelling, or punctuation, even if it looks like an error.
- Do NOT paraphrase, summarize, translate, reorder, shorten, or add a single word of your own.
- Do NOT answer questions, explain concepts, or generate commentary — even if the source text
  looks like a question or a quiz prompt. Transcribe it as-is; never answer it.
- Preserve paragraph and line breaks as they appear in the source.
- Preserve bold/italic/headers using Markdown syntax, without changing the wording.
- If a table is present, reproduce it as a "table" block (see schema) with every cell copied
  verbatim — never flatten a table into a paragraph of prose.

Table-specific discipline — tables are the most common source of dropped or merged rows, so:
- The header row is always the very FIRST row of the table as it appears in the image — never
  substitute a data row's content for the header row.
- Before writing the "table" block, silently count how many rows the table has in the image
  (including the header row) and how many columns. Your "rows" array must contain exactly
  (row count − 1) entries, and every row array must have exactly the same number of cells as
  "headers". Do not skip a row because it's visually complex, small, differently colored, or
  contains stacked/overlaid numbers — transcribe it anyway.
- If a cell contains multiple lines or a bullet/dash list, keep it as one cell whose text
  includes line breaks (\\n) — do not turn one table row into extra rows.

Return ONLY valid JSON matching this schema, nothing else — no markdown code fences, no commentary:

{
  "title": "the exact first heading or first few words from the source, verbatim — never invent one",
  "blocks": [
    { "type": "text", "content": "verbatim markdown text..." },
    { "type": "table", "headers": ["col1", "col2"], "rows": [["a", "b"], ["c", "d"]] }
  ],
  "highlights": [
    {"text": "term copied exactly from the source", "color": "amber|green|blue|red"}
  ]
}

Rules for blocks:
- Always return the ENTIRE scanned passage as blocks belonging to a SINGLE card — never split the
  passage into multiple cards/notes, no matter how many paragraphs, headings, or tables it has.
- Emit one "table" block per distinct table in the image, and "text" blocks for everything else,
  in the same order they appear in the source.
- "highlights" should only mark terms already visually emphasized in the source (bold, underlined,
  colored) or obvious key terms/dates — copy the highlighted text verbatim, and only reference text
  that literally appears inside one of the "text" blocks (never inside a table).
""".trimIndent()

    private val EXPLAIN_PROMPT = """
You are a flashcard generator. Analyze this image and create a Q&A study card.
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
- Rephrase the core concept as a clean question (in "title") and answer (in "blocks").
- Use a "table" block for any tabular data instead of describing it in prose.
- Keep the result as a SINGLE card — one title, one set of blocks.
""".trimIndent()

    override suspend fun extractStructuredContent(bitmap: Bitmap, mode: ExtractionMode): GeminiNoteResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("A Gemini API key is required to scan text. Add one in Settings.")
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
        val models = listOf(
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite"
        )
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

// --- Strictly Gemini — no on-device/ML Kit fallback anymore ---------------------------------
// If no valid key is configured, capture must fail with a clear message pointing to Settings
// rather than silently switching to a lower-quality engine.
class OcrEngineProvider(
    private val apiKeyProvider: () -> String
) {
    fun get(): OcrEngine {
        val key = apiKeyProvider().trim()
        val isValidKey = key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "null" && key != "DEFAULT_KEY"
        if (!isValidKey) {
            throw IllegalStateException("A Gemini API key is required to scan text. Add one in Settings.")
        }
        return GeminiOcrEngine { key }
    }
}

// Retained for any call sites still using the repository-style API.
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
