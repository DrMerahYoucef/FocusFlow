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
    val lastExportSummary: String? = null,
    val hasApiKey: Boolean = false
)

class RevisionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusFlowApplication.instance.revisionRepository
    private val localMediaStore = com.example.data.repository.LocalMediaStore(application)
    private val sharedPrefs = application.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(RevisionsUiState())
    val uiState: StateFlow<RevisionsUiState> = _uiState.asStateFlow()

    private val ocrEngineProvider = com.example.data.repository.OcrEngineProvider(
        apiKeyProvider = { getEffectiveApiKey() }
    )

    fun getEffectiveApiKey(): String {
        val userKey = _uiState.value.srsSettings.geminiApiKey.trim()
        val prefKey = sharedPrefs.getString("gemini_api_key", "")?.trim().orEmpty()
        val buildConfigKey = try {
            val key = com.example.BuildConfig.GEMINI_API_KEY.trim()
            if (key.isNotBlank() && key != "null" && key != "MY_GEMINI_API_KEY" && key != "DEFAULT_KEY") key else ""
        } catch (e: Exception) { "" }

        return when {
            userKey.isNotBlank() && userKey != "MY_GEMINI_API_KEY" -> userKey
            prefKey.isNotBlank() && prefKey != "MY_GEMINI_API_KEY" -> prefKey
            buildConfigKey.isNotBlank() -> buildConfigKey
            else -> ""
        }
    }

    init {
        loadSettings()
        observeData()
    }

    private fun loadSettings() {
        val defaultModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.5-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-2.0-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite"
        )
        val savedModelsCsv = sharedPrefs.getString("gemini_available_models", null)
        val availableModels = if (savedModelsCsv.isNullOrBlank()) {
            defaultModels
        } else {
            savedModelsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
        }
        val selectedModel = sharedPrefs.getString("selected_gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"

        val settings = SrsSettings(
            newCardsPerDay = sharedPrefs.getInt("srs_new_cards_per_day", 20),
            maxReviewsPerDay = sharedPrefs.getInt("srs_max_reviews_per_day", 200),
            startingEaseFactor = sharedPrefs.getFloat("srs_starting_ease", 2.5f),
            reminderHour = sharedPrefs.getInt("srs_reminder_hour", 19),
            reminderMinute = sharedPrefs.getInt("srs_reminder_minute", 0),
            notificationsEnabled = sharedPrefs.getBoolean("srs_notifications_enabled", true),
            geminiApiKey = sharedPrefs.getString("gemini_api_key", "") ?: "",
            explainModeEnabled = sharedPrefs.getBoolean("srs_explain_mode_enabled", false),
            customPromptOverride = sharedPrefs.getString("srs_custom_prompt_override", null),
            selectedGeminiModel = selectedModel,
            availableGeminiModels = availableModels
        )
        val lastSummary = sharedPrefs.getString("srs_last_export_summary", null)
        _uiState.update { it.copy(srsSettings = settings, lastExportSummary = lastSummary, hasApiKey = getEffectiveApiKey().isNotBlank()) }
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
                        totalCount = all.size,
                        hasApiKey = getEffectiveApiKey().isNotBlank()
                    )
                }
            }
        }
    }

    fun setSelectedDeck(deckId: String) {
        _uiState.update { it.copy(selectedDeckId = deckId) }
    }

    fun addDeck(name: String, colorHex: String = "#4CAF50") {
        addDeckAndSelect(name, colorHex)
    }

    fun addDeckAndSelect(name: String, colorHex: String = "#4CAF50", onCreated: (String) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val deck = RevisionDeckEntity(name = name, colorHex = colorHex)
            repository.upsertDeck(deck)
            setSelectedDeck(deck.id)
            onCreated(deck.id)
        }
    }

    fun generateTitleFromContent(
        content: String,
        onResult: (String) -> Unit
    ) {
        val promptText = content.ifBlank { "Study Card" }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true) }
            try {
                val apiKey = getEffectiveApiKey()
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    _uiState.update { it.copy(isProcessingOcr = false) }
                    val fallback = promptText.trim().lines().firstOrNull()?.take(35) ?: "Study Card"
                    onResult(fallback)
                    return@launch
                }
                val titleEngine = com.example.data.repository.GeminiTitleEngine { apiKey }
                val titleRes = titleEngine.generateTitle(promptText.toByteArray(Charsets.UTF_8), "text/plain")
                _uiState.update { it.copy(isProcessingOcr = false) }
                onResult(titleRes.title)
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessingOcr = false) }
                val fallback = promptText.trim().lines().firstOrNull()?.take(35) ?: "Study Card"
                onResult(fallback)
            }
        }
    }

    fun generateTitleFromAudio(
        audioFile: java.io.File?,
        currentTitleText: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true) }
            try {
                val apiKey = getEffectiveApiKey()
                if (audioFile != null && audioFile.exists() && audioFile.length() > 0L && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    val audioBytes = audioFile.readBytes()
                    val titleEngine = com.example.data.repository.GeminiTitleEngine { apiKey }
                    val titleRes = titleEngine.generateTitle(audioBytes, "audio/3gpp")
                    _uiState.update { it.copy(isProcessingOcr = false) }
                    onResult(titleRes.title)
                    return@launch
                }
                if (currentTitleText.isNotBlank() && apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    val titleEngine = com.example.data.repository.GeminiTitleEngine { apiKey }
                    val titleRes = titleEngine.generateTitle(currentTitleText.toByteArray(Charsets.UTF_8), "text/plain")
                    _uiState.update { it.copy(isProcessingOcr = false) }
                    onResult(titleRes.title)
                    return@launch
                }
                _uiState.update { it.copy(isProcessingOcr = false) }
                val fallback = currentTitleText.ifBlank { "Voice Note ${System.currentTimeMillis() % 10000}" }
                onResult(fallback)
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessingOcr = false) }
                val fallback = currentTitleText.ifBlank { "Voice Note ${System.currentTimeMillis() % 10000}" }
                onResult(fallback)
            }
        }
    }

    fun deleteDeck(deck: RevisionDeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
        }
    }

    suspend fun ensureAtLeastOneDeck(): String {
        val decks = repository.getAllDecksOnce()
        if (decks.isNotEmpty()) return decks.first().id
        val defaultDeck = RevisionDeckEntity(id = "default_deck", name = "Général", colorHex = "#4CAF50")
        repository.upsertDeck(defaultDeck)
        return defaultDeck.id
    }

    fun deleteDeckAndNotes(deckId: String) {
        viewModelScope.launch {
            val notesInDeck = repository.getAllNotesOnce().filter { it.deckId == deckId }
            notesInDeck.forEach { note ->
                if (!note.mediaFilePath.isNullOrBlank()) {
                    localMediaStore.delete(note.mediaFilePath)
                }
            }
            repository.deleteDeckAndNotes(deckId)
        }
    }

    fun moveNoteToDeck(noteId: String, newDeckId: String) {
        viewModelScope.launch {
            repository.getNoteById(noteId)?.let { note ->
                repository.upsertNote(note.copy(deckId = newDeckId, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateNote(note: RevisionNoteEntity) {
        viewModelScope.launch {
            repository.upsertNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun submitReviewGrade(note: RevisionNoteEntity, grade: ReviewGrade) {
        viewModelScope.launch {
            val updatedNote = Sm2Scheduler.schedule(note, grade)
            repository.upsertNote(updatedNote)
            RevisionSyncWorker.scheduleSyncWork(getApplication())
        }
    }

    fun processCapturedImage(
        croppedBitmap: Bitmap,
        explainMode: Boolean = false,
        onComplete: (Boolean) -> Unit
    ) {
        processCapturedImageWithCustomPrompt(
            croppedBitmap = croppedBitmap,
            temporaryPromptAddendum = null,
            explainMode = explainMode,
            targetDeckId = _uiState.value.selectedDeckId,
            onResult = { noteResult, error ->
                if (noteResult != null) {
                    val singleNote = noteResult.toSingleNote(_uiState.value.selectedDeckId, _uiState.value.srsSettings.startingEaseFactor)
                    viewModelScope.launch {
                        repository.upsertNote(singleNote)
                        RevisionSyncWorker.scheduleSyncWork(getApplication())
                    }
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }
        )
    }

    fun processCapturedImageWithCustomPrompt(
        croppedBitmap: Bitmap,
        temporaryPromptAddendum: String?,
        explainMode: Boolean = false,
        targetDeckId: String,
        onResult: (com.example.data.repository.GeminiNoteResult?, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, ocrError = null) }
            try {
                val mode = if (explainMode) com.example.data.repository.ExtractionMode.EXPLAIN else com.example.data.repository.ExtractionMode.VERBATIM
                val engine = ocrEngineProvider.get()
                val result = engine.extractStructuredContent(
                    bitmap = croppedBitmap,
                    mode = mode,
                    promptOverride = _uiState.value.srsSettings.customPromptOverride,
                    temporaryPromptAddendum = temporaryPromptAddendum
                )

                _uiState.update { it.copy(isProcessingOcr = false) }
                onResult(result, null)
            } catch (e: Exception) {
                android.util.Log.e("RevisionsViewModel", "OCR process failed", e)
                val errorMessage = when {
                    !e.message.isNullOrBlank() -> e.message!!
                    !e.localizedMessage.isNullOrBlank() -> e.localizedMessage!!
                    else -> "Error processing card (${e.javaClass.simpleName})"
                }
                _uiState.update { it.copy(isProcessingOcr = false, ocrError = errorMessage) }
                onResult(null, errorMessage)
            }
        }
    }

    fun createManualNote(
        title: String,
        answerText: String,
        deckId: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val blocksJson = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("type", "text")
                    put("content", answerText)
                    put("highlights", org.json.JSONArray())
                })
            }.toString()

            val note = RevisionNoteEntity(
                deckId = deckId,
                title = title.ifBlank { "Manual Card" },
                contentBlocksJson = blocksJson,
                plainTextPreview = answerText,
                contentMarkdown = answerText,
                easeFactor = _uiState.value.srsSettings.startingEaseFactor
            )
            repository.upsertNote(note)
            RevisionSyncWorker.scheduleSyncWork(getApplication())
            onComplete()
        }
    }

    fun createLocalImageCard(
        bitmap: Bitmap,
        userTitle: String?,
        deckId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, ocrError = null) }
            try {
                val savedPath = localMediaStore.savePhoto(bitmap)
                var titleToUse = userTitle?.trim().orEmpty()

                if (titleToUse.isBlank()) {
                    try {
                        val bos = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
                        val titleEngine = com.example.data.repository.GeminiTitleEngine { getEffectiveApiKey() }
                        val res = titleEngine.generateTitle(bos.toByteArray(), "image/jpeg")
                        titleToUse = res.title
                    } catch (e: Exception) {
                        titleToUse = "Photo Card"
                    }
                }

                val note = RevisionNoteEntity(
                    deckId = deckId,
                    title = titleToUse.ifBlank { "Photo Card" },
                    contentBlocksJson = "",
                    plainTextPreview = "Photo Card",
                    contentMarkdown = "",
                    mediaType = "IMAGE",
                    mediaFilePath = savedPath,
                    easeFactor = _uiState.value.srsSettings.startingEaseFactor
                )
                repository.upsertNote(note)
                _uiState.update { it.copy(isProcessingOcr = false) }
                onComplete(true, null)
            } catch (e: Exception) {
                android.util.Log.e("RevisionsViewModel", "Local photo card creation failed", e)
                _uiState.update { it.copy(isProcessingOcr = false, ocrError = e.message) }
                onComplete(false, e.message)
            }
        }
    }

    fun createLocalAudioCard(
        recordingFile: java.io.File,
        userTitle: String?,
        deckId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, ocrError = null) }
            try {
                val savedPath = localMediaStore.saveAudioFrom(recordingFile)
                var titleToUse = userTitle?.trim().orEmpty()

                if (titleToUse.isBlank()) {
                    try {
                        val audioBytes = java.io.File(savedPath).readBytes()
                        val titleEngine = com.example.data.repository.GeminiTitleEngine { getEffectiveApiKey() }
                        val res = titleEngine.generateTitle(audioBytes, "audio/m4a")
                        titleToUse = res.title
                    } catch (e: Exception) {
                        titleToUse = "Voice Note Card"
                    }
                }

                val note = RevisionNoteEntity(
                    deckId = deckId,
                    title = titleToUse.ifBlank { "Voice Note Card" },
                    contentBlocksJson = "",
                    plainTextPreview = "Voice Note Card",
                    contentMarkdown = "",
                    mediaType = "AUDIO",
                    mediaFilePath = savedPath,
                    easeFactor = _uiState.value.srsSettings.startingEaseFactor
                )
                repository.upsertNote(note)
                _uiState.update { it.copy(isProcessingOcr = false) }
                onComplete(true, null)
            } catch (e: Exception) {
                android.util.Log.e("RevisionsViewModel", "Local audio card creation failed", e)
                _uiState.update { it.copy(isProcessingOcr = false, ocrError = e.message) }
                onComplete(false, e.message)
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
            .putString("selected_gemini_model", settings.selectedGeminiModel)
            .putString("gemini_available_models", settings.availableGeminiModels.joinToString(","))
            .apply()

        _uiState.update { it.copy(srsSettings = settings, hasApiKey = getEffectiveApiKey().isNotBlank()) }
        RevisionReminderWorker.scheduleRevisionReminder(getApplication(), settings)
    }

    fun setGeminiApiKey(apiKey: String) {
        val current = _uiState.value.srsSettings
        updateSrsSettings(current.copy(geminiApiKey = apiKey))
    }

    fun setSelectedGeminiModel(model: String) {
        val current = _uiState.value.srsSettings
        updateSrsSettings(current.copy(selectedGeminiModel = model))
    }

    fun addCustomGeminiModel(model: String) {
        val current = _uiState.value.srsSettings
        val cleanModel = model.trim().lowercase()
        if (cleanModel.isNotBlank() && !current.availableGeminiModels.contains(cleanModel)) {
            val updated = current.availableGeminiModels + cleanModel
            updateSrsSettings(current.copy(availableGeminiModels = updated, selectedGeminiModel = cleanModel))
        }
    }

    fun removeCustomGeminiModel(model: String) {
        val current = _uiState.value.srsSettings
        if (current.availableGeminiModels.size > 1) {
            val updated = current.availableGeminiModels.filter { it != model }
            val newSelected = if (current.selectedGeminiModel == model) updated.first() else current.selectedGeminiModel
            updateSrsSettings(current.copy(availableGeminiModels = updated, selectedGeminiModel = newSelected))
        }
    }

    fun deleteNote(note: RevisionNoteEntity) {
        viewModelScope.launch {
            if (!note.mediaFilePath.isNullOrBlank()) {
                localMediaStore.delete(note.mediaFilePath)
            }
            repository.deleteNote(note)
        }
    }
}
