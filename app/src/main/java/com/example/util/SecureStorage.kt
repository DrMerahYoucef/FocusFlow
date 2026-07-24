package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStorage {
    
    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secure_settings",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard private preferences if MasterKey fails on specific custom ROMs/emulators
            context.getSharedPreferences("secure_settings_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveGeminiApiKey(context: Context, key: String) {
        getEncryptedPrefs(context).edit().putString("gemini_api_key", key.trim()).apply()
    }

    fun getGeminiApiKey(context: Context): String? {
        val key = getEncryptedPrefs(context).getString("gemini_api_key", null)
        return if (key.isNull_or_blank()) null else key
    }

    fun clearGeminiApiKey(context: Context) {
        getEncryptedPrefs(context).edit().remove("gemini_api_key").apply()
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
