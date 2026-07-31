package com.example.ui.screen.exams

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.ExamEntity
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.NeumorphicColors
import androidx.compose.material.icons.filled.Wallpaper
import com.example.service.DayNightLiveWallpaperService
import com.example.widget.ExamCountdownWidgetReceiver
import com.example.widget.ExamMatrixWidgetReceiver
import java.util.Calendar

@Composable
fun ExamsScreen(
    viewModel: ExamsViewModel,
    modifier: Modifier = Modifier
) {
    val examsList by viewModel.exams.collectAsState()
    var isDialogOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val todayCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeumorphicColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header
        Text(
            text = "EXAM COUNTDOWNS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = NeumorphicColors.TextPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )
        Text(
            text = "Track remaining days before academic trials",
            style = MaterialTheme.typography.bodySmall,
            color = NeumorphicColors.TextSecondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Main List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Widget Pinning Card
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Widget Icon",
                            tint = NeumorphicColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "HOME SCREEN WIDGETS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = NeumorphicColors.TextPrimary
                        )
                    }
                    Text(
                        text = "Add live glassy widgets and dynamic auto day/night phone wallpaper:",
                        fontSize = 12.sp,
                        color = NeumorphicColors.TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { requestPinWidgetToHomeScreen(context, ExamCountdownWidgetReceiver::class.java) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stats Widget", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { requestPinWidgetToHomeScreen(context, ExamMatrixWidgetReceiver::class.java) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Matrix Widget", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = { launchLiveWallpaperPicker(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5B37))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Auto Day/Night Phone Wallpaper", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (examsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = NeumorphicColors.SurfaceDark.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No academic trials registered",
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicColors.TextPrimary
                            )
                            Text(
                                text = "Add exams to view precision countdowns.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeumorphicColors.TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(
                    items = examsList.sortedBy { it.examDate },
                    key = { it.id }
                ) { exam ->
                    val examCalendar = Calendar.getInstance().apply {
                        timeInMillis = exam.examDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val diffMs = examCalendar.timeInMillis - todayCalendar.timeInMillis
                    val daysLeft = (diffMs / (24 * 60 * 60 * 1000L)).toInt()

                    val countdownColor = when {
                        daysLeft < 0 -> NeumorphicColors.TextSecondary
                        daysLeft <= 3 -> NeumorphicColors.Accent       // Short critical interval (coral)
                        daysLeft <= 7 -> NeumorphicColors.Warning      // Medium alert interval (orange)
                        else -> NeumorphicColors.Success               // Healthy safe interval (green)
                    }

                    val daysText = when {
                        daysLeft < 0 -> "Passed"
                        daysLeft == 0 -> "Today!"
                        daysLeft == 1 -> "1 day left"
                        else -> "$daysLeft days left"
                    }

                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        elevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left accent color strip from chosen tag path
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(exam.color))
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeumorphicColors.TextPrimary
                                )
                                Text(
                                    text = exam.subject,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeumorphicColors.TextSecondary
                                )
                            }

                            // Days Remaining block
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = daysText,
                                    color = countdownColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = NeumorphicColors.Accent.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.deleteExam(exam) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Exam Trigger button floated neat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            NeumorphicButton(
                label = "Add Exam",
                icon = Icons.Default.Add,
                onClick = { isDialogOpen = true },
                accentColor = NeumorphicColors.Primary
            )
        }
    }

    // Modal adding dialog
    if (isDialogOpen) {
        val colorTags = listOf(
            0xFF6C63FF, // Violet
            0xFFFF6584, // Coral
            0xFF4CAF82, // Green
            0xFFFFB347, // Orange
            0xFF3A86FF, // Blue
            0xFFFF007F, // Pink
            0xFF00F5D4, // Cyan
            0xFFD4AF37  // Gold
        )

        var name by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var activeColor by remember { mutableStateOf(colorTags[0].toInt()) }

        // Date Inputs state
        var dayInput by remember { mutableStateOf("") }
        var monthInput by remember { mutableStateOf("") }
        var yearInput by remember { mutableStateOf("") }

        var validationError by remember { mutableStateOf("") }

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NeumorphicColors.TextPrimary,
            unfocusedTextColor = NeumorphicColors.TextPrimary,
            focusedLabelColor = NeumorphicColors.Primary,
            unfocusedLabelColor = NeumorphicColors.TextSecondary,
            focusedBorderColor = NeumorphicColors.Primary,
            unfocusedBorderColor = NeumorphicColors.TextSecondary.copy(alpha = 0.5f),
            focusedPlaceholderColor = NeumorphicColors.TextSecondary,
            unfocusedPlaceholderColor = NeumorphicColors.TextSecondary.copy(alpha = 0.7f)
        )

        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            containerColor = NeumorphicColors.DialogBackground, // Dynamic to theme
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Add New Exam",
                    fontWeight = FontWeight.Black,
                    color = NeumorphicColors.TextPrimary,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Exam Name (e.g. Physics)") },
                        colors = textFieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject (e.g. Thermodynamics)") },
                        colors = textFieldColors,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Resilient Date Segmented input
                    Text(
                        text = "Exam Date",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dayInput,
                            onValueChange = { if (it.length <= 2) dayInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Day") },
                            placeholder = { Text("15") },
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = monthInput,
                            onValueChange = { if (it.length <= 2) monthInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Month") },
                            placeholder = { Text("06") },
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { if (it.length <= 4) yearInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Year") },
                            placeholder = { Text("2026") },
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // Colour dot selector
                    Text(
                        text = "Card Color Tag",
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicColors.TextPrimary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorTags.forEach { cHex ->
                            val colorInt = cHex.toInt()
                            val isSelected = activeColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(cHex))
                                    .clickable { activeColor = colorInt },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (validationError.isNotEmpty()) {
                        Text(
                            text = validationError,
                            color = NeumorphicColors.Accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dVal = dayInput.toIntOrNull()
                        val mVal = monthInput.toIntOrNull()
                        val yVal = yearInput.toIntOrNull()

                        if (name.trim().isEmpty() || subject.trim().isEmpty()) {
                            validationError = "Please specify Name and Subject"
                        } else if (dVal == null || mVal == null || yVal == null || dVal !in 1..31 || mVal !in 1..12 || yVal < 2026) {
                            validationError = "Specify a valid future date after 2026-01-01"
                        } else {
                            // Compile date epoch millis safely
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, yVal)
                                set(Calendar.MONTH, mVal - 1)
                                set(Calendar.DAY_OF_MONTH, dVal)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            
                            viewModel.addExam(name, subject, cal.timeInMillis, activeColor)
                            isDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeumorphicColors.Primary)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogOpen = false }) {
                    Text("Cancel", color = NeumorphicColors.TextSecondary)
                }
            }
        )
    }
}

private fun requestPinWidgetToHomeScreen(context: Context, receiverClass: Class<*>) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val myProvider = android.content.ComponentName(context, receiverClass)
            appWidgetManager.requestPinAppWidget(myProvider, null, null)
            Toast.makeText(context, "Adding widget to home screen...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Widget pinning is not supported by your launcher.", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Long-press your home screen to pick Focus Flow widgets.", Toast.LENGTH_LONG).show()
    }
}

private fun launchLiveWallpaperPicker(context: Context) {
    try {
        val intent = android.content.Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                android.content.ComponentName(context, DayNightLiveWallpaperService::class.java)
            )
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = android.content.Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Live wallpaper setup opened.", Toast.LENGTH_SHORT).show()
        }
    }
}
