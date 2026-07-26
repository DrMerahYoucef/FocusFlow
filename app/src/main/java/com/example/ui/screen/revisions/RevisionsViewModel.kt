package com.example.ui.screen.revisions

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.db.entity.RevisionDeckEntity
import com.example.data.db.entity.RevisionMediaType
import com.example.data.db.entity.RevisionNoteEntity
import com.example.data.repository.CaptureMediaKind
import com.example.data.repository.GeminiQuestionEngine
import com.example.data.repository.ImportMode
import com.example.data.repository.RevisionExportFile
import com.example.data.srs.ReviewGrade
import com.example.data.srs.Sm2Scheduler
import com.example.data.srs.SrsSettings
import com.example.service.RevisionReminderWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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
    val isTestingModel: Boolean = false,
    val modelTestMessage: String? = null,
    val isModelVerified: Boolean = false
)

class RevisionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusFlowApplication.instance.revisionRepository
    private val localMediaStore = FocusFlowApplication.instance.localMediaStore
    private val sharedPrefs = application.getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(RevisionsUiState())
    val uiState: StateFlow<RevisionsUiState> = _uiState.asStateFlow()

    private val geminiQuestionEngine = GeminiQuestionEngine(
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
        },
        modelProvider = {
            val model = _uiState.value.srsSettings.geminiModel.trim()
            if (model.isNotBlank()) model else "gemini-3.5-flash"
        }
    )

    init {
        loadSettings()
        observeData()
    }

    private fun loadSettings() {
        val customModelsStr = sharedPrefs.getString("gemini_custom_models", "") ?: ""
        val customList = if (customModelsStr.isNotBlank()) {
            customModelsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        val settings = SrsSettings(
            newCardsPerDay = sharedPrefs.getInt("srs_new_cards_per_day", 20),
            maxReviewsPerDay = sharedPrefs.getInt("srs_max_reviews_per_day", 200),
            startingEaseFactor = sharedPrefs.getFloat("srs_starting_ease", 2.5f),
            reminderHour = sharedPrefs.getInt("srs_reminder_hour", 19),
            reminderMinute = sharedPrefs.getInt("srs_reminder_minute", 0),
            notificationsEnabled = sharedPrefs.getBoolean("srs_notifications_enabled", true),
            geminiApiKey = sharedPrefs.getString("gemini_api_key", "") ?: "",
            geminiModel = sharedPrefs.getString("gemini_model", "gemini-3.5-flash") ?: "gemini-3.5-flash",
            customModels = customList
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
        }
    }

    fun captureNoteFromPhoto(
        bitmap: Bitmap,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, ocrError = null) }
            var savedPath: String? = null
            try {
                savedPath = localMediaStore.savePhoto(bitmap)
                val fileBytes = File(savedPath).readBytes()
                val questionResult = geminiQuestionEngine.generateQuestion(fileBytes, CaptureMediaKind.IMAGE_JPEG)

                val note = RevisionNoteEntity(
                    deckId = _uiState.value.selectedDeckId,
                    question = questionResult.question,
                    mediaType = RevisionMediaType.IMAGE,
                    mediaFilePath = savedPath,
                    title = questionResult.title,
                    easeFactor = _uiState.value.srsSettings.startingEaseFactor
                )

                repository.upsertNote(note)
                _uiState.update { it.copy(isProcessingOcr = false) }
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("RevisionsViewModel", "Photo card creation failed", e)
                if (savedPath != null) {
                    localMediaStore.delete(savedPath)
                }
                val errorMessage = when {
                    !e.message.isNullOrBlank() -> e.message!!
                    !e.localizedMessage.isNullOrBlank() -> e.localizedMessage!!
                    else -> "Failed to process photo (${e.javaClass.simpleName}). Please check API key."
                }
                _uiState.update { it.copy(isProcessingOcr = false, ocrError = errorMessage) }
                onComplete(false)
            }
        }
    }

    fun processCapturedImage(
        croppedBitmap: Bitmap,
        onComplete: (Boolean) -> Unit
    ) {
        captureNoteFromPhoto(croppedBitmap, onComplete)
    }

    fun captureNoteFromAudio(
        tempRecordingFile: File,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, ocrError = null) }
            var savedPath: String? = null
            try {
                savedPath = localMediaStore.saveAudioFrom(tempRecordingFile)
                val fileBytes = File(savedPath).readBytes()
                val questionResult = geminiQuestionEngine.generateQuestion(fileBytes, CaptureMediaKind.AUDIO_M4A)

                val note = RevisionNoteEntity(
                    deckId = _uiState.value.selectedDeckId,
                    question = questionResult.question,
                    mediaType = RevisionMediaType.AUDIO,
                    mediaFilePath = savedPath,
                    title = questionResult.title,
                    easeFactor = _uiState.value.srsSettings.startingEaseFactor
                )

                repository.upsertNote(note)
                _uiState.update { it.copy(isProcessingOcr = false) }
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("RevisionsViewModel", "Audio card creation failed", e)
                if (savedPath != null) {
                    localMediaStore.delete(savedPath)
                }
                val errorMessage = when {
                    !e.message.isNullOrBlank() -> e.message!!
                    !e.localizedMessage.isNullOrBlank() -> e.localizedMessage!!
                    else -> "Failed to process audio (${e.javaClass.simpleName}). Please check API key."
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
            .putString("gemini_model", settings.geminiModel)
            .putString("gemini_custom_models", settings.customModels.joinToString(","))
            .apply()

        _uiState.update { it.copy(srsSettings = settings, isModelVerified = false, modelTestMessage = null) }
        RevisionReminderWorker.scheduleRevisionReminder(getApplication(), settings)
    }

    fun setGeminiApiKey(apiKey: String) {
        val current = _uiState.value.srsSettings
        updateSrsSettings(current.copy(geminiApiKey = apiKey))
    }

    fun setGeminiModel(model: String) {
        val current = _uiState.value.srsSettings
        updateSrsSettings(current.copy(geminiModel = model))
    }

    fun addCustomModel(modelName: String) {
        val cleanName = modelName.trim()
        if (cleanName.isBlank()) return
        val current = _uiState.value.srsSettings
        val updatedList = (current.customModels + cleanName).distinct()
        updateSrsSettings(current.copy(customModels = updatedList, geminiModel = cleanName))
    }

    fun checkActiveModel(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingModel = true, modelTestMessage = null) }
            val activeModel = _uiState.value.srsSettings.geminiModel
            val result = geminiQuestionEngine.testActiveModel(activeModel)
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isTestingModel = false,
                            isModelVerified = true,
                            modelTestMessage = msg
                        )
                    }
                    onResult(true, msg)
                },
                onFailure = { err ->
                    val errorMsg = err.message ?: "Failed to connect to Gemini API."
                    _uiState.update {
                        it.copy(
                            isTestingModel = false,
                            isModelVerified = false,
                            modelTestMessage = errorMsg
                        )
                    }
                    onResult(false, errorMsg)
                }
            )
        }
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
            onComplete()
        }
    }

    fun deleteNote(note: RevisionNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}
