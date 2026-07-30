package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.repository.ImportMode
import com.example.data.repository.RevisionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class CardTypeFilter(val label: String) {
    ALL("All Cards (Text, Image, Audio)"),
    TEXT("Text & Markdown Cards Only"),
    IMAGE("Photo & Image Cards Only"),
    AUDIO("Voice & Audio Cards Only")
}

data class BackupRestoreResult(
    val notesCount: Int,
    val decksCount: Int,
    val mediaCount: Int,
    val summaryText: String
)

object CardBackupEngine {

    private const val TAG = "CardBackupEngine"
    private const val MANIFEST_FILE_NAME = "backup_manifest.json"

    /**
     * Creates a .focuscards ZIP backup containing filtered notes, decks, and media files.
     */
    suspend fun createBackup(
        context: Context,
        repository: RevisionRepository,
        cardTypeFilter: CardTypeFilter,
        deckIdFilter: String, // "ALL" or specific deck id
        outputFile: File,
        onProgress: (progress: Float, status: String) -> Unit
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        onProgress(0.05f, "Gathering cards and decks...")

        val allDecks = repository.getAllDecksOnce()
        val allNotes = repository.getAllNotesOnce()

        // Filter notes
        val filteredNotes = allNotes.filter { note ->
            val matchesDeck = deckIdFilter == "ALL" || note.deckId == deckIdFilter
            val matchesType = when (cardTypeFilter) {
                CardTypeFilter.ALL -> true
                CardTypeFilter.TEXT -> note.mediaType == null && note.mediaFilePath.isNullOrBlank()
                CardTypeFilter.IMAGE -> note.mediaType == "IMAGE" || (!note.mediaFilePath.isNullOrBlank() && isImagePath(note.mediaFilePath))
                CardTypeFilter.AUDIO -> note.mediaType == "AUDIO" || (!note.mediaFilePath.isNullOrBlank() && isAudioPath(note.mediaFilePath))
            }
            matchesDeck && matchesType
        }

        // Decks associated with filtered notes (or all decks if ALL filter)
        val deckIdsInNotes = filteredNotes.map { it.deckId }.toSet()
        val filteredDecks = if (deckIdFilter == "ALL") {
            allDecks.filter { it.id in deckIdsInNotes || allDecks.size <= 1 }
        } else {
            allDecks.filter { it.id == deckIdFilter }
        }

        if (filteredNotes.isEmpty() && filteredDecks.isEmpty()) {
            throw IllegalStateException("No cards match the selected filter criteria.")
        }

        onProgress(0.15f, "Found ${filteredNotes.size} cards to backup...")

        // Collect media files
        val mediaFilesToExport = mutableListOf<Pair<RevisionNoteEntity, File>>()
        filteredNotes.forEach { note ->
            val path = note.mediaFilePath
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    mediaFilesToExport.add(Pair(note, file))
                }
            }
        }

        val totalWork = 1 + mediaFilesToExport.size
        var currentWork = 0

        // Ensure parent output dir exists
        outputFile.parentFile?.mkdirs()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
            // 1. Build backup_manifest.json
            onProgress(0.20f, "Generating backup manifest...")

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val rootJson = JSONObject().apply {
                put("appName", "Focus Island")
                put("version", 2)
                put("exportedAt", sdf.format(Date()))
                put("exportedAtMs", System.currentTimeMillis())
                put("cardTypeFilter", cardTypeFilter.name)
                put("deckIdFilter", deckIdFilter)

                val decksArr = JSONArray()
                filteredDecks.forEach { d ->
                    decksArr.put(JSONObject().apply {
                        put("id", d.id)
                        put("name", d.name)
                        put("colorHex", d.colorHex)
                        put("createdAt", d.createdAt)
                    })
                }
                put("decks", decksArr)

                val notesArr = JSONArray()
                filteredNotes.forEach { n ->
                    notesArr.put(JSONObject().apply {
                        put("id", n.id)
                        put("deckId", n.deckId)
                        put("title", n.title)
                        put("contentBlocksJson", n.contentBlocksJson)
                        put("plainTextPreview", n.plainTextPreview)
                        put("contentMarkdown", n.contentMarkdown)
                        put("mediaType", n.mediaType ?: JSONObject.NULL)
                        val mediaFile = n.mediaFilePath?.let { File(it) }
                        if (mediaFile != null && mediaFile.exists()) {
                            put("mediaFileName", mediaFile.name)
                        } else {
                            put("mediaFileName", JSONObject.NULL)
                        }
                        put("easeFactor", n.easeFactor.toDouble())
                        put("intervalDays", n.intervalDays)
                        put("repetitions", n.repetitions)
                        put("dueDate", n.dueDate)
                        put("lastReviewedAt", n.lastReviewedAt ?: JSONObject.NULL)
                        put("syncStatus", n.syncStatus.name)
                    })
                }
                put("notes", notesArr)
            }

            val manifestBytes = rootJson.toString(2).toByteArray(Charsets.UTF_8)
            val manifestEntry = ZipEntry(MANIFEST_FILE_NAME)
            zos.putNextEntry(manifestEntry)
            zos.write(manifestBytes)
            zos.closeEntry()

            currentWork++
            val initialProgress = 0.20f + (currentWork.toFloat() / totalWork.toFloat()) * 0.75f
            onProgress(initialProgress, "Manifest created. Packaging media...")

            // 2. Packaging media files
            val buffer = ByteArray(32 * 1024)
            mediaFilesToExport.forEachIndexed { index, (note, mediaFile) ->
                val progressVal = 0.20f + ((currentWork + index).toFloat() / totalWork.toFloat()) * 0.75f
                onProgress(progressVal, "Packaging media (${index + 1}/${mediaFilesToExport.size}): ${mediaFile.name}")

                val zipEntryPath = "media/${mediaFile.name}"
                val entry = ZipEntry(zipEntryPath)
                zos.putNextEntry(entry)

                BufferedInputStream(FileInputStream(mediaFile)).use { fis ->
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        zos.write(buffer, 0, read)
                    }
                }
                zos.closeEntry()
            }
        }

        onProgress(1.0f, "Backup generated successfully!")

        val summary = "${filteredNotes.size} cards, ${filteredDecks.size} decks, ${mediaFilesToExport.size} media files"
        return@withContext BackupRestoreResult(
            notesCount = filteredNotes.size,
            decksCount = filteredDecks.size,
            mediaCount = mediaFilesToExport.size,
            summaryText = summary
        )
    }

    /**
     * Restores notes, decks, and media files from a .focuscards ZIP archive Uri.
     */
    suspend fun restoreBackup(
        context: Context,
        repository: RevisionRepository,
        inputFileUri: Uri,
        importMode: ImportMode,
        onProgress: (progress: Float, status: String) -> Unit
    ): BackupRestoreResult = withContext(Dispatchers.IO) {
        onProgress(0.05f, "Reading backup archive...")

        val mediaOutputDir = File(context.filesDir, "revisions_media")
        if (!mediaOutputDir.exists()) {
            mediaOutputDir.mkdirs()
        }

        // Temporary extraction directory
        val tempExtractDir = File(context.cacheDir, "backup_restore_temp_${System.currentTimeMillis()}")
        tempExtractDir.mkdirs()

        try {
            var manifestFile: File? = null
            val mediaExtractedFiles = mutableListOf<File>()

            // 1. Unzip archive contents
            val inputStream: InputStream = context.contentResolver.openInputStream(inputFileUri)
                ?: throw IllegalArgumentException("Could not open file URI: $inputFileUri")

            onProgress(0.15f, "Extracting backup archive contents...")

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                val buffer = ByteArray(32 * 1024)
                var entry: ZipEntry? = zis.nextEntry
                var fileCount = 0

                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory) {
                        if (entryName == MANIFEST_FILE_NAME) {
                            val dest = File(tempExtractDir, MANIFEST_FILE_NAME)
                            FileOutputStream(dest).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                            manifestFile = dest
                        } else if (entryName.startsWith("media/")) {
                            val fileName = File(entryName).name
                            val destMediaFile = File(mediaOutputDir, fileName)
                            FileOutputStream(destMediaFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                            mediaExtractedFiles.add(destMediaFile)
                        }
                    }
                    fileCount++
                    val extractProgress = (0.15f + (fileCount * 0.03f)).coerceAtMost(0.50f)
                    onProgress(extractProgress, "Unpacking file $fileCount: ${entryName.substringAfterLast('/')}")
                    entry = zis.nextEntry
                }
            }

            if (manifestFile == null || !manifestFile!!.exists()) {
                throw IllegalStateException("Invalid backup file: Missing $MANIFEST_FILE_NAME inside archive.")
            }

            onProgress(0.55f, "Parsing backup manifest...")

            val manifestContent = manifestFile!!.readText(Charsets.UTF_8)
            val rootJson = JSONObject(manifestContent)

            val decksArray = rootJson.optJSONArray("decks") ?: JSONArray()
            val parsedDecks = mutableListOf<RevisionDeckEntity>()
            for (i in 0 until decksArray.length()) {
                val d = decksArray.getJSONObject(i)
                parsedDecks.add(
                    RevisionDeckEntity(
                        id = d.optString("id"),
                        name = d.optString("name", "Deck"),
                        colorHex = d.optString("colorHex", "#4CAF50"),
                        createdAt = d.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val notesArray = rootJson.optJSONArray("notes") ?: JSONArray()
            val parsedNotes = mutableListOf<RevisionNoteEntity>()

            for (i in 0 until notesArray.length()) {
                val n = notesArray.getJSONObject(i)
                val mediaFileName = if (n.isNull("mediaFileName")) null else n.optString("mediaFileName")
                val resolvedMediaPath = if (!mediaFileName.isNullOrBlank()) {
                    File(mediaOutputDir, mediaFileName).absolutePath
                } else null

                val lastRev = if (n.isNull("lastReviewedAt")) null else n.optLong("lastReviewedAt")
                val mediaType = if (n.isNull("mediaType")) null else n.optString("mediaType")

                parsedNotes.add(
                    RevisionNoteEntity(
                        id = n.optString("id"),
                        deckId = n.optString("deckId", "default_deck"),
                        title = n.optString("title", "Untitled Card"),
                        contentBlocksJson = n.optString("contentBlocksJson", ""),
                        plainTextPreview = n.optString("plainTextPreview", ""),
                        contentMarkdown = n.optString("contentMarkdown", ""),
                        mediaType = mediaType,
                        mediaFilePath = resolvedMediaPath,
                        easeFactor = n.optDouble("easeFactor", 2.5).toFloat(),
                        intervalDays = n.optInt("intervalDays", 0),
                        repetitions = n.optInt("repetitions", 0),
                        dueDate = n.optLong("dueDate", System.currentTimeMillis()),
                        lastReviewedAt = lastRev,
                        syncStatus = try {
                            com.example.data.db.entity.SyncStatus.valueOf(n.optString("syncStatus", "SYNCED"))
                        } catch (e: Exception) {
                            com.example.data.db.entity.SyncStatus.SYNCED
                        }
                    )
                )
            }

            onProgress(0.70f, "Writing ${parsedNotes.size} cards to database...")

            if (importMode == ImportMode.REPLACE_ALL) {
                onProgress(0.75f, "Clearing existing cards for replace mode...")
                val currentNotes = repository.getAllNotesOnce()
                currentNotes.forEach { oldNote ->
                    if (!oldNote.mediaFilePath.isNullOrBlank()) {
                        try { File(oldNote.mediaFilePath).delete() } catch (_: Exception) {}
                    }
                }
            }

            // Restore decks
            parsedDecks.forEach { deck ->
                repository.upsertDeck(deck)
            }

            // Restore notes
            parsedNotes.forEachIndexed { index, note ->
                val progressVal = 0.80f + ((index + 1).toFloat() / parsedNotes.size.toFloat()) * 0.18f
                onProgress(progressVal, "Importing card ${index + 1}/${parsedNotes.size}: ${note.title}")

                if (importMode == ImportMode.REPLACE_ALL) {
                    repository.upsertNote(note)
                } else {
                    val existing = repository.getNoteById(note.id)
                    val shouldOverwrite = existing == null || (note.lastReviewedAt ?: 0L) >= (existing.lastReviewedAt ?: 0L)
                    if (shouldOverwrite) {
                        repository.upsertNote(note)
                    }
                }
            }

            onProgress(1.0f, "Import completed successfully!")

            val summary = "Imported ${parsedNotes.size} cards and ${parsedDecks.size} decks (${mediaExtractedFiles.size} media files extracted)"
            return@withContext BackupRestoreResult(
                notesCount = parsedNotes.size,
                decksCount = parsedDecks.size,
                mediaCount = mediaExtractedFiles.size,
                summaryText = summary
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            throw e
        } finally {
            try { tempExtractDir.deleteRecursively() } catch (_: Exception) {}
        }
    }

    private fun isImagePath(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun isAudioPath(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".3gp") || lower.endsWith(".aac")
    }
}
