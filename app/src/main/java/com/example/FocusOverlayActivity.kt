package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.neumorphicShadow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeumorphicColors

class FocusOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blockedPkg = intent.getStringExtra("blocked_package") ?: ""
        var appName = "An app"
        try {
            val appInfo = packageManager.getApplicationInfo(blockedPkg, 0)
            appName = packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sharedPrefs = getSharedPreferences("focusflow_prefs", MODE_PRIVATE)
        val themeMode = sharedPrefs.getString("theme_mode", "system") ?: "system"
        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val darkTheme = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemDark
        }

        setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                FocusOverlayScreen(
                    appName = appName,
                    onDismiss = { finish() },
                    onGoBack = {
                        startActivity(Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun FocusOverlayScreen(appName: String, onDismiss: () -> Unit, onGoBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .neumorphicShadow(cornerRadius = 50.dp, elevation = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 42.sp)
            }

            Text(
                text = "Stay focused!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NeumorphicColors.TextPrimary
            )
            Text(
                text = "$appName is blocked\nduring your focus session.",
                style = MaterialTheme.typography.bodyMedium,
                color = NeumorphicColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            val quotes = listOf(
                "Every minute counts. You've got this.",
                "The phone can wait. Your goals can't.",
                "Deep work is your superpower.",
                "Focus now, scroll later.",
                "You're building something great."
            )
            Text(
                text = "\"${quotes.random()}\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = NeumorphicColors.Primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            NeumorphicButton(
                label = "Go Back to Focus",
                icon = Icons.Default.ArrowBack,
                onClick = onGoBack,
                accentColor = NeumorphicColors.Primary
            )
            TextButton(onClick = onDismiss) {
                Text(
                    "Dismiss (not recommended)",
                    fontSize = 12.sp,
                    color = NeumorphicColors.TextSecondary
                )
            }
        }
    }
}
