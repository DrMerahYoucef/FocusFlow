package com.example.ui.screen.revisions

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.repository.GeminiOcrRepository
import com.example.data.repository.ImportMode
import com.example.data.repository.OcrEngineProvider
import com.example.data.repository.RevisionExportFile
import com.example.data.repository.toSingleNote
import com.example.data.srs.ReviewGrade
import com.example.data.srs.Sm2Scheduler
import com.example.data.srs.SrsSettings
import com.example.service.RevisionReminderWorker
import com.example.service.RevisionSyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RevisionsUiState(
    val decks: List<RevisionDeckEntity> = emptyList(),
    val dueNotes: List<RevisionNoteEntity> = emptyList(),
    val allNotes: List<RevisionNoteEntity> = emptyList(),
    val selectedDeckId: String = "default_deck",
    val dueCount: Int = 0,
    val totalCount: Int = 0,
    val isProcessingOcr: Boolean = false,
    val ocrError: String? = null,
    val srsSettings: SrsSettings = SrsSettings(),
    val lastExportSummary: String? = null
)

class RevisionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusFlowApplication.instance.revisionRepository
    private val sharedPrefs = application.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(RevisionsUiState())
    val uiState: StateFlow<RevisionsUiState> = _uiState.asStateFlow()

    private val ocrEngineProvider = com.example.data.repository.OcrEngineProvider(
        apiKeyProvider = {
            val userKey = _uiState.value.srsSettings.geminiApiKey.trim()
            val prefKey = sharedPrefs.getString("gemini_api_key", "")?.trim().orEmpty()
            val buildConfigKey = try {
                val key = com.example.BuildConfig.GEMINI_API_KEY.trim()
                if (key.isNotBlank() && key != "null" && key != "MY_GEMINI_API_KEY" && key != "DEFAULT_KEY") key else ""
            } catch (e: Exception) { "" }

            when {
                userKey.isNotBlank() && userKey != "MY_GEMINI_API_KEY" -> userKey
                prefKey.isNotBlank() && prefKey != "MY_GEMINI_API_KEY" -> prefKey
                buildConfigKey.isNotBlank() -> buildConfigKey
                else -> ""
            }
        }
    )

    init {
        loadSettings()
        observeData()
    }

    private fun loadSettings() {
        val settings = SrsSettings(
            newCardsPerDay = sharedPrefs.getInt("srs_new_cards_per_day", 20),
            maxReviewsPerDay = sharedPrefs.getInt("srs_max_reviews_per_day", 200),
            startingEaseFactor = sharedPrefs.getFloat("srs_starting_ease", 2.5f),
            reminderHour = sharedPrefs.getInt("srs_reminder_hour", 19),
            reminderMinute = sharedPrefs.getInt("srs_reminder_minute", 0),
            notificationsEnabled = sharedPrefs.getBoolean("srs_notifications_enabled", true),
            geminiApiKey = sharedPrefs.getString("gemini_api_key", "") ?: "",
            explainModeEnabled = sharedPrefs.getBoolean("srs_explain_mode_enabled", false),
            customPromptOverride = sharedPrefs.getString("srs_custom_prompt_override", null)
        )
        val lastSummary = sharedPrefs.getString("srs_last_export_summary", null)
        _uiState.update { it.copy(srsSettings = settings, lastExportSummary = lastSummary) }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.allDecks,
                repository.dueNotes,
                repository.allNotes
            ) { decks, due, all ->
                Triple(decks, due, all)
            }.collect { (decks, due, all) ->
                _uiState.update { state ->
                    val selectedDeckExists = decks.any { it.id == state.selectedDeckId }
                    val activeDeckId = if (selectedDeckExists) state.selectedDeckId else (decks.firstOrNull()?.id ?: "default_deck")
                    state.copy(
                        decks = decks,
                        dueNotes = due,
                        allNotes = all,
                        selectedDeckId = activeDeckId,
                        dueCount = due.size,
                        totalCount = all.size
                    )
                }
            }
        }
    }

    fun setSelectedDeck(deckId: String) {
        _uiState.update { it.copy(selectedDeckId = deckId) }
    }

    fun addDeck(name: String, colorHex: String = "#4CAF50") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val deck = RevisionDeckEntity(name = name, colorHex = colorHex)
            repository.upsertDeck(deck)
            setSelectedDeck(deck.id)
        }
    }

    fun deleteDeck(deck: RevisionDeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
        }
    }

    fun deleteDeckAndNotes(deckId: String) {
        viewModelScope.launch {
            repository.deleteDeckAndNotes(deckId)
        }
    }

    fun submitReviewGrade(note: RevisionNoteEntity, grade: ReviewGrade) {
        viewModelScope.launch {
            val updatedNote = Sm2Scheduler.schedule(note, grade)
            repository.upsertNote(updatedNote)
            RevisionSyncWorker.scheduleSyncWork(getApplication())
        }
    }

    fun ensureAtLeastOneDeck(onDeckReady: (String) -> Unit = {}) {
        viewModelScope.launch {
            val decks = repository.allDecks.first()
            if (decks.isEmpty()) {
                val general = RevisionDeckEntity(id = "default_deck", name = "General", colorHex = "#4CAF50")
                repository.upsertDeck(general)
                onDeckReady(general.id)
            } else {
                onDeckReady(decks.first().id)
            }
        }
    }

    fun moveNoteToDeck(noteId: String, newDeckId: String) {
        viewModelScope.launch {
            val note = repository.allNotes.first().find { it.id == noteId }
            if (note != null) {
                repository.upsertNote(note.copy(deckId = newDeckId, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateNoteTitleAndContent(noteId: String, newTitle: String, newTextContent: String) {
        viewModelScope.launch {
            val note = repository.allNotes.first().find { it.id == noteId } ?: return@launch
            val currentBlocks = com.example.data.repository.NoteBlocksSerializer.fromJson(note.contentBlocksJson)
            val updatedBlocks = if (currentBlocks.isNotEmpty()) {
                currentBlocks.mapIndexed { idx, block ->
                    if (idx == 0 && block is com.example.data.repository.NoteBlock.TextBlock) {
                        block.copy(content = newTextContent)
                    } else block
                }
            } else {
                listOf(com.example.data.repository.NoteBlock.TextBlock(content = newTextContent))
            }
            val newBlocksJson = com.example.data.repository.NoteBlocksSerializer.toJson(updatedBlocks)
            val newPreview = com.example.data.repository.NoteBlocksSerializer.toPlainTextPreview(updatedBlocks)
            val updatedNote = note.copy(
                title = newTitle,
                contentBlocksJson = newBlocksJson,
                plainTextPreview = newPreview,
                updatedAt = System.currentTimeMillis()
            )
            repository.upsertNote(updatedNote)
        }
    }

    fun processCapturedImage(
        croppedBitmap: Bitmap,
        targetDeckId: String? = null,
        explainMode: Boolean = false,
        temporaryPromptAddendum: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, ocrError = null) }
            try {
                val deckId = targetDeckId ?: _uiState.value.selectedDeckId
                val mode = if (explainMode) com.example.data.repository.ExtractionMode.EXPLAIN else com.example.data.repository.ExtractionMode.VERBATIM

                val engine = ocrEngineProvider.get()
                val promptOverride = _uiState.value.srsSettings.customPromptOverride
                val result = engine.extractStructuredContent(
                    bitmap = croppedBitmap,
                    mode = mode,
                    promptOverride = promptOverride,
                    temporaryPromptAddendum = temporaryPromptAddendum
                )
                val singleNote = result.toSingleNote(deckId, _uiState.value.srsSettings.startingEaseFactor)

                repository.upsertNote(singleNote)
                RevisionSyncWorker.scheduleSyncWork(getApplication())

                _uiState.update { it.copy(isProcessingOcr = false) }
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("RevisionsViewModel", "OCR failed", e)
                val errorMessage = when {
                    !e.message.isNullOrBlank() -> e.message!!
                    !e.localizedMessage.isNullOrBlank() -> e.localizedMessage!!
                    else -> "Failed to recognize text from image (${e.javaClass.simpleName}). Please check image or API key."
                }
                _uiState.update { it.copy(isProcessingOcr = false, ocrError = errorMessage) }
                onComplete(false)
            }
        }
    }

    fun updateSrsSettings(settings: SrsSettings) {
        sharedPrefs.edit()
            .putInt("srs_new_cards_per_day", settings.newCardsPerDay)
            .putInt("srs_max_reviews_per_day", settings.maxReviewsPerDay)
            .putFloat("srs_starting_ease", settings.startingEaseFactor)
            .putInt("srs_reminder_hour", settings.reminderHour)
            .putInt("srs_reminder_minute", settings.reminderMinute)
            .putBoolean("srs_notifications_enabled", settings.notificationsEnabled)
            .putString("gemini_api_key", settings.geminiApiKey)
            .putBoolean("srs_explain_mode_enabled", settings.explainModeEnabled)
            .putString("srs_custom_prompt_override", settings.customPromptOverride)
            .apply()

        _uiState.update { it.copy(srsSettings = settings) }
        RevisionReminderWorker.scheduleRevisionReminder(getApplication(), settings)
    }

    fun setGeminiApiKey(apiKey: String) {
        val current = _uiState.value.srsSettings
        updateSrsSettings(current.copy(geminiApiKey = apiKey))
    }

    // Export / Import
    suspend fun getExportJson(): String {
        val json = repository.buildExportJson()
        val summary = "${_uiState.value.totalCount} fiches, ${_uiState.value.decks.size} paquets"
        sharedPrefs.edit().putString("srs_last_export_summary", summary).apply()
        _uiState.update { it.copy(lastExportSummary = summary) }
        return json
    }

    fun parseImportJson(jsonString: String): RevisionExportFile? {
        return try {
            repository.parseExportJson(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("RevisionsViewModel", "Failed to parse import JSON", e)
            null
        }
    }

    fun applyImport(data: RevisionExportFile, mode: ImportMode, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.applyImport(data, mode)
            RevisionSyncWorker.scheduleSyncWork(getApplication())
            onComplete()
        }
    }

    fun deleteNote(note: RevisionNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}
