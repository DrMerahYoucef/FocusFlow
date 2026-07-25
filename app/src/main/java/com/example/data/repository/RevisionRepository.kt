package com.example.data.repository

import com.example.data.db.dao.RevisionDeckDao
import com.example.data.db.dao.RevisionNoteDao
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.db.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ExportedDeck(
    val id: String,
    val name: String,
    val colorHex: String
)

data class ExportedNote(
    val id: String,
    val deckId: String,
    val title: String,
    val contentMarkdown: String,
    val easeFactor: Float,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: Long,
    val lastReviewedAt: Long?
)

data class RevisionExportFile(
    val formatVersion: Int = 1,
    val exportedAt: String,
    val decks: List<ExportedDeck>,
    val notes: List<ExportedNote>
)

enum class ImportMode { MERGE, REPLACE_ALL }

class RevisionRepository(
    private val deckDao: RevisionDeckDao,
    private val noteDao: RevisionNoteDao
) {
    val allDecks: Flow<List<RevisionDeckEntity>> = deckDao.getAllDecks()
    val dueNotes: Flow<List<RevisionNoteEntity>> = noteDao.getDueNotes()
    val allNotes: Flow<List<RevisionNoteEntity>> = noteDao.getAllNotes()

    fun getNotesForDeck(deckId: String): Flow<List<RevisionNoteEntity>> = noteDao.getNotesForDeck(deckId)
    fun getDueNotesForDeck(deckId: String): Flow<List<RevisionNoteEntity>> = noteDao.getDueNotesForDeck(deckId)

    suspend fun getDueCount(): Int = noteDao.getDueCount()
    suspend fun getTotalCount(): Int = noteDao.getTotalCount()

    suspend fun upsertDeck(deck: RevisionDeckEntity) = deckDao.upsert(deck)
    suspend fun deleteDeck(deck: RevisionDeckEntity) = deckDao.delete(deck)

    suspend fun upsertNote(note: RevisionNoteEntity) = noteDao.upsert(note)
    suspend fun upsertNotes(notes: List<RevisionNoteEntity>) = noteDao.upsertAll(notes)
    suspend fun deleteNote(note: RevisionNoteEntity) = noteDao.delete(note)

    suspend fun getUnsyncedNotes() = noteDao.getUnsyncedNotes()

    // Export JSON
    suspend fun buildExportJson(): String {
        val decks = deckDao.getAllDecksOnce()
        val notes = noteDao.getAllNotesOnce()

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val root = JSONObject().apply {
            put("formatVersion", 1)
            put("exportedAt", sdf.format(Date()))

            val decksArray = JSONArray()
            decks.forEach { d ->
                decksArray.put(JSONObject().apply {
                    put("id", d.id)
                    put("name", d.name)
                    put("colorHex", d.colorHex)
                })
            }
            put("decks", decksArray)

            val notesArray = JSONArray()
            notes.forEach { n ->
                notesArray.put(JSONObject().apply {
                    put("id", n.id)
                    put("deckId", n.deckId)
                    put("title", n.title)
                    put("contentMarkdown", n.contentMarkdown)
                    put("easeFactor", n.easeFactor.toDouble())
                    put("intervalDays", n.intervalDays)
                    put("repetitions", n.repetitions)
                    put("dueDate", n.dueDate)
                    if (n.lastReviewedAt != null) {
                        put("lastReviewedAt", n.lastReviewedAt)
                    } else {
                        put("lastReviewedAt", JSONObject.NULL)
                    }
                })
            }
            put("notes", notesArray)
        }

        return root.toString(2)
    }

    // Parse JSON for Import Preview
    fun parseExportJson(jsonString: String): RevisionExportFile {
        val root = JSONObject(jsonString)
        val formatVersion = root.optInt("formatVersion", 1)
        val exportedAt = root.optString("exportedAt", "")

        val decksArray = root.optJSONArray("decks") ?: JSONArray()
        val decksList = mutableListOf<ExportedDeck>()
        for (i in 0 until decksArray.length()) {
            val d = decksArray.getJSONObject(i)
            decksList.add(
                ExportedDeck(
                    id = d.optString("id"),
                    name = d.optString("name", "Paquet"),
                    colorHex = d.optString("colorHex", "#4CAF50")
                )
            )
        }

        val notesArray = root.optJSONArray("notes") ?: JSONArray()
        val notesList = mutableListOf<ExportedNote>()
        for (i in 0 until notesArray.length()) {
            val n = notesArray.getJSONObject(i)
            val lastRev = if (n.isNull("lastReviewedAt")) null else n.optLong("lastReviewedAt")
            notesList.add(
                ExportedNote(
                    id = n.optString("id"),
                    deckId = n.optString("deckId", "default_deck"),
                    title = n.optString("title", "Sans titre"),
                    contentMarkdown = n.optString("contentMarkdown", ""),
                    easeFactor = n.optDouble("easeFactor", 2.5).toFloat(),
                    intervalDays = n.optInt("intervalDays", 0),
                    repetitions = n.optInt("repetitions", 0),
                    dueDate = n.optLong("dueDate", System.currentTimeMillis()),
                    lastReviewedAt = lastRev
                )
            )
        }

        return RevisionExportFile(
            formatVersion = formatVersion,
            exportedAt = exportedAt,
            decks = decksList,
            notes = notesList
        )
    }

    // Apply Import
    suspend fun applyImport(importData: RevisionExportFile, mode: ImportMode) {
        if (mode == ImportMode.REPLACE_ALL) {
            noteDao.deleteAll()
            deckDao.deleteAll()
        }

        val nowMs = System.currentTimeMillis()

        importData.decks.forEach { d ->
            deckDao.upsert(
                RevisionDeckEntity(
                    id = d.id,
                    name = d.name,
                    colorHex = d.colorHex,
                    createdAt = nowMs
                )
            )
        }

        importData.notes.forEach { n ->
            val existing = noteDao.getById(n.id)
            val shouldOverwrite = mode == ImportMode.REPLACE_ALL ||
                    existing == null ||
                    (n.lastReviewedAt ?: 0L) >= (existing.lastReviewedAt ?: 0L)

            if (shouldOverwrite) {
                noteDao.upsert(
                    RevisionNoteEntity(
                        id = n.id,
                        deckId = n.deckId,
                        title = n.title,
                        contentMarkdown = n.contentMarkdown,
                        createdAt = nowMs,
                        updatedAt = nowMs,
                        easeFactor = n.easeFactor,
                        intervalDays = n.intervalDays,
                        repetitions = n.repetitions,
                        dueDate = n.dueDate,
                        lastReviewedAt = n.lastReviewedAt,
                        syncStatus = SyncStatus.PENDING_UPLOAD
                    )
                )
            }
        }
    }
}
