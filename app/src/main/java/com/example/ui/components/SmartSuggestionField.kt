package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/** A small banner card with a switch that turns smart suggestions/autofill on or off for a form. */
@Composable
fun SuggestionsToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "Learns from what you type and autofills fields"
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Smart suggestions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

/**
 * A text field that offers autocomplete chips drawn from values the surgeon has
 * typed into this same field before (see `SuggestionRepository`). When
 * [suggestionsEnabled] is false it behaves exactly like a plain [OutlinedTextField]
 * — no dropdown, and the caller should also skip learning on save.
 *
 * For multi-value fields (e.g. "Penicillin, Latex") suggestions are matched and
 * inserted against the last comma/semicolon-separated segment being typed, so
 * picking a suggestion completes just that item rather than replacing the whole field.
 */
@Composable
fun SmartSuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    suggestionsEnabled: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val activeSegment = remember(value) {
        value.substringAfterLast(',').substringAfterLast(';').trimStart()
    }
    val segmentPrefix = remember(value, activeSegment) {
        value.substring(0, value.length - activeSegment.length)
    }

    val filtered = remember(suggestions, activeSegment, suggestionsEnabled, isFocused) {
        if (!suggestionsEnabled || !isFocused || activeSegment.trim().length < 2) {
            emptyList()
        } else {
            suggestions.filter {
                it.contains(activeSegment.trim(), ignoreCase = true) &&
                    !it.equals(activeSegment.trim(), ignoreCase = true)
            }.take(5)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            isError = isError,
            trailingIcon = {
                if (suggestionsEnabled) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusEvent { focusState -> isFocused = focusState.isFocused }
        )
        if (isError && errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
        if (filtered.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column {
                    filtered.forEachIndexed { index, suggestion ->
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(segmentPrefix + suggestion)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        if (index != filtered.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * A protocol/steps field that auto-formats free text into bullet points: typing a
 * period ends the current bullet and starts a new one on the next line (and pressing
 * Enter does the same), instead of the text staying one continuous paragraph.
 */
@Composable
fun BulletProtocolField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Operative protocol",
    placeholder: String = "Type a step and end it with a period to start the next one…"
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Sync from external changes only (e.g. loading an existing surgery record) — never
    // on every keystroke, or the cursor would jump to the end while mid-edit.
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { incoming ->
            val formatted = formatAsBulletPoints(fieldValue, incoming)
            fieldValue = formatted
            onValueChange(formatted.text)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = 5,
        modifier = modifier.fillMaxWidth()
    )
}

private fun formatAsBulletPoints(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    // Seed the very first bullet marker as soon as the surgeon starts typing.
    if (old.text.isEmpty() && new.text.isNotEmpty() && !new.text.startsWith("\u2022")) {
        val seeded = "\u2022 " + new.text
        return TextFieldValue(seeded, TextRange(seeded.length))
    }

    val isSingleCharacterInsert = new.text.length == old.text.length + 1 &&
        new.selection.start == new.text.length &&
        old.selection.start == old.text.length

    if (isSingleCharacterInsert) {
        when (new.text.last()) {
            '.' -> {
                val withoutDot = new.text.dropLast(1).trimEnd()
                val newText = "$withoutDot.\n\u2022 "
                return TextFieldValue(newText, TextRange(newText.length))
            }
            '\n' -> {
                val newText = new.text + "\u2022 "
                return TextFieldValue(newText, TextRange(newText.length))
            }
        }
    }
    return new
}
