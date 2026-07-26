package com.example.data.repository

import org.json.JSONArray
import org.json.JSONObject

// --- The structured content model --------------------------------------------------------
// This is what gets stored (as JSON) and rendered directly — no markdown string round-trip,
// no line-scanning, no pipe-boundary guessing. A "table" block's headers/rows are exactly the
// arrays Gemini returned; nothing is ever flattened into "| a | b |" text and reparsed.
sealed class NoteBlock {
    data class TextBlock(
        val content: String,
        val highlights: List<NoteHighlight> = emptyList()
    ) : NoteBlock()

    data class TableBlock(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : NoteBlock()
}

data class NoteHighlight(val text: String, val color: String)

// --- JSON (de)serialization, using org.json to match the rest of this file's style --------
object NoteBlocksSerializer {

    fun toJson(blocks: List<NoteBlock>): String {
        val array = JSONArray()
        blocks.forEach { block ->
            array.put(
                when (block) {
                    is NoteBlock.TextBlock -> JSONObject().apply {
                        put("type", "text")
                        put("content", block.content)
                        put("highlights", JSONArray().apply {
                            block.highlights.forEach { h ->
                                put(JSONObject().apply {
                                    put("text", h.text)
                                    put("color", h.color)
                                })
                            }
                        })
                    }
                    is NoteBlock.TableBlock -> JSONObject().apply {
                        put("type", "table")
                        put("headers", JSONArray(block.headers))
                        put("rows", JSONArray().apply {
                            block.rows.forEach { row -> put(JSONArray(row)) }
                        })
                    }
                }
            )
        }
        return array.toString()
    }

    fun fromJson(json: String): List<NoteBlock> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                when (obj.optString("type", "text")) {
                    "table" -> {
                        val headersArr = obj.optJSONArray("headers") ?: JSONArray()
                        val headers = (0 until headersArr.length()).map { headersArr.optString(it, "") }
                        val rowsArr = obj.optJSONArray("rows") ?: JSONArray()
                        val rows = (0 until rowsArr.length()).map { r ->
                            val rowArr = rowsArr.optJSONArray(r) ?: JSONArray()
                            (0 until rowArr.length()).map { c -> rowArr.optString(c, "") }
                        }
                        NoteBlock.TableBlock(headers, rows)
                    }
                    else -> {
                        val highlightsArr = obj.optJSONArray("highlights") ?: JSONArray()
                        val highlights = (0 until highlightsArr.length()).map { h ->
                            val hObj = highlightsArr.getJSONObject(h)
                            NoteHighlight(hObj.optString("text", ""), hObj.optString("color", "amber"))
                        }
                        NoteBlock.TextBlock(obj.optString("content", ""), highlights)
                    }
                }
            }
        } catch (e: Exception) {
            // Malformed/corrupt JSON — surface as a single visible text block instead of
            // crashing the render or silently showing nothing.
            listOf(NoteBlock.TextBlock(content = "[Could not load this card's content]"))
        }
    }

    // Flattened text used ONLY for search/export — never for rendering. Tables become a simple
    // "header: cell, cell" line per row so the content is still searchable/readable in a raw
    // export, without pretending to be a real table.
    fun toPlainTextPreview(blocks: List<NoteBlock>): String {
        return blocks.joinToString("\n\n") { block ->
            when (block) {
                is NoteBlock.TextBlock -> block.content
                is NoteBlock.TableBlock -> {
                    val headerLine = block.headers.joinToString(" | ")
                    val rowLines = block.rows.joinToString("\n") { it.joinToString(" | ") }
                    "$headerLine\n$rowLines"
                }
            }
        }.trim()
    }
}
