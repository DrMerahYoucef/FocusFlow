package com.focusisland.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,   // e.g. "com.whatsapp"
    val appName: String,                   // display name e.g. "WhatsApp"
    val appIconBase64: String,             // Base64 PNG for UI display
    val blockNotifications: Boolean = true,
    val blockLaunch: Boolean = true
)
