package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeumorphicColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(
    initialTitle: String = "",
    initialQuestion: String = "",
    mediaFilePath: String? = null,
    dialogTitle: String = "Modifier la fiche",
    confirmButtonLabel: String = "Enregistrer",
    onSave: (title: String, question: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var question by remember { mutableStateOf(initialQuestion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeumorphicColors.DialogBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = NeumorphicColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NeumorphicColors.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!mediaFilePath.isNullOrBlank() && File(mediaFilePath).exists()) {
                    val bitmap = remember(mediaFilePath) {
                        try {
                            BitmapFactory.decodeFile(mediaFilePath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeumorphicColors.SurfaceDark.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured image preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre / Étiquette") },
                    placeholder = { Text("ex: Chapitre 1 - Définition") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NeumorphicColors.TextPrimary,
                        unfocusedTextColor = NeumorphicColors.TextPrimary,
                        focusedBorderColor = NeumorphicColors.Primary,
                        unfocusedBorderColor = NeumorphicColors.TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question / Contenu du Recto") },
                    placeholder = { Text("Rédigez la question ou le contenu à réviser...") },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NeumorphicColors.TextPrimary,
                        unfocusedTextColor = NeumorphicColors.TextPrimary,
                        focusedBorderColor = NeumorphicColors.Primary,
                        unfocusedBorderColor = NeumorphicColors.TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank()) {
                        val finalTitle = title.ifBlank {
                            question.trim().take(30)
                        }
                        onSave(finalTitle, question.trim())
                    }
                },
                enabled = question.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(confirmButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = NeumorphicColors.TextSecondary)
            }
        }
    )
}
