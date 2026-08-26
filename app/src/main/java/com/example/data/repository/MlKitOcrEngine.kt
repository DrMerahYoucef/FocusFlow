package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

enum class OcrEngineChoice {
    ML_KIT,
    GEMINI
}

/**
 * On-device OCR engine powered by Google ML Kit Text Recognition (Latin / French script).
 * Completely offline / unbundled model that operates without requiring a Gemini API key.
 * Specially tuned to recognize French accents, quotes, and flashcard structures (titles, definitions, lists).
 */
class MlKitOcrEngine : OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extractStructuredContent(
        bitmap: Bitmap,
        mode: ExtractionMode,
        promptOverride: String?,
        temporaryPromptAddendum: String?
    ): GeminiNoteResult = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val visionText = suspendCancellableCoroutine<Text> { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }

        parseVisionText(visionText, temporaryPromptAddendum, mode)
    }

    private fun parseVisionText(
        visionText: Text,
        userTitleHint: String?,
        mode: ExtractionMode
    ): GeminiNoteResult {
        val rawText = visionText.text.trim()
        if (rawText.isBlank()) {
            return GeminiNoteResult(
                title = userTitleHint?.ifBlank { null } ?: "Carte de Révision",
                blocks = listOf(
                    GeminiBlock(
                        type = "text",
                        content = "Aucun texte détecté dans l'image sélectionnée. Veuillez vérifier le cadrage ou l'éclairage de votre document."
                    )
                ),
                highlights = emptyList()
            )
        }

        val textBlocks = visionText.textBlocks
        val highlights = mutableListOf<GeminiHighlightItem>()
        val parsedBlocks = mutableListOf<GeminiBlock>()

        // 1. Identify Candidate Title
        var extractedTitle = userTitleHint?.trim().orEmpty()
        var titleFoundFromImage = false

        if (extractedTitle.isBlank()) {
            // Find the top-most prominent line or block to serve as card title
            val sortedBlocks = textBlocks.sortedBy { it.boundingBox?.top ?: 0 }
            val firstBlock = sortedBlocks.firstOrNull()
            val firstLine = firstBlock?.lines?.firstOrNull()?.text?.trim().orEmpty()

            if (firstLine.isNotBlank() && firstLine.length <= 90) {
                extractedTitle = cleanTitle(firstLine)
                titleFoundFromImage = true
            } else if (firstLine.isNotBlank()) {
                extractedTitle = cleanTitle(firstLine.take(60) + "...")
                titleFoundFromImage = true
            } else {
                extractedTitle = "Carte de Révision"
            }
        }

        // 2. Table Detection: check if multiple blocks share aligned Y or X coordinates
        val tableBlock = tryExtractTable(textBlocks)
        if (tableBlock != null) {
            parsedBlocks.add(tableBlock)
        }

        // 3. Process Text Content and Structure (Headings, Bullets, Definitions, French Accents)
        val contentMarkdownBuilder = StringBuilder()
        var isFirstLineInDoc = true

        for (block in textBlocks) {
            val blockLines = block.lines
            for (line in blockLines) {
                val lineText = line.text.trim()
                if (lineText.isBlank()) continue

                // Skip repeating the exact title if it was extracted from the very first line
                if (titleFoundFromImage && isFirstLineInDoc && lineText.equals(extractedTitle, ignoreCase = true)) {
                    isFirstLineInDoc = false
                    continue
                }
                isFirstLineInDoc = false

                // Process French formatting:
                // Check if heading (Roman numerals, Chapter, All Caps short, etc.)
                val headingPrefix = detectFrenchHeadingPrefix(lineText)
                if (headingPrefix != null) {
                    if (contentMarkdownBuilder.isNotEmpty() && !contentMarkdownBuilder.endsWith("\n\n")) {
                        contentMarkdownBuilder.append("\n\n")
                    }
                    contentMarkdownBuilder.append("$headingPrefix $lineText\n\n")
                    continue
                }

                // Check if list / bullet point
                val bulletPrefix = detectBulletPrefix(lineText)
                if (bulletPrefix != null) {
                    contentMarkdownBuilder.append("$lineText\n")
                    // Extract term if definition style in bullet (e.g. "- Terme : Définition")
                    extractDefinitionHighlight(lineText, highlights)
                    continue
                }

                // Check if definition format (e.g. "Photosynthèse : Processus de fabrication...")
                if (lineText.contains(":") && !lineText.startsWith("http")) {
                    val colonIndex = lineText.indexOf(':')
                    val term = lineText.substring(0, colonIndex).trim()
                    val definition = lineText.substring(colonIndex + 1).trim()
                    if (term.isNotBlank() && term.length in 2..45 && definition.isNotBlank()) {
                        highlights.add(GeminiHighlightItem(text = term, color = "amber"))
                        contentMarkdownBuilder.append("**$term** : $definition\n\n")
                        continue
                    }
                }

                // Standard paragraph line
                contentMarkdownBuilder.append("$lineText\n")
            }
            contentMarkdownBuilder.append("\n")
        }

        val cleanMarkdown = contentMarkdownBuilder.toString().trim()
        if (cleanMarkdown.isNotBlank()) {
            parsedBlocks.add(
                0,
                GeminiBlock(
                    type = "text",
                    content = cleanMarkdown
                )
            )
        }

        // 4. Extract highlights from French quotes (« » or " ") and important keywords
        extractFrenchKeywords(rawText, highlights)

        if (parsedBlocks.isEmpty()) {
            parsedBlocks.add(GeminiBlock(type = "text", content = rawText))
        }

        return GeminiNoteResult(
            title = extractedTitle.ifBlank { "Carte de Révision" },
            blocks = parsedBlocks,
            highlights = highlights.distinctBy { it.text.lowercase(Locale.ROOT) }
        )
    }

    private fun cleanTitle(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .removePrefix("##")
            .removePrefix("###")
            .removePrefix("-")
            .removePrefix("•")
            .removePrefix("*")
            .trim()
            .trim(':', '-', '.', '—', '–')
            .trim()
    }

    private fun detectFrenchHeadingPrefix(line: String): String? {
        val trimmed = line.trim()
        val upper = trimmed.uppercase(Locale.FRENCH)

        // French roman numeral or lesson headers: "I. INTRODUCTION", "1. DÉFINITION", "CHAPITRE 1"
        if (Regex("""^(I|II|III|IV|V|VI|VII|VIII|IX|X)\.\s+.*""").matches(upper)) {
            return "##"
        }
        if (Regex("""^(CHAPITRE|SECTION|PARTIE|LEÇON|MODULE|THEME|THÈME)\s+\d+.*""").matches(upper)) {
            return "#"
        }
        if (Regex("""^(DÉFINITION|DEFINITION|PROPRIÉTÉ|PROPRIETE|THÉORÈME|THEOREME|EXEMPLE|REMARQUE|VOCABULAIRE|RÈGLE|REGLE)\s*(:|-)?.*""").matches(upper) && trimmed.length < 50) {
            return "###"
        }
        // Short ALL-CAPS lines often represent section titles in French textbooks/cards
        if (trimmed.length in 4..35 && upper == trimmed && !trimmed.contains(":") && !trimmed.endsWith(".")) {
            return "##"
        }

        return null
    }

    private fun detectBulletPrefix(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ") ||
            trimmed.startsWith("– ") || trimmed.startsWith("— ") || trimmed.startsWith("> ")
        ) {
            return trimmed.take(2)
        }
        if (Regex("""^\d+[\.\)]\s+.*""").matches(trimmed)) {
            return "numbered"
        }
        if (Regex("""^[a-zA-Z][\.\)]\s+.*""").matches(trimmed)) {
            return "letter"
        }
        return null
    }

    private fun extractDefinitionHighlight(line: String, highlights: MutableList<GeminiHighlightItem>) {
        if (line.contains(":")) {
            val stripped = line.trim().trimStart('•', '-', '*', '–', '—', '>', ' ').trim()
            val colonIndex = stripped.indexOf(':')
            if (colonIndex in 2..40) {
                val candidate = stripped.substring(0, colonIndex).trim()
                if (candidate.isNotBlank() && !candidate.contains("http")) {
                    highlights.add(GeminiHighlightItem(text = candidate, color = "amber"))
                }
            }
        }
    }

    private fun extractFrenchKeywords(text: String, highlights: MutableList<GeminiHighlightItem>) {
        // French quotation marks: « mot clé »
        val guillemetRegex = Regex("""«\s*([^»]{2,45})\s*»""")
        guillemetRegex.findAll(text).forEach { match ->
            val term = match.groupValues[1].trim()
            if (term.isNotBlank()) {
                highlights.add(GeminiHighlightItem(text = term, color = "green"))
            }
        }

        // Standard quotes: "mot clé"
        val quoteRegex = Regex(""""([^"\n]{2,35})"""")
        quoteRegex.findAll(text).forEach { match ->
            val term = match.groupValues[1].trim()
            if (term.isNotBlank()) {
                highlights.add(GeminiHighlightItem(text = term, color = "blue"))
            }
        }
    }

    private fun tryExtractTable(blocks: List<Text.TextBlock>): GeminiBlock? {
        if (blocks.size < 4) return null

        // If blocks are arranged in apparent columns (multiple blocks sharing horizontal/vertical bands)
        val sortedByY = blocks.sortedBy { it.boundingBox?.top ?: 0 }
        val rows = mutableListOf<List<String>>()
        var currentY = -1
        var currentRow = mutableListOf<Text.TextBlock>()

        for (block in sortedByY) {
            val top = block.boundingBox?.top ?: 0
            if (currentY == -1 || abs(top - currentY) < 30) {
                currentRow.add(block)
                currentY = top
            } else {
                if (currentRow.size >= 2) {
                    val rowText = currentRow.sortedBy { it.boundingBox?.left ?: 0 }.map { it.text.trim() }
                    rows.add(rowText)
                }
                currentRow = mutableListOf(block)
                currentY = top
            }
        }
        if (currentRow.size >= 2) {
            val rowText = currentRow.sortedBy { it.boundingBox?.left ?: 0 }.map { it.text.trim() }
            rows.add(rowText)
        }

        if (rows.size >= 2) {
            val headerRow = rows.first()
            val dataRows = rows.drop(1)
            val maxCols = rows.maxOf { it.size }
            if (maxCols >= 2) {
                val normalizedHeaders = headerRow.take(maxCols).let {
                    if (it.size < maxCols) it + List(maxCols - it.size) { colIdx -> "Col ${colIdx + 1}" } else it
                }
                val normalizedRows = dataRows.map { r ->
                    if (r.size < maxCols) r + List(maxCols - r.size) { "" } else r.take(maxCols)
                }
                return GeminiBlock(
                    type = "table",
                    headers = normalizedHeaders,
                    rows = normalizedRows
                )
            }
        }
        return null
    }
}
