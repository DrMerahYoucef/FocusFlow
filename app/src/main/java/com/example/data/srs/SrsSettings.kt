package com.example.data.srs

import java.time.LocalTime

data class SrsSettings(
    val newCardsPerDay: Int = 20,
    val maxReviewsPerDay: Int = 200,
    val startingEaseFactor: Float = 2.5f,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val notificationsEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val explainModeEnabled: Boolean = false,
    val customPromptOverride: String? = null,
    val selectedGeminiModel: String = "gemini-2.5-flash",
    val availableGeminiModels: List<String> = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
        "gemini-2.5-flash-lite",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-2.0-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite"
    )
)
